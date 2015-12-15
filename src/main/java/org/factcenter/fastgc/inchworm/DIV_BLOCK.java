package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.*;

/**
 * DIV_BLOCK - A modified 1 Bit full subtractor (the basic building block of an
 * attempt subtraction combinational divider). Input ports order is: A_bit
 * (dividend, X), B_bit (divisor, Y), Li, OS. Output ports order is: DTag, Lo.
 */
public class DIV_BLOCK extends CompositeCircuit {

	/**
	 * Input ports.
	 */
	public final static int A = 0;
	public final static int B = 1;
	public final static int Li = 2;
	public final static int OS = 3;

	/**
	 * Output ports.
	 */
	public final static int Lo = 0;
	public final static int DTag = 1;

	/**
	 * Sub-circuits instances.
	 */
	private AND_2_1 and1Cell;
	private AND_2_1 and2Cell;
	private AND_2_1 and3Cell;
	private AND_2_1 and4Cell;

	private XOR_2_1 xor1Cell;
	private XOR_2_1 xor2Cell;

	private INVERTER inverter1;
	private INVERTER inverter2;
	private INVERTER inverter3;

	private OR_2_1 or1Cell;
	private OR_2_1 or2Cell;

	/**
	 * Constructs a new {@code DIV_BLOCK} object.
	 * 
	 * @param globals - Global circuit parameters.
	 */
	public DIV_BLOCK(CircuitGlobals globals) {
		super(globals, 4, 2, 11, "DIV_BLOCK");

	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		int circuitNdx = 0;
		and1Cell = AND_2_1.newInstance(globals, isForGarbling);
		subCircuits[circuitNdx++] = and1Cell;
		and2Cell = AND_2_1.newInstance(globals, isForGarbling);
		subCircuits[circuitNdx++] = and2Cell;
		and3Cell = AND_2_1.newInstance(globals, isForGarbling);
		subCircuits[circuitNdx++] = and3Cell;
		and4Cell = AND_2_1.newInstance(globals, isForGarbling);
		subCircuits[circuitNdx++] = and4Cell;
		xor1Cell = new XOR_2_1(globals);
		subCircuits[circuitNdx++] = xor1Cell;
		xor2Cell = new XOR_2_1(globals);
		subCircuits[circuitNdx++] = xor2Cell;
		or1Cell = OR_2_1.newInstance(globals, isForGarbling);
		subCircuits[circuitNdx++] = or1Cell;
		or2Cell = OR_2_1.newInstance(globals, isForGarbling);
		subCircuits[circuitNdx++] = or2Cell;
		inverter1 = new INVERTER(globals);
		subCircuits[circuitNdx++] = inverter1;
		inverter2 = new INVERTER(globals);
		subCircuits[circuitNdx++] = inverter2;
		inverter3 = new INVERTER(globals);
		subCircuits[circuitNdx++] = inverter3;
	}

	@Override
	protected void connectWires() {
		connectInputWires();
		connectInternalWires();
	}

	@Override
	protected void defineOutputWires() {
        or1Cell.outputWires[0].connectTo(outputWires, Lo);
        or2Cell.outputWires[0].connectTo(outputWires, DTag);
	}

	/**
	 * Connect A,B, LI, OS inputs.
	 */
	private void connectInputWires() {
		inputWires[A].connectTo(xor1Cell.inputWires, 1);
		inputWires[A].connectTo(inverter1.inputWires, 0);
		// = = = = = = = = = = = = = = = =
		// NOTE: OS signal routing swapped.
		// inputWires[A].connectTo(and4Cell.inputWires, 0);
		inputWires[A].connectTo(and3Cell.inputWires, 0);
		// = = = = = = = = = = = = = = = =
		inputWires[B].connectTo(xor1Cell.inputWires, 0);
		inputWires[B].connectTo(and1Cell.inputWires, 1);
		inputWires[Li].connectTo(xor2Cell.inputWires, 0);
		inputWires[Li].connectTo(and2Cell.inputWires, 1);
		inputWires[OS].connectTo(and3Cell.inputWires, 1);
		inputWires[OS].connectTo(inverter3.inputWires, 0);
	}

	/**
	 * Connect internal wires.
	 */
	private void connectInternalWires() {
		inverter1.outputWires[0].connectTo(and1Cell.inputWires, 0);
		xor1Cell.outputWires[0].connectTo(inverter2.inputWires, 0);
		xor1Cell.outputWires[0].connectTo(xor2Cell.inputWires, 1);
		inverter2.outputWires[0].connectTo(and2Cell.inputWires, 0);
		and1Cell.outputWires[0].connectTo(or1Cell.inputWires, 0);
		and2Cell.outputWires[0].connectTo(or1Cell.inputWires, 1);
		// = = = = = = = = = = = = = = = =
		// NOTE: OS signal routing swapped.
		// xor2Cell.outputWires[0].connectTo(and3Cell.inputWires, 0);
		xor2Cell.outputWires[0].connectTo(and4Cell.inputWires, 0);
		// = = = = = = = = = = = = = = = =
		inverter3.outputWires[0].connectTo(and4Cell.inputWires, 1);
		and3Cell.outputWires[0].connectTo(or2Cell.inputWires, 0);
		and4Cell.outputWires[0].connectTo(or2Cell.inputWires, 1);
	}

}
