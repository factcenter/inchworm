// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC;


/**
 * Converts a unary counter of size 2^k to a binary counter of size k?
 */
public class COUNTER_2toK_K extends CompositeCircuit {

	public COUNTER_2toK_K(CircuitGlobals globals, int k) {
		super(globals, 1<<(k-1), k, (1<<(k-1))-1, "COUNTER_" + (1<<(k-1)) + "_" + k);
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		int i = 0;
		for (int level = 0; level < outDegree-1; level++){
			int l = outDegree - level - 1;
			for (int x = 0; x < (1<<level); x++) {
				subCircuits[i] = new ADD_2L_Lplus1(globals, l);
				i++;
			}
		}
	}
	
	@Override
	protected void fixInternalWires() {
		int i = 0;
		for (int level = 0; level < outDegree-1; level++){
			int l = outDegree - level - 1;
			for (int x = 0; x < (1<<level); x++) {
				Wire internalWire = subCircuits[i].inputWires[2*l];
				internalWire.fixWire(0); // todo: find a place to test this
			}
		}
	}
	

	protected void connectWires() {
		int i = 0, j = 0;
		for (int level = 0; level < outDegree-1; level++) {
			if (level == outDegree-2) {
				for (int x = 0; x < (1<<level); x++) {
					((ADD_2L_Lplus1) subCircuits[i]).
					connectWiresToXY(inputWires, j, inputWires, j+1);
					i++; j += 2;
				}
			}
			else {
				for (int x = 0; x < (1<<level); x++) {
					((ADD_2L_Lplus1) subCircuits[i]).
					connectWiresToXY(subCircuits[2*i+1].outputWires, 0, 
							subCircuits[2*i+2].outputWires, 0);
					i++;
				}
			}
		}
	}

	protected void defineOutputWires() {
		System.arraycopy(subCircuits[0].outputWires, 0, outputWires, 0, outDegree);
	}
}