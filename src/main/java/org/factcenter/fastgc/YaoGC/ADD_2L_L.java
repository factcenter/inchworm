// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC;

import java.io.IOException;


/*
 * Fig. 1 of [KSS09]
 */
/**
 * Add two inputs of length l (in bits).
 * L bit full-adder circuit implementation, NO carry-out output port.
 * Input ports: X0 X1 .... Xl-1 , Y0 Y1 .... yl-1
 */
public class ADD_2L_L extends CompositeCircuit {
	private final int L;

	public ADD_2L_L(CircuitGlobals globals, int l) {
		super(globals, 2*l, l, l, "ADD_" + (2*l) + "_" + (l+1));

		L = l;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		for (int i = 0; i < L; i++) 
			subCircuits[i] = new ADD_3_2(globals);
	}

	protected void connectWires() {
		inputWires[X(0)].connectTo(subCircuits[0].inputWires, ADD_3_2.X);
		inputWires[Y(0)].connectTo(subCircuits[0].inputWires, ADD_3_2.Y);

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
	}

	protected void fixInternalWires() {
		Wire internalWire = subCircuits[0].inputWires[ADD_3_2.CIN];
		internalWire.fixWire(0);
	}

	public int X(int i) {
		return i + L;
	}

	public int Y(int i) {
		return i;
	}
	
	/*
	 * Connect xWires[xStartPos...xStartPos+L] to the wires representing bits of
	 * X; yWires[yStartPos...yStartPos+L] to the wires representing bits of Y;
	 */
	public void connectWiresToXY(Wire[] xWires, int xStartPos, Wire[] yWires,
			int yStartPos) throws IOException {
		if (xStartPos + L > xWires.length || yStartPos + L > yWires.length)
			throw new IOException("Unmatched number of wires.");

		for (int i = 0; i < L; i++) {
			xWires[xStartPos + i].connectTo(inputWires, X(i));
			yWires[yStartPos + i].connectTo(inputWires, Y(i));
		}
	}
}