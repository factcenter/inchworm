// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC;


/*
 * Fig. 5 of [KSS09]
 */
public class GT_2L_1 extends CompositeCircuit {
	private final int L;

	public GT_2L_1(CircuitGlobals globals, int l) {
		super(globals, 2*l, 1, l, "GT_" + (2*l) + "_1");
		L = l;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		for (int i = 0; i < L; i++) {
			subCircuits[i] = new GT_3_1(globals);
		}
	}

	protected void connectWires() {
		inputWires[X(0)].connectTo(subCircuits[0].inputWires, GT_3_1.X);
		inputWires[Y(0)].connectTo(subCircuits[0].inputWires, GT_3_1.Y);

		for (int i = 1; i < L; i++) {
			inputWires[X(i)].connectTo(subCircuits[i].inputWires, GT_3_1.X);
			inputWires[Y(i)].connectTo(subCircuits[i].inputWires, GT_3_1.Y);
			subCircuits[i-1].outputWires[0].connectTo(subCircuits[i].inputWires, GT_3_1.C);
		}
	}

	protected void defineOutputWires() {
		 subCircuits[L-1].outputWires[0].connectTo(outputWires, 0);
	}

	protected void fixInternalWires() {
		Wire internalWire = subCircuits[0].inputWires[GT_3_1.C];
		internalWire.fixWire(0);
	}

	static int X(int i) {
		return 2*i+1;
	}

	static int Y(int i) {
		return 2*i;
	}
}
