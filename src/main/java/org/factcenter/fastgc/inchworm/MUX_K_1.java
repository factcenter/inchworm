package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.AND_2_1;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;
import org.factcenter.fastgc.YaoGC.XOR_L_1;

/**
 * Mux_K_1 - selects a single bit from a K bits input. The value of the N
 * control bits {@code (K <= 2^N)} determines which input bit the circuit returns.
 * Input ports order (LSB to MSB) is: data_inputs + control_inputs.
 */
public class MUX_K_1 extends CompositeCircuit {

	private final int muxBitWidth;
	private final int muxControlBits;

//
//	/**
//	 * Array of muxBitWidth {@link AND_L_1} instances.
//	 */
//	private AND_L_1[] andComponents;
//	/**
//	 * Array of muxControlBits {@link INVERTER} instances.
//	 */
//	private INVERTER[] inverterComponents;

    private UNARY_CTR_K ctr_k;

    private AND_2_1[] andComponents;

    // We can use XOR instead of OR, since we know that only one of the bits
    // can possibly be non-zero.
	private XOR_L_1 orComponent;

	/**
	 * Constructs a new {@code MUX_K_1} object.
	 * 
	 * @param globals - Global circuit parameters.
	 * @param bitWidth - input block word bit size.
	 * @param cntControlBits - number of selection control bits.
	 */
	public MUX_K_1(CircuitGlobals globals, int bitWidth, int cntControlBits) {
		super(globals, bitWidth + cntControlBits, 1, bitWidth + 2,
				"MUX_" + bitWidth + "_1");

		this.muxBitWidth = bitWidth;
		this.muxControlBits = cntControlBits;

	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		//
		// bitWidth * AND circuits of width 1 + cntControlBits
		// 1 OR of width = bitWidth.
		// cntControlBits * INVERTER
		//
		int circuitNdx = 0;
		
		// Create muxBitWidth components of type AND_[1 + muxControlBits]_1. 
		andComponents = new AND_2_1[muxBitWidth];
		for (int i = 0; i < muxBitWidth; i++) {
			andComponents[i] = AND_2_1.newInstance(globals, isForGarbling);
			subCircuits[circuitNdx++] = andComponents[i];
		}
		
		orComponent = new XOR_L_1(globals, muxBitWidth);
		subCircuits[circuitNdx++] = orComponent;

        ctr_k = new UNARY_CTR_K(globals, muxControlBits, muxBitWidth);
        subCircuits[circuitNdx++] = ctr_k;
	}

	@Override
	protected void connectWires() {

        // First connect each of the control bits to the counter
        for (int i = 0; i < muxControlBits; ++i) {
            inputWires[muxBitWidth + i].connectTo(ctr_k.inputWires, i);
        }

        // Now connect one counter output and one input to each AND.
        // And the output of the and to the OR

		for (int i = 0; i < muxBitWidth; i++) {
			inputWires[i].connectTo(andComponents[i].inputWires, 0);
            ctr_k.outputWires[i].connectTo(andComponents[i].inputWires, 1);

            andComponents[i].outputWires[0].connectTo(orComponent.inputWires, i);
		}
	}
	
	@Override
	protected void defineOutputWires() {
		 orComponent.outputWires[0].connectTo(outputWires, 0);
	}

}
