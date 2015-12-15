// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC.AESComponents;

import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;
import org.factcenter.fastgc.YaoGC.State;

public class SBox extends CompositeCircuit {
	private static final byte Inv = 0;
	private static final byte Aff = 1;

	public SBox(CircuitGlobals globals) {
		super(globals, 8, 8, 2, "SBox");
	}

	public State startExecuting(State s) {
		for (int j = 0; j < 8; j++) {
			inputWires[j].value = s.wires[j].value;
			inputWires[j].invd = s.wires[j].invd;
			inputWires[j].setLabel(s.wires[j].lbl);
			inputWires[j].setReady(s.execSerial);
		}

		return State.fromWires(outputWires);
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		subCircuits[Inv] = new Inverse_GF256(globals);
		subCircuits[Aff] = new AffineTransform(globals);
	}

	protected void connectWires() {
		for (int i = 0; i < 8; i++) {
			inputWires[i].connectTo(subCircuits[Inv].inputWires, i);

			subCircuits[Inv].outputWires[i].connectTo(
					subCircuits[Aff].inputWires, i);
		}
	}

	protected void defineOutputWires() {
		System.arraycopy(subCircuits[Aff].outputWires, 0, outputWires, 0, 8);
	}

//	public static void main(String[] args) throws Exception {
//		SBox cc = new SBox();
//
//		cc.build(true);
//
//		for (int b = 0x0; b < 0x3f; b++) {
//			State in = new State(java.math.BigInteger.valueOf(b), 8);
//			State out = cc.startExecuting(in);
//			java.math.BigInteger outInt = java.math.BigInteger.ZERO;
//			for (int j = 0; j < 8; j++)
//				if (out.wires[j].value == 1)
//					outInt = outInt.setBit(j);
//
//			System.out.print(outInt.toString(16) + " ");
//		}
//		System.out.println();
//	}
}