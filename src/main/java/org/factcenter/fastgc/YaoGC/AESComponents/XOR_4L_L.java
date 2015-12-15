// Copyright (C) 2010 by Yan Huang <yh8h@virginia.edu>

package org.factcenter.fastgc.YaoGC.AESComponents;

import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;
import org.factcenter.fastgc.YaoGC.XOR_2L_L;

public class XOR_4L_L extends CompositeCircuit {

	public XOR_4L_L(CircuitGlobals globals, int l) {
		super(globals, 4 * l, l, 3, "XOR_" + (4 * l) + "_" + l);
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		for (int i = 0; i < nSubCircuits; i++) 
			subCircuits[i] = new XOR_2L_L(globals, outDegree);
	}
	
	protected void connectWires() {
		for (int i = 0; i < outDegree; i++) {
			inputWires[i].connectTo(subCircuits[0].inputWires, i);
			inputWires[i + outDegree].connectTo(subCircuits[0].inputWires, i
					+ outDegree);

			inputWires[i + 2 * outDegree].connectTo(subCircuits[1].inputWires,
					i);
			inputWires[i + 3 * outDegree].connectTo(subCircuits[1].inputWires,
					i + outDegree);

			subCircuits[0].outputWires[i].connectTo(subCircuits[2].inputWires,
					i);
			subCircuits[1].outputWires[i].connectTo(subCircuits[2].inputWires,
					i + outDegree);
		}
	}

	protected void defineOutputWires() {
		System.arraycopy(subCircuits[2].outputWires, 0, outputWires, 0,
				outDegree);
	}
}