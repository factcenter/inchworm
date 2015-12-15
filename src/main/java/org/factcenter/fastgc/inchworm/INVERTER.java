package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.TransitiveObservable;
import org.factcenter.fastgc.YaoGC.Wire;

/**
 * INVERTER - Inverts a boolean value.
 */
public class INVERTER extends Circuit {

	/**
	 * Constructs a new {@code INVERTER} object.
	 * 
	 * @param globals - Global circuit parameters.
	 */
	public INVERTER(CircuitGlobals globals) {
		super(globals, 1, 1, "INVERTER");
	}

	@Override
	public void build(boolean isForGarbling) {
		super.createInputWires();
		inputWires[0].addObserver(this, new TransitiveObservable.Socket(
				inputWires, 0));
        new Wire(globals, outputWires, 0);
	}

	@Override
	protected void compute() {
		/*-
		 *  NOTE: This method is being called only by Wire.fixWire() method.
		 */
		logger.trace("subcircuit {} compute()", serialNo);
		outputWires[0].value = 1 - inputWires[0].value;
	}

	@Override
	protected void execute() {
		// Set the output wire to the inverted value of the input wire.
		outputWires[0].value = inputWires[0].value;
		outputWires[0].lbl = inputWires[0].lbl;
		outputWires[0].invd = !inputWires[0].invd;
		outputWires[0].setReady(inputWires[0].getExecSerial());
	}

}
