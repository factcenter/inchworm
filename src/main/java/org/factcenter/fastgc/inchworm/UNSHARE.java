package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;
import org.factcenter.fastgc.YaoGC.Wire;
import org.factcenter.fastgc.YaoGC.XOR_2L_L;

/**
 * 2*2N bit composite xor secret un-share circuit implementation. Input ports
 * order is: [Player0_xShare, Player0_yShare, Player1_xShare, Player1_yShare]
 * Output ports order is: [xValue, yValue].
 * 
 */
public class UNSHARE extends CompositeCircuit {

	private final int bitWidth;

	XOR_2L_L xXOR;
	XOR_2L_L yXOR;

	/**
	 * Constructs a new {@link UNSHARE} object.
	 * 
	 * @param globals - Global circuit parameters.
	 * @param bitWidth - op word size.
	 */
	public UNSHARE(CircuitGlobals globals, int bitWidth) {
		super(globals, 4 * bitWidth, 2 * bitWidth, 2, "UNSHARE_" + bitWidth);
		this.bitWidth = bitWidth;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		// Create the sub circuits.
		xXOR = new XOR_2L_L(globals, bitWidth);
		yXOR = new XOR_2L_L(globals, bitWidth);

		int i = 0;
		subCircuits[i++] = xXOR;
		subCircuits[i++] = yXOR;
	}

	@Override
	protected void connectWires() {
		for (int i = 0; i < bitWidth; ++i) {
			// Wire x shares to the xXor inputs.
			inputWires[XLeft(i)].connectTo(xXOR.inputWires, xXOR.X(i));
			inputWires[XRight(i)].connectTo(xXOR.inputWires, xXOR.Y(i));
			// Wire y shares to the yXor inputs.
			inputWires[YLeft(i)].connectTo(yXOR.inputWires, yXOR.X(i));
			inputWires[YRight(i)].connectTo(yXOR.inputWires, yXOR.Y(i));
		}
	}

	@Override
	protected void defineOutputWires() {
		for (int i = 0; i < bitWidth; i++) {
			 xXOR.outputWires[i].connectTo(outputWires, i);
			 yXOR.outputWires[i].connectTo(outputWires, i + bitWidth);
		}
	}

	/**
	 * Share of X input to player 0 (left player).
	 * 
	 * @param i - index of wire in input
	 * @return index of wire within all input wires.
	 */
	private int XLeft(int i) {
		return i;
	}

	/**
	 * Share of X input to player 1 (right player).
	 * 
	 * @param i - index of wire in input
	 * @return index of wire within all input wires.
	 */
	private int XRight(int i) {
		return i + 2 * bitWidth;
	}

	/**
	 * Share of Y input to player 0 (left player).
	 * 
	 * @param i - index of wire in input
	 * @return index of wire within all input wires.
	 */
	private int YLeft(int i) {
		return i + bitWidth;
	}

	/**
	 * Share of X input to player 1 (right player).
	 * 
	 * @param i
	 *            index of wire in input
	 * @return index of wire within all input wires.
	 */
	private int YRight(int i) {
		return i + 3 * bitWidth;
	}

	/**
	 * Connect xWires[xStartPos...xStartPos + bitWidth] to the wires
	 * representing bits of X; yWires[yStartPos...yStartPos + bitWidth] to the
	 * wires representing bits of Y;
	 */
	public void connectWiresToXY(Wire[] xWires, int xStartPos, Wire[] yWires,
			int yStartPos) {
		if (xStartPos + bitWidth > xWires.length
				|| yStartPos + bitWidth > yWires.length)
			throw new RuntimeException("Unmatched number of wires.");

		for (int i = 0; i < bitWidth; i++) {
			// Player0_xShare
			xWires[i + xStartPos].connectTo(inputWires, XLeft(i));
			// Player0_yShare
			xWires[i + xStartPos + bitWidth].connectTo(inputWires, YLeft(i));
			// Player1_xShare
			yWires[i + yStartPos].connectTo(inputWires, XRight(i));
			// Player1_yShare
			yWires[i + yStartPos + bitWidth].connectTo(inputWires, YRight(i));
		}
	}

}
