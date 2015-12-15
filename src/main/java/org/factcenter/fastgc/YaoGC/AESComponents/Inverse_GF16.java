// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC.AESComponents;

import org.factcenter.fastgc.YaoGC.AND_2_1;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;
import org.factcenter.fastgc.YaoGC.XOR_L_1;

class Inverse_GF16 extends CompositeCircuit {
	private static final byte A = 0;
	private static final byte q0 = 1;
	private static final byte q1 = 2;
	private static final byte q2 = 3;
	private static final byte q3 = 4;
	private static final byte and01 = 5;
	private static final byte and02 = 6;
	private static final byte and03 = 7;
	private static final byte and12 = 8;
	private static final byte and13 = 9;
	private static final byte and23 = 10;
	private static final byte and012 = 11;
	private static final byte and123 = 12;
	private static final byte and023 = 13;
	private static final byte and013 = 14;

	public Inverse_GF16(CircuitGlobals globals) {
		super(globals, 4, 4, 15, "Inverse_GF16");
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		subCircuits[A] = new XOR_L_1(globals, 4);
		subCircuits[q0] = new XOR_L_1(globals, 5);
		subCircuits[q1] = new XOR_L_1(globals, 6);
		subCircuits[q2] = new XOR_L_1(globals, 6);
		subCircuits[q3] = new XOR_L_1(globals, 4);

		subCircuits[and01] = AND_2_1.newInstance(globals, isForGarbling);
		subCircuits[and02] = AND_2_1.newInstance(globals, isForGarbling);
		subCircuits[and03] = AND_2_1.newInstance(globals, isForGarbling);
		subCircuits[and12] = AND_2_1.newInstance(globals, isForGarbling);
		subCircuits[and13] = AND_2_1.newInstance(globals, isForGarbling);
		subCircuits[and23] = AND_2_1.newInstance(globals, isForGarbling);
		subCircuits[and012] = AND_2_1.newInstance(globals, isForGarbling);
		subCircuits[and123] = AND_2_1.newInstance(globals, isForGarbling);
		subCircuits[and023] = AND_2_1.newInstance(globals, isForGarbling);
		subCircuits[and013] = AND_2_1.newInstance(globals, isForGarbling);
	}

	protected void connectWires() {
		inputWires[0].connectTo(subCircuits[and01].inputWires, 0);
		inputWires[1].connectTo(subCircuits[and01].inputWires, 1);

		inputWires[0].connectTo(subCircuits[and02].inputWires, 0);
		inputWires[2].connectTo(subCircuits[and02].inputWires, 1);

		inputWires[0].connectTo(subCircuits[and03].inputWires, 0);
		inputWires[3].connectTo(subCircuits[and03].inputWires, 1);

		inputWires[1].connectTo(subCircuits[and12].inputWires, 0);
		inputWires[2].connectTo(subCircuits[and12].inputWires, 1);

		inputWires[1].connectTo(subCircuits[and13].inputWires, 0);
		inputWires[3].connectTo(subCircuits[and13].inputWires, 1);

		inputWires[2].connectTo(subCircuits[and23].inputWires, 0);
		inputWires[3].connectTo(subCircuits[and23].inputWires, 1);

		subCircuits[and01].outputWires[0].connectTo(
				subCircuits[and012].inputWires, 0);
		inputWires[2].connectTo(subCircuits[and012].inputWires, 1);

		subCircuits[and12].outputWires[0].connectTo(
				subCircuits[and123].inputWires, 0);
		inputWires[3].connectTo(subCircuits[and123].inputWires, 1);

		subCircuits[and02].outputWires[0].connectTo(
				subCircuits[and023].inputWires, 0);
		inputWires[3].connectTo(subCircuits[and023].inputWires, 1);

		subCircuits[and01].outputWires[0].connectTo(
				subCircuits[and013].inputWires, 0);
		inputWires[3].connectTo(subCircuits[and013].inputWires, 1);

		inputWires[1].connectTo(subCircuits[A].inputWires, 0);
		inputWires[2].connectTo(subCircuits[A].inputWires, 1);
		inputWires[3].connectTo(subCircuits[A].inputWires, 2);
		subCircuits[and123].outputWires[0].connectTo(subCircuits[A].inputWires,
				3);

		subCircuits[A].outputWires[0].connectTo(subCircuits[q0].inputWires, 0);
		inputWires[0].connectTo(subCircuits[q0].inputWires, 1);
		subCircuits[and02].outputWires[0].connectTo(subCircuits[q0].inputWires,
				2);
		subCircuits[and12].outputWires[0].connectTo(subCircuits[q0].inputWires,
				3);
		subCircuits[and012].outputWires[0].connectTo(
				subCircuits[q0].inputWires, 4);

		subCircuits[and01].outputWires[0].connectTo(subCircuits[q1].inputWires,
				0);
		subCircuits[and02].outputWires[0].connectTo(subCircuits[q1].inputWires,
				1);
		subCircuits[and12].outputWires[0].connectTo(subCircuits[q1].inputWires,
				2);
		inputWires[3].connectTo(subCircuits[q1].inputWires, 3);
		subCircuits[and13].outputWires[0].connectTo(subCircuits[q1].inputWires,
				4);
		subCircuits[and013].outputWires[0].connectTo(
				subCircuits[q1].inputWires, 5);

		subCircuits[and01].outputWires[0].connectTo(subCircuits[q2].inputWires,
				0);
		inputWires[2].connectTo(subCircuits[q2].inputWires, 1);
		subCircuits[and02].outputWires[0].connectTo(subCircuits[q2].inputWires,
				2);
		inputWires[3].connectTo(subCircuits[q2].inputWires, 3);
		subCircuits[and03].outputWires[0].connectTo(subCircuits[q2].inputWires,
				4);
		subCircuits[and023].outputWires[0].connectTo(
				subCircuits[q2].inputWires, 5);

		subCircuits[A].outputWires[0].connectTo(subCircuits[q3].inputWires, 0);
		subCircuits[and03].outputWires[0].connectTo(subCircuits[q3].inputWires,
				1);
		subCircuits[and13].outputWires[0].connectTo(subCircuits[q3].inputWires,
				2);
		subCircuits[and23].outputWires[0].connectTo(subCircuits[q3].inputWires,
				3);
	}

	protected void defineOutputWires() {
		 subCircuits[q0].outputWires[0].connectTo(outputWires, 0);
		 subCircuits[q1].outputWires[0].connectTo(outputWires, 1);
		 subCircuits[q2].outputWires[0].connectTo(outputWires, 2);
		 subCircuits[q3].outputWires[0].connectTo(outputWires, 3);
	}
}