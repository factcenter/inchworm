package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;
import org.factcenter.fastgc.YaoGC.OR_L_1;

/**
 * K bit combinational divider. Input ports order is: [Player0_xShare
 * (dividend), Player0_yShare (divisor), Player0_rValue (double width),
 * Player1_xShare, Player1_yShare, Player1_rValue]. Output ports order is:
 * Quotient + Remainder (LSB to MSB)
 * 
 * @author GILBZ
 * 
 */
public class DIV_K_ZERO_DETECT extends CompositeCircuit {

	private final int opBitWidth;

	OR_L_1 zeroDetector;
	DIV_K divK;
    MUX_2L_L zeroSelector;
    BINARY_CONSTANT_L zeroBits;


	/**
	 * Constructs a new {@code DivOpCircuit} object.
	 * 
	 * @param globals - Global circuit parameters.
	 * @param cntBits - op word size.
	 */
	public DIV_K_ZERO_DETECT(CircuitGlobals globals, int cntBits) {
		super(globals, 2 * cntBits, 2 * cntBits, 4, "Div_" + cntBits + "bits");
		opBitWidth = cntBits;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
        int i = 0;

        subCircuits[i++] = divK = new DIV_K(globals, opBitWidth);
        subCircuits[i++] = zeroDetector = new OR_L_1(globals, opBitWidth);
        subCircuits[i++] = zeroSelector = new MUX_2L_L(globals, opBitWidth * 2);
        subCircuits[i++] = zeroBits = new BINARY_CONSTANT_L(globals, opBitWidth * 2, 0);
	}

	@Override
	protected void connectWires() {
		// Wire the inputs to the div inputs
		divK.connectWiresToXY(inputWires, 0,
                inputWires, opBitWidth);

		// Wire the divisor inputs to the zero detector
		zeroDetector.connectWiresToInputs(inputWires, opBitWidth);

        for (int i = 0; i < opBitWidth * 2; ++i) {
            zeroBits.outputWires[i].connectTo(zeroSelector.inputWires, i + opBitWidth * 2);
            divK.outputWires[i].connectTo(zeroSelector.inputWires, i );
        }

		// Wire the or output to the inverter.
		zeroDetector.outputWires[0].connectTo(zeroSelector.inputWires, opBitWidth * 4);
	}

	protected void defineOutputWires() {
		for (int i = 0; i < 2 * opBitWidth; ++i)
            zeroSelector.outputWires[i].connectTo(outputWires, i);
	}

}
