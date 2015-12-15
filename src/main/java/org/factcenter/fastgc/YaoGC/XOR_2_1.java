// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC;

import java.math.BigInteger;

public class XOR_2_1 extends SimpleCircuit_2_1 {
	public XOR_2_1(CircuitGlobals globals) {
		super(globals, "XOR_2_1");
	}

	protected void compute() {
		int left = inputWires[0].value;
		int right = inputWires[1].value;

		outputWires[0].value = left ^ right;
	}

	public void execute() {
		Wire inWireL = inputWires[0];
		Wire inWireR = inputWires[1];
		Wire outWire = outputWires[0];

		if (inWireL.value != Wire.UNKNOWN_SIG
				&& inWireR.value != Wire.UNKNOWN_SIG) {
            // Both inputs are known, we can compute the known output.
			compute();
		} else if (inWireL.value != Wire.UNKNOWN_SIG) {
            // Only the left input is known
			if (inWireL.value == 0)
				outWire.invd = inWireR.invd;
			else
				outWire.invd = !inWireR.invd;

			outWire.value = Wire.UNKNOWN_SIG;
			outWire.setLabel(inWireR.lbl);
		} else if (inWireR.value != Wire.UNKNOWN_SIG) {
            // Only the right input is known
			if (inWireR.value == 0)
				outWire.invd = inWireL.invd;
			else
				outWire.invd = !inWireL.invd;

			outWire.value = Wire.UNKNOWN_SIG;
			outWire.setLabel(inWireL.lbl);
		} else {
			if (collapse()) {

			} else {
				BigInteger l = inWireL.lbl;
				BigInteger r = inWireR.lbl;

				BigInteger out = l.xor(r);

				outWire.invd = inWireL.invd ^ inWireR.invd;
				outWire.value = Wire.UNKNOWN_SIG;
				outWire.setLabel(out);
			}
		}

		outWire.setReady(Math.min(inWireL.execSerial, inWireR.execSerial));
	}

	protected boolean collapse() {
		Wire inWireL = inputWires[0];
		Wire inWireR = inputWires[1];
		Wire outWire = outputWires[0];

		if (inWireL.lbl.equals(inWireR.lbl)) {
			if (inWireL.invd == inWireR.invd) {
				outWire.invd = false;
				outWire.value = 0;
			} else {
				outWire.invd = false;
				outWire.value = 1;
			}

			return true;
		}

		return false;
	}

	// Never used for XOR gate.
	protected boolean shortCut() {
		return false;
	}

	// Never used for XOR gate.
	protected void fillTruthTable() {
	}

	protected void execYao() {
	}
}
