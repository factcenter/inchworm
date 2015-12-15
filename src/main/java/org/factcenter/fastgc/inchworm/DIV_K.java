package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;
import org.factcenter.fastgc.YaoGC.Wire;

/*-
 * NOTE: Input ports order of the final op (DivOpCircuit) is:
 *      [Player0_xShare, Player0_yShare, Player0_rValue, 
 *       Player1_xShare, Player1_yShare, Player1_rValue].
 */

/**
 * DIV_K- K bit combinational divider. Input ports order (LSB to MSB) is:
 * A_value dividend + B_value divisor (server + client). Output ports order is:
 * Quotient + Remainder.
 */
public class DIV_K extends CompositeCircuit {

	private final int opBitWidth;

	/**
	 * Array of opBitWidth {@link DIV_BLOCK} instances.
	 */
	private DIV_BLOCK[][] divBlocks;

	/**
	 * Array of opBitWidth {@link INVERTER} instances.
	 */
	private INVERTER[] inverters;

	/**
	 * Constructs a new {@code DIV_K} object.
	 * 
	 * @param globals - Global circuit parameters.
	 * @param cntBits - Circuit word size in bits.
	 */
	public DIV_K(CircuitGlobals globals, int cntBits) {
		super(globals, 2 * cntBits, 2 * cntBits, cntBits * (1 + cntBits),
				"DIV_" + cntBits);
		opBitWidth = cntBits;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {

		// Create a 2 dim array of type DIV_BLOCK.
		int circuitNdx = 0;
		divBlocks = new DIV_BLOCK[opBitWidth][opBitWidth];
		for (int i = 0; i < opBitWidth; i++) {
			for (int j = 0; j < opBitWidth; j++) {
				divBlocks[i][j] = new DIV_BLOCK(globals);
				subCircuits[circuitNdx++] = divBlocks[i][j];
			}
		}

		// Create the inverter array.
		inverters = new INVERTER[opBitWidth];
		for (int i = 0; i < opBitWidth; i++) {
			inverters[i] = new INVERTER(globals);
			subCircuits[circuitNdx++] = inverters[i];
		}
	}

	@Override
	protected void fixInternalWires() {

		// Tie Li inputs of div_blocks right column.
		for (int i = 0; i < opBitWidth; i++) {
			Wire internalWire = divBlocks[i][0].inputWires[DIV_BLOCK.Li];
			internalWire.fixWire(0);
		}

		// Tie free A inputs of the div_blocks.
		for (int j = 1; j < opBitWidth; j++) {
			Wire internalWire = divBlocks[0][j].inputWires[DIV_BLOCK.A];
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

		// Quotient - take output from the inverters.
		int baseNdx = opBitWidth - 1;
		int outputNdx = 0;
		for (int i = 0; i < opBitWidth; i++) {
            inverters[i].outputWires[0].connectTo(outputWires, outputNdx++);
		}

		// Remainder - take output from the div_blocks of last row.
		outputNdx = opBitWidth;
		for (int i = 0; i < opBitWidth; i++) {
            divBlocks[baseNdx][i].outputWires[DIV_BLOCK.DTag].connectTo(outputWires, outputNdx++);
		}

	}

	/**
	 * Connect the A inputs to the div_blocks in the right side of the div_block
	 * array.
	 */
	private void connectAInputWires() {
		int inputWireIndex = opBitWidth - 1;
		for (int i = 0; i < opBitWidth; i++) {
			inputWires[inputWireIndex - i].connectTo(
					divBlocks[i][0].inputWires, DIV_BLOCK.A);
		}

	}

	/**
	 * Connect the B inputs to all div_blocks.
	 */
	private void connectBInputWires() {
		for (int j = 0; j < opBitWidth; j++) {
			int inputWireIndex = j + opBitWidth;
			for (int i = 0; i < opBitWidth; i++) {
				inputWires[inputWireIndex].connectTo(
						divBlocks[i][j].inputWires, DIV_BLOCK.B);
			}
		}
	}

	/**
	 * Connect internal wires for the Lo, DTag, and OS ports.
	 */
	private void connectInternalWires() {

		// In each row connect the loan output of the block to the loan
		// input of the adjacent block. The loan output of the last, MSB block
		// is driving the OS inputs of all the blocks in the current row.
		int msbIndex = opBitWidth - 1;
		for (int i = 0; i < opBitWidth; i++) {
			for (int j = 0; j < opBitWidth; j++) {
				if (j != opBitWidth - 1) {
					divBlocks[i][j].outputWires[DIV_BLOCK.Lo].connectTo(
							divBlocks[i][j + 1].inputWires, DIV_BLOCK.Li);
				}
				divBlocks[i][msbIndex].outputWires[DIV_BLOCK.Lo].connectTo(
						divBlocks[i][j].inputWires, DIV_BLOCK.OS);
			}
		}

		// Connect the DTag output to the shifted free A inputs of the row
		// below the current one (except the last one...)
		for (int i = 0; i < opBitWidth - 1; i++) {
			for (int j = 0; j < opBitWidth - 1; j++) {
				divBlocks[i][j].outputWires[DIV_BLOCK.DTag].connectTo(
						divBlocks[i + 1][j + 1].inputWires, DIV_BLOCK.A);
			}
		}

		// Connect the quotient result (Lo outputs) to the inverters.
		for (int i = 0; i < opBitWidth; i++) {
			divBlocks[i][msbIndex].outputWires[DIV_BLOCK.Lo].connectTo(
					inverters[msbIndex - i].inputWires, 0);
		}

	}
	
	/**
	 * A value (dividend).
	 */
	private int A(int i) {
		return i;
	}

	/**
	 * B value (divisor).
	 */
	private int B(int i) {
		return i + opBitWidth;
	}
	
	/**
	 * Connect xWires[xStartPos...xStartPos+L] to the wires representing bits of X;
	 * yWires[yStartPos...yStartPos+L] to the wires representing bits of Y;
	 */
	public void connectWiresToXY(Wire[] xWires, int xStartPos, Wire[] yWires, int yStartPos) {
		if (xStartPos + opBitWidth > xWires.length || yStartPos + opBitWidth > yWires.length)
			throw new RuntimeException("Unmatched number of wires.");

		for (int i = 0; i < opBitWidth; i++) {
			xWires[xStartPos+i].connectTo(inputWires, A(i));
			yWires[yStartPos+i].connectTo(inputWires, B(i));
		}
	}

}
