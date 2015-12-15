// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC;


class EDT_2L_1 extends CompositeCircuit {
	final int L;

	public EDT_2L_1(CircuitGlobals globals, int l) {
		super(globals, 2*l, 1, l+1, "EDT_2L_1");
		L = l;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		for (int i = 0; i < L; i++)
			subCircuits[i] = new XOR_2_1(globals);

		subCircuits[L] = new OR_L_1(globals, L);
	}

	protected void connectWires() {
		for (int i = 0; i < L; i++) {
			inputWires[i  ].connectTo(subCircuits[i].inputWires, 0);
			inputWires[i+L].connectTo(subCircuits[i].inputWires, 1);
			subCircuits[i].outputWires[0].connectTo(subCircuits[L].inputWires, i);
		}
	}

	protected void defineOutputWires() {
		 subCircuits[L].outputWires[0].connectTo(outputWires, 0);
	}
}