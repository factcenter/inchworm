package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;
import org.factcenter.fastgc.YaoGC.MUX_3_1;
import org.factcenter.fastgc.YaoGC.Wire;

/**
 * Mux_2L_L - selects between two inputs of same length (in bits). If the
 * choices bit (in the highest position - LSB bit of aValue) == 0 bValue is
 * returned, if choices bit == 1 cValue is returned.
 */
public class MUX_2L_L extends CompositeCircuit {

	private final int muxBitWidth;
	private final int choiceBitNdx;

	/**
	 * Constructs a new {@code MUX_2L_L} object.
	 * @param globals - Global circuit parameters.
	 * @param bitWidth - op word size.
	 */
	public MUX_2L_L(CircuitGlobals globals, int bitWidth) {
		super(globals, 2 * bitWidth + 1, bitWidth, bitWidth, "MUX21_"
				+ bitWidth);
		muxBitWidth = bitWidth;
		choiceBitNdx = 2 * bitWidth;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		for (int i = 0; i < muxBitWidth; i++)
			subCircuits[i] = new MUX_3_1(globals);

	}

	protected void connectWires() {

		for (int i = 0; i < muxBitWidth; i++) {
			inputWires[X(i)].connectTo(subCircuits[i].inputWires, MUX_3_1.X);
			inputWires[Y(i)].connectTo(subCircuits[i].inputWires, MUX_3_1.Y);
			inputWires[choiceBitNdx].connectTo(subCircuits[i].inputWires,
					MUX_3_1.C);
		}
	}

	protected void defineOutputWires() {
		for (int i = 0; i < muxBitWidth; i++)
			 subCircuits[i].outputWires[0].connectTo(outputWires, i);
	}

	private int X(int i) {
		return i + muxBitWidth;
	}

	private int Y(int i) {
		return i;
	}

	/**
	 * Connect xWires[xStartPos...xStartPos+L] to the wires representing bits of
	 * X; yWires[yStartPos...yStartPos+L] to the wires representing bits of Y;
	 */
	public void connectWiresToXY(Wire[] xWires, int xStartPos, Wire[] yWires,
			int yStartPos) {
		if (xStartPos + muxBitWidth > xWires.length
				|| yStartPos + muxBitWidth > yWires.length)
			throw new RuntimeException("Unmatched number of wires.");

		for (int i = 0; i < muxBitWidth; i++) {
			xWires[xStartPos + i].connectTo(inputWires, X(i));
			yWires[yStartPos + i].connectTo(inputWires, Y(i));
		}
	}

}
