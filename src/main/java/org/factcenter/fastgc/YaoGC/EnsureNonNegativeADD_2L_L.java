// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC;


/*
 * The circuit has two input numbers x and y. It computes
 *          Max(0, x+y),
 * where x is assumed to be non-negative, y can be negative.
 */
public class EnsureNonNegativeADD_2L_L extends CompositeCircuit {
	private final int L;

	private static final int ADD = 0;
	private static final int MUX = 1;

	public EnsureNonNegativeADD_2L_L(CircuitGlobals globals, int l) {
		super(globals, 2*l, l, 2, "EnsureNonNegativeADD_" + (2*l) + "_" + l);

		L = l;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		subCircuits[ADD] = new ADD_2L_Lplus1(globals, L);
		subCircuits[MUX] = new MUX_2Lplus1_L(globals, L);
	}

	protected void connectWires() {
		for (int i = 0; i < L; i++) {
			inputWires[X(i)].connectTo(subCircuits[ADD].inputWires, ADD_2L_Lplus1.X(i));
			inputWires[Y(i)].connectTo(subCircuits[ADD].inputWires, ADD_2L_Lplus1.Y(i));

			subCircuits[ADD].outputWires[i].connectTo(subCircuits[MUX].inputWires, 
					MUX_2Lplus1_L.X(i));
		}

		subCircuits[ADD].outputWires[L-1].connectTo(subCircuits[MUX].inputWires, 2*L);
	}

	protected void defineOutputWires() {
		System.arraycopy(subCircuits[MUX].outputWires, 0, outputWires, 0, L);
	}
	
	@Override
	protected void fixInternalWires() {
		Wire internalWire;
		for (int i = 0; i < L; i++) {
			internalWire = subCircuits[MUX].inputWires[MUX_2Lplus1_L.Y(i)];
			internalWire.fixWire(0);
		}
		internalWire = subCircuits[ADD].inputWires[2*L];
		internalWire.fixWire(0); // todo: find a place to test this
	}

	private int X(int i) {
		return i + L;
	}

	private int Y(int i) {
		return i;
	}
}