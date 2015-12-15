// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC.AESComponents;

import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;
import org.factcenter.fastgc.YaoGC.XOR_2_1;

class Square extends CompositeCircuit {
	
	public Square(CircuitGlobals globals) {
		super(globals, 4, 4, 4, "Square");
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		for (int i = 0; i < nSubCircuits; i++) 
			subCircuits[i] = new XOR_2_1(globals);
	}

	protected void connectWires() {
		inputWires[0].connectTo(subCircuits[0].inputWires, 0);
		inputWires[2].connectTo(subCircuits[0].inputWires, 1);

		inputWires[2].connectTo(subCircuits[1].inputWires, 0);
		subCircuits[1].inputWires[1].fixWire(0);

		inputWires[1].connectTo(subCircuits[2].inputWires, 0);
		inputWires[3].connectTo(subCircuits[2].inputWires, 1);

		inputWires[3].connectTo(subCircuits[3].inputWires, 0);
		subCircuits[3].inputWires[1].fixWire(0);
	}

	protected void defineOutputWires() {
		 subCircuits[0].outputWires[0].connectTo(outputWires, 0);
		 subCircuits[1].outputWires[0].connectTo(outputWires, 1);
		 subCircuits[2].outputWires[0].connectTo(outputWires, 2);
		 subCircuits[3].outputWires[0].connectTo(outputWires, 3);
	}
}