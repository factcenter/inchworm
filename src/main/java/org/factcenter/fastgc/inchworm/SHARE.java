package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;
import org.factcenter.fastgc.YaoGC.Wire;
import org.factcenter.fastgc.YaoGC.XOR_2L_L;

/**
 * 3*N bit composite xor secret share circuit implementation. Input ports order
 * is: [Player0_random, Player1_random, Computation_Result] Output ports order
 * is: [xValue, yValue].
 * 
 */
public class SHARE extends CompositeCircuit {

	private final int bitWidth;

	XOR_2L_L rXOR;
	XOR_2L_L outXOR;

	/**
	 * Constructs a new {@link SHARE} object.
	 * 
	 * @param globals - Global circuit parameters.
	 * @param bitWidth - op word size.
	 */
	public SHARE(CircuitGlobals globals, int bitWidth) {
		super(globals, 3 * bitWidth, bitWidth, 2, "SHARE_" + bitWidth);
		this.bitWidth = bitWidth;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		// Create the sub circuits.
		rXOR = new XOR_2L_L(globals, bitWidth);
		outXOR = new XOR_2L_L(globals, bitWidth);

		int i = 0;
		subCircuits[i++] = rXOR;
		subCircuits[i++] = outXOR;
	}

	@Override
	protected void connectWires() {
		for (int i = 0; i < bitWidth; ++i) {
			inputWires[LRandom(i)].connectTo(rXOR.inputWires, rXOR.X(i));
			inputWires[RRandom(i)].connectTo(rXOR.inputWires, rXOR.Y(i));

			rXOR.outputWires[i].connectTo(outXOR.inputWires, outXOR.X(i));
			inputWires[Result(i)].connectTo(outXOR.inputWires, outXOR.Y(i));
		}
	}

	@Override
	protected void defineOutputWires() {
		for (int i = 0; i < bitWidth; i++) {
			 outXOR.outputWires[i].connectTo(outputWires, i);
		}
	}

	/**
	 * Player 0 (left player) random value.
	 * 
	 * @param i - index of wire in input
	 * @return index of wire within all input wires.
	 */
	private int LRandom(int i) {
		return i;
	}

	/**
	 * Player 1 (right player) random value.
	 * 
	 * @param i - index of wire in input
	 * @return index of wire within all input wires.
	 */
	private int RRandom(int i) {
		return i + bitWidth;
	}

	/**
	 * Results of computation for secret sharing.
	 * 
	 * @param i - index of wire in input
	 * @return index of wire within all input wires.
	 */
	private int Result(int i) {
		return i + 2 * bitWidth;
	}

	/**
	 * Connect xWires[xStartPos...xStartPos + bitWidth] to the wires
	 * representing bits of X; Order of data is 
	 * Player0_rValue + Player1_rValue (rWires[]) + Computation_Result (dWires[]).
	 */
	public void connectWiresToInputs(Wire[] leftRand, int lStartPos,
			Wire[] rightRand, int rStartPos, Wire[] result, int dStartPos) {

		for (int i = 0; i < bitWidth; i++) {
			// Computation result (sometimes random values might be wider).
			if (i < result.length)
		    	result[i + dStartPos].connectTo(inputWires, Result(i));
			// Player0_rValue
			leftRand[i + lStartPos].connectTo(inputWires, LRandom(i));
			// Player1_rValue
			rightRand[i + rStartPos].connectTo(inputWires, RRandom(i));

		}
	}

}
