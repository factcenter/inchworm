// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC;

import java.math.BigInteger;

public abstract class AND_2_1 extends SimpleCircuit_2_1 {
	public AND_2_1(CircuitGlobals globals) {
		super(globals, "AND_2_1");
	}

	public static AND_2_1 newInstance(CircuitGlobals globals, boolean isForGarbling) {
		if (isForGarbling)
			return new G_AND_2_1(globals);
		else
			return new E_AND_2_1(globals);
	}

	protected void compute() {
		int left = inputWires[0].value;
		int right = inputWires[1].value;

		outputWires[0].value = left & right;
	}

	protected void fillTruthTable() {
		Wire inWireL = inputWires[0];
		Wire inWireR = inputWires[1];
		Wire outWire = outputWires[0];

		// Labels for left input wire
		BigInteger[] labelL = { inWireL.lbl, inWireL.conjugate() };
		if (inWireL.invd == true) {
			BigInteger tmp = labelL[0];
			labelL[0] = labelL[1];
			labelL[1] = tmp;
		}

		// Labels for right input wire
		BigInteger[] labelR = { inWireR.lbl, inWireR.conjugate() };
		if (inWireR.invd == true) {
			BigInteger tmp = labelR[0];
			labelR[0] = labelR[1];
			labelR[1] = tmp;
		}

		int k = outWire.serialNum;

		gtt = new BigInteger[2][2];

		int cL = inWireL.lbl.testBit(0) ? 1 : 0;
		int cR = inWireR.lbl.testBit(0) ? 1 : 0;

		BigInteger[] lb = new BigInteger[2];
		lb[cL & cR] = globals.cipher.encrypt(labelL[cL], labelR[cR], k, BigInteger.ZERO);
		lb[1 - (cL & cR)] = Wire.conjugate(globals.R, lb[cL & cR]);
		outWire.lbl = lb[0];
		
		if (logger.isTraceEnabled()) {
			logger.trace("Computed garbled table: output {} label is 0x{}", outWire.serialNum, outWire.lbl.toString(16));
		}

		gtt[0 ^ cL][0 ^ cR] = lb[0];
		gtt[0 ^ cL][1 ^ cR] = lb[0];
		gtt[1 ^ cL][0 ^ cR] = lb[0];
		gtt[1 ^ cL][1 ^ cR] = lb[1];
	}

	protected boolean shortCut() {
		if (inputWires[0].value == 0) {
			outputWires[0].value = 0;
			return true;
		}

		if (inputWires[1].value == 0) {
			outputWires[0].value = 0;
			return true;
		}

		return false;
	}

	protected boolean collapse() {
		Wire inWireL = inputWires[0];
		Wire inWireR = inputWires[1];
		Wire outWire = outputWires[0];

		if (inWireL.lbl.equals(inWireR.lbl)) {
			if (inWireL.invd == inWireR.invd) {
				outWire.invd = inWireL.invd;
				outWire.setLabel(inWireL.lbl);
			} else {
				outWire.invd = false;
				outWire.value = 0;
			}

			return true;
		}

		return false;
	}
}
