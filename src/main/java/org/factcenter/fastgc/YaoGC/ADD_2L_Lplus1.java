// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC;


/*
 * Fig. 1 of [KSS09]
 */
public class ADD_2L_Lplus1 extends CompositeCircuit {
	private final int L;
	private final int CIN_INDEX;

	public ADD_2L_Lplus1(CircuitGlobals globals, int l) {
		super(globals, 2*l + 1 /* +1 for cin */, l+1, l, "ADD_" + (2*l) + "_" + (l+1));

		L = l;
		CIN_INDEX = L*2;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		for (int i = 0; i < L; i++) 
			subCircuits[i] = new ADD_3_2(globals);
	}

	protected void connectWires() {
		inputWires[X(0)].connectTo(subCircuits[0].inputWires, ADD_3_2.X);
		inputWires[Y(0)].connectTo(subCircuits[0].inputWires, ADD_3_2.Y);
		inputWires[CIN_INDEX].connectTo(subCircuits[0].inputWires, ADD_3_2.CIN);

		for (int i = 1; i < L; i++) {
			inputWires[X(i)].connectTo(subCircuits[i].inputWires, ADD_3_2.X);
			inputWires[Y(i)].connectTo(subCircuits[i].inputWires, ADD_3_2.Y);
			subCircuits[i-1].outputWires[ADD_3_2.COUT].connectTo(subCircuits[i].inputWires,
					ADD_3_2.CIN);
		}
	}

	protected void defineOutputWires() {
		for (int i = 0; i < L; i++)
			 subCircuits[i].outputWires[ADD_3_2.S].connectTo(outputWires, i);
		 subCircuits[L-1].outputWires[ADD_3_2.COUT].connectTo(outputWires, L);
	}

	protected void fixInternalWires() {
		// nothing, the cin is connected from the outside now.
	}

	public static int X(int i) {
		return 2*i+1;
	}

	public static int Y(int i) {
		return 2*i;
	}

	/**
	 * Connect xWires[xStartPos...xStartPos+L] to the wires representing bits of X;
	 * yWires[yStartPos...yStartPos+L] to the wires representing bits of Y;
	 * xStartPos is the LSB of X and yStartPos is the index of the LSB of Y 
	 */
	public void connectWiresToXY(Wire[] xWires, int xStartPos, Wire[] yWires, int yStartPos) {
		if (xStartPos + L > xWires.length || yStartPos + L > yWires.length)
			throw new RuntimeException("Unmatched number of wires.");

		for (int i = 0; i < L; i++) {
			xWires[xStartPos+i].connectTo(inputWires, X(i));
			yWires[yStartPos+i].connectTo(inputWires, Y(i));
		}
	}
}
