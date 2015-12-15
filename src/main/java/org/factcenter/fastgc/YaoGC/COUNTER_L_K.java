// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC;


public class COUNTER_L_K extends CompositeCircuit {

	public COUNTER_L_K(CircuitGlobals globals, int l, int k) {
		super(globals, l, k, 1, "COUNTER_" + l + "_" + k);

		if (l > (2<<k)) {
			System.err.println("The COUNTER will overflow.");
			(new Exception()).printStackTrace();
			System.exit(1);
		}
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		subCircuits[0] = new COUNTER_2toK_K(globals, outDegree);
	}

	protected void connectWires() {
		for (int i = 0; i < inDegree; i++)
			inputWires[i].connectTo(subCircuits[0].inputWires, i);
	}

	protected void defineOutputWires() {
		System.arraycopy(subCircuits[0].outputWires, 0, outputWires, 0, outDegree);
	}

	protected void fixInternalWires() {
		for (int i = inDegree; i < subCircuits[0].inDegree; i++) 
			subCircuits[0].inputWires[i].fixWire(0);
	}
}