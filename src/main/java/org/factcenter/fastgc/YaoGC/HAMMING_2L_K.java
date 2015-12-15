// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC;


public class HAMMING_2L_K extends CompositeCircuit {
	private final int COUNTER;

	public HAMMING_2L_K(CircuitGlobals globals, int l, int k) {
		// The COUNTER has k bits output including the carry-out of the adder.
		super(globals, 2*l, k, l+1, "Hamming_" + (2*l) + "_" + k);

		COUNTER = l;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		for (int i = 0; i < inDegree/2; i++)
			subCircuits[i]  = new XOR_2_1(globals);
		subCircuits[COUNTER] = new COUNTER_L_K(globals, inDegree/2, outDegree);
	}

	protected void connectWires()  {
		for (int i = 0; i < subCircuits.length-1; i++) {
			inputWires[X(i)].connectTo(subCircuits[i].inputWires, 0);
			inputWires[Y(i)].connectTo(subCircuits[i].inputWires, 1);

			subCircuits[i].outputWires[0].connectTo(subCircuits[COUNTER].inputWires, i);
		}
	}

	protected void defineOutputWires() {
		System.arraycopy(subCircuits[COUNTER].outputWires, 0, outputWires, 0, outDegree);
	}

	private int X(int i) {
		return i + inDegree/2;
	}

	private int Y(int i) {
		return i;
	}
}