package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;
import org.factcenter.fastgc.YaoGC.Wire;

/*-
 * NOTE: Input ports order of the final op (MulOpCircuit) is:
 *      [Player0_xShare, Player0_yShare, Player0_rValue (double width), 
 *       Player1_xShare, Player1_yShare, Player1_rValue (double width)].
 */

/**
 * MUL_K- K bit combinational multiplier. Input ports order (LSB to MSB) is:
 * A_value + B_value (server + client).
 */
public class MUL_K extends CompositeCircuit {

	private final int opBitWidth;

	/**
	 * Array of opBitWidth {@link MUL_BLOCK} instances.
	 */
	private MUL_BLOCK[][] mulBlocks;

	/**
	 * Constructs a new {@code MUL_K} object.
	 * 
	 * @param globals - Global circuit parameters.
	 * @param cntBits - op word size.
	 */
	public MUL_K(CircuitGlobals globals, int cntBits) {
		super(globals, 2 * cntBits, 2 * cntBits, cntBits * cntBits, "MUL_"
				+ cntBits);
		opBitWidth = cntBits;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {

		// Create a 2 dim array of type MUL_BLOCK.
		int circuitNdx = 0;
		mulBlocks = new MUL_BLOCK[opBitWidth][opBitWidth];
		for (int i = 0; i < opBitWidth; i++) {
			for (int j = 0; j < opBitWidth; j++) {
				mulBlocks[i][j] = new MUL_BLOCK(globals);
				subCircuits[circuitNdx++] = mulBlocks[i][j];
			}
		}

	}

	@Override
	protected void fixInternalWires() {
		// Tie carry and sum inputs of mul_blocks first row.
		for (int i = 0; i < opBitWidth; i++) {
			Wire internalWire = mulBlocks[0][i].inputWires[MUL_BLOCK.C_IN];
			internalWire.fixWire(0);
			internalWire = mulBlocks[0][i].inputWires[MUL_BLOCK.SUM_IN];
			internalWire.fixWire(0);
		}

		// Tie sum inputs of left mul_blocks of all rows except the first row.
		for (int i = 1; i < opBitWidth; i++) {
			Wire internalWire = mulBlocks[i][opBitWidth - 1].inputWires[MUL_BLOCK.SUM_IN];
			internalWire.fixWire(0);
		}

	}

	@Override
	protected void connectWires() {

		connectAInputWires();
		connectBInputWires();
		connectInternalWires();

	}

	@Override
	protected void defineOutputWires() {
		int outputNdx = 0;
		// All lines except the last one - take output from the left mul_block.
		for (int i = 0; i < opBitWidth - 1; i++) {
			 mulBlocks[i][0].outputWires[MUL_BLOCK.SUM_OUT].connectTo(outputWires, outputNdx++);
		}
		// Last mul_block line.
		for (int i = 0; i < opBitWidth; i++) {
			 mulBlocks[opBitWidth - 1][i].outputWires[MUL_BLOCK.SUM_OUT].connectTo(outputWires, outputNdx++);
			if (i == opBitWidth - 1) {
				 mulBlocks[opBitWidth - 1][i].outputWires[MUL_BLOCK.C_OUT].connectTo(outputWires, outputNdx);
			}
		}
	}

	/**
	 * Connect the A inputs to all mul_blocks.
	 */
	private void connectAInputWires() {
		for (int i = 0; i < opBitWidth; i++) {
			for (int j = 0; j < opBitWidth; j++) {
				inputWires[i]
						.connectTo(mulBlocks[j][i].inputWires, MUL_BLOCK.X);
			}
		}
	}

	/**
	 * Connect the B inputs to all mul_blocks.
	 */
	private void connectBInputWires() {
		for (int i = 0; i < opBitWidth; i++) {
			for (int j = 0; j < opBitWidth; j++) {
				inputWires[i + opBitWidth].connectTo(
						mulBlocks[i][j].inputWires, MUL_BLOCK.Y);
			}
		}
	}

	/**
	 * Connect internal wires for the carry and sum ports.
	 */
	private void connectInternalWires() {
		for (int i = 0; i < opBitWidth - 1; i++) {
			for (int j = 0; j < opBitWidth; j++) {
				if (j != 0) {
					mulBlocks[i][j].outputWires[MUL_BLOCK.SUM_OUT].connectTo(
							mulBlocks[i + 1][j - 1].inputWires,	MUL_BLOCK.SUM_IN);
				}
				mulBlocks[i][j].outputWires[MUL_BLOCK.C_OUT].connectTo(
						mulBlocks[i + 1][j].inputWires, MUL_BLOCK.C_IN);

			}
		}
	}
	
	
	private int X(int i) {
		return i + opBitWidth;
	}

	private int Y(int i) {
		return i;
	}
	
	/**
	 * Connect xWires[xStartPos...xStartPos+L] to the wires representing bits of X;
	 * yWires[yStartPos...yStartPos+L] to the wires representing bits of Y;
	 */
	public void connectWiresToXY(Wire[] xWires, int xStartPos, Wire[] yWires, int yStartPos) {
		if (xStartPos + opBitWidth > xWires.length || yStartPos + opBitWidth > yWires.length)
			throw new RuntimeException("Unmatched number of wires.");

		for (int i = 0; i < opBitWidth; i++) {
			xWires[xStartPos+i].connectTo(inputWires, X(i));
			yWires[yStartPos+i].connectTo(inputWires, Y(i));
		}
	}


}
