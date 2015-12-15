// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC;


public class MAX_2L_L extends CompositeCircuit {
	private final int L;

	public final static int  GT = 0;
	public final static int MUX = 1;

	public MAX_2L_L(CircuitGlobals globals, int l) {
		super(globals, 2*l, l, 2, "MAX_" + 2*l + "_" + l);
		L = l;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		subCircuits[GT]  = new GT_2L_1(globals, L);
		subCircuits[MUX] = new MUX_2Lplus1_L(globals, L);
	}

	protected void connectWires() {
		for (int i = 0; i < L; i++) {
			inputWires[i+L].connectTo(subCircuits[GT].inputWires, GT_2L_1.X(i));
			inputWires[i  ].connectTo(subCircuits[GT].inputWires, GT_2L_1.Y(i));

			inputWires[i  ].connectTo(subCircuits[MUX].inputWires, MUX_2Lplus1_L.X(i));
			inputWires[i+L].connectTo(subCircuits[MUX].inputWires, MUX_2Lplus1_L.Y(i));
		}

		subCircuits[GT].outputWires[0].connectTo(subCircuits[MUX].inputWires, 2*L);
	}

	protected void defineOutputWires() {
		for (int i = 0; i < L; i++)
			 subCircuits[MUX].outputWires[i].connectTo(outputWires, i);
	}
}
