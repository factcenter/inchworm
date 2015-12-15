package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.TransitiveObservable;
import org.factcenter.fastgc.YaoGC.Wire;

/**
 * ROL_L_X - Rotates a L-bits number X-bits to the left.
 * Input ports order is: the number to rotate.
 */
public class ROL_L_X extends Circuit {

	private final int bitWidth;
	private final int shiftSize;

	/**
	 * Constructs a new {@code ROL_L_X} object.
	 * 
	 * @param globals - Global circuit parameters.
	 * @param bitWidth - op word size.
	 * @param cntBitsShift - number of bits to rotate.
	 */
	public ROL_L_X(CircuitGlobals globals, int bitWidth, int cntBitsShift) {
		super(globals, bitWidth, bitWidth, "ROL_" + bitWidth + "_"
				+ cntBitsShift);
		this.bitWidth = bitWidth;
		this.shiftSize = cntBitsShift;
	}

	@Override
	public void build(boolean isForGarbling) {
		super.createInputWires();
		for (int i = 0; i < inDegree; i++) {
			inputWires[i].addObserver(this, new TransitiveObservable.Socket(
					inputWires, i));
		}
		createOutputWires();
	}

	protected void createOutputWires() {
		for (int i = 0; i < inDegree; i++)
            new Wire(globals, outputWires, i);
	}

	@Override
	protected void compute() {
		// Do nothing, just wires.
		for (int i = 0; i < inDegree; ++i)
			outputWires[(i + shiftSize) % bitWidth].value = inputWires[i].value;
		/*-
		 * NOTE: This method is never called. 
		 *       Normally it is implemented and called only for:
		 *       1) AND_2_1, OR_2_1 and XOR_2_1 (classes that extend SimpleCircuit_2_1 when 
		 *          both inputs != Wire.UNKNOWN_SIG), 
		 *       2) by fixWire() for a circuit with tied inputs.
		 */

	}

	@Override
	protected void execute() {
        int minSerial = Integer.MAX_VALUE;
        for (int i = 0; i < inDegree; ++i)
            minSerial = Math.min(minSerial, inputWires[i].getExecSerial());

        // Do nothing, just wires.
		for (int i = 0; i < inDegree; ++i) {
			outputWires[(i + shiftSize) % bitWidth].value = inputWires[i].value;
			outputWires[(i + shiftSize) % bitWidth].lbl = inputWires[i].lbl;
			outputWires[(i + shiftSize) % bitWidth].invd = inputWires[i].invd;
			outputWires[(i + shiftSize) % bitWidth].setReady(minSerial);
		}
	}

}
