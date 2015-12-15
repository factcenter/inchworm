// Copyright (C) 2010 by Yan Huang <yh8h@virginia.edu>

package org.factcenter.fastgc.YaoGC;


public class XOR_2L_L extends CompositeCircuit {

	public XOR_2L_L(CircuitGlobals globals, int l) {
		super(globals, 2*l, l, l, "XOR_"+(2*l)+"_"+l);
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		for (int i = 0; i < outDegree; i++) 
			subCircuits[i] = new XOR_2_1(globals);
	}

	protected void connectWires() {
		for (int i = 0; i < outDegree; i++) {
			inputWires[X(i)].connectTo(subCircuits[i].inputWires, 0);
			inputWires[Y(i)].connectTo(subCircuits[i].inputWires, 1);
		}
	}

	protected void defineOutputWires() {
		for (int i = 0; i < outDegree; i++)
			 subCircuits[i].outputWires[0].connectTo(outputWires, i);
	}

	public int X(int i) {
		return i + outDegree;
	}

	public int Y(int i) {
		return i;
	}
}