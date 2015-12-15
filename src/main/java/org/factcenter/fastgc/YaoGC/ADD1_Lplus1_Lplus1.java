// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC;


public class ADD1_Lplus1_Lplus1 extends CompositeCircuit {
	private final int L;

	public ADD1_Lplus1_Lplus1(CircuitGlobals globals, int l) {
		super(globals, l+1, l+1, 1, "ADD1_" + (l+1) + "_" + (l+1));
		L = l;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		subCircuits[0] = new ADD_2L_Lplus1(globals, L);
	}

	protected void connectWires() {
		inputWires[0].connectTo(subCircuits[0].inputWires, 0);

		for (int i = 0; i < L; i++)
			inputWires[i+1].connectTo(subCircuits[0].inputWires, 2*i+1);
	}

	protected void defineOutputWires() {
		System.arraycopy(subCircuits[0].outputWires, 0, outputWires, 0, L+1);
	}

	protected void fixInternalWires() {
		Wire internalWire;
		for (int i = 1; i < L; i++) {
			internalWire = subCircuits[0].inputWires[2*i];
			internalWire.fixWire(0);
		}
		internalWire = subCircuits[0].inputWires[2*L];
		internalWire.fixWire(0); // todo: add a test for this.
	}
}