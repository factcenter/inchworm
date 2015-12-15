package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.AND_2_1;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;
import org.factcenter.fastgc.YaoGC.Wire;


/**
 * Bitwise and circuit of two inputs of same length (in bits).
 */
public class AND_2L_L extends CompositeCircuit {

	private final int andBitWidth;

	/**
	 * Constructs a new {@code AND_2L_L} object.
	 * @param globals - Global circuit parameters.
	 * @param bitWidth - op word size.
	 */
	public AND_2L_L(CircuitGlobals globals, int bitWidth) {
		super(globals, 2 * bitWidth, bitWidth, bitWidth, "AND_" + (2 * bitWidth) + "_"
				+ bitWidth);
		andBitWidth = bitWidth;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		for (int i = 0; i < andBitWidth; i++)
			subCircuits[i] = AND_2_1.newInstance(globals, isForGarbling);
	}

	protected void connectWires() {

		for (int i = 0; i < andBitWidth; i++) {
			inputWires[X(i)].connectTo(subCircuits[i].inputWires, 0);
			inputWires[Y(i)].connectTo(subCircuits[i].inputWires, 1);
		}
	}

	protected void defineOutputWires() {
		for (int i = 0; i < andBitWidth; i++)
            subCircuits[i].outputWires[0].connectTo(outputWires, i);
	}


	public int X(int i) {
		return i + andBitWidth;
	}

	public int Y(int i) {
		return i;
	}
	
	/**
	 * Connect xWires[xStartPos...xStartPos+L] to the wires representing bits of X;
	 * yWires[yStartPos...yStartPos+L] to the wires representing bits of Y;
	 */
	public void connectWiresToXY(Wire[] xWires, int xStartPos, Wire[] yWires, int yStartPos) {
		if (xStartPos + andBitWidth > xWires.length || yStartPos + andBitWidth > yWires.length)
			throw new RuntimeException("Unmatched number of wires.");

		for (int i = 0; i < andBitWidth; i++) {
			xWires[xStartPos+i].connectTo(inputWires, X(i));
			yWires[yStartPos+i].connectTo(inputWires, Y(i));
		}
	}

}
