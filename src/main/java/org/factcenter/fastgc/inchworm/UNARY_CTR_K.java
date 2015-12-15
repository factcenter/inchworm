package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;

/**
 * Converts from binary to unary counter. Input is K bits, outputs is up to 2^K bits,
 * of which all are 0 except for the one indexed by the input counter. The output can be limited
 * to less than 2^k bits.
  */
public class UNARY_CTR_K extends CompositeCircuit {

	private final int binaryCtrLen;
	private final int unaryCtrLen;

	/**
	 * Array constant counters, from 0 to unaryCtrLen
	 */
	private BINARY_CONSTANT_L[] constantCounters;

    private COMPARE_2L_1[] comparators;



	/**
	 * Constructs a new {@link UNARY_CTR_K} object.
	 *
	 * @param globals - Global circuit parameters.
	 * @param binaryCtrLen - number of bits in the input binary counter.
	 * @param unaryCtrLen - number of bits in the output unary counter (up to 2^binaryCtrLen).
	 */
	public UNARY_CTR_K(CircuitGlobals globals, int binaryCtrLen, int unaryCtrLen) {
		super(globals, binaryCtrLen, unaryCtrLen, 2 * unaryCtrLen, "UNARY_CTR_" + binaryCtrLen);

		this.binaryCtrLen = binaryCtrLen;
		this.unaryCtrLen = unaryCtrLen;

	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
        int n = 0;

        constantCounters = new BINARY_CONSTANT_L[unaryCtrLen];
        comparators = new COMPARE_2L_1[unaryCtrLen];

		for (int i = 0; i < unaryCtrLen; ++i) {
            subCircuits[n++] = constantCounters[i] = new BINARY_CONSTANT_L(globals, binaryCtrLen, i);
            subCircuits[n++] = comparators[i] = new COMPARE_2L_1(globals, binaryCtrLen);
        }
	}

	@Override
	protected void connectWires() {
        for (int i = 0; i < unaryCtrLen; ++i) {
            for (int j = 0; j < binaryCtrLen; ++j) {
                inputWires[j].connectTo(comparators[i].inputWires, j);
                constantCounters[i].outputWires[j].connectTo(comparators[i].inputWires, binaryCtrLen + j);
            }
        }
	}

	@Override
	protected void defineOutputWires() {
		for (int i = 0; i < unaryCtrLen; ++i) {
             comparators[i].outputWires[0].connectTo(outputWires, i);
        }
	}

}
