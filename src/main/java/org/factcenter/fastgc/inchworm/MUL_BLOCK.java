package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.ADD_3_2;
import org.factcenter.fastgc.YaoGC.AND_2_1;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;

/**
 * MUL_BLOCK - Basic building block of a partial-products combinational multiplier.
 * Input ports order is: x_bit, y_bit, carry_in, sum_in.
 * Output ports order is: sum_out, carry_out.
 */
public class MUL_BLOCK extends CompositeCircuit {

	/**
	 * Input ports.
	 */
	public final static int X = 0;
	public final static int Y = 1;
	public final static int C_IN = 2;
	public final static int SUM_IN = 3;
	
	/**
	 * Output ports.
	 */
	public final static int SUM_OUT = 0;
	public final static int C_OUT = 1;
	
	/**
	 * Sub-circuits instances.
	 */
	private AND_2_1 andCell;
	private ADD_3_2 fullAdder;

	/**
	 * Constructs a new {@code MUL_BLOCK} object.
	 * @param globals - Global circuit parameters.
	 */
	public MUL_BLOCK(CircuitGlobals globals) {
		super(globals, 4, 2, 2, "MUL_BLOCK");

	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		andCell = AND_2_1.newInstance(globals, isForGarbling);
		subCircuits[0] = andCell;
		fullAdder = new ADD_3_2(globals);
		subCircuits[1] = fullAdder;

	}

	@Override
	protected void connectWires() {
		// Connect x,y inputs to the and cell.
		inputWires[X].connectTo(andCell.inputWires, 0);
		inputWires[Y].connectTo(andCell.inputWires, 1);
		
		andCell.outputWires[0].connectTo(fullAdder.inputWires, Y);
		inputWires[SUM_IN].connectTo(fullAdder.inputWires, X);
		inputWires[C_IN].connectTo(fullAdder.inputWires, C_IN);

	}

	@Override
	protected void defineOutputWires() {
		 fullAdder.outputWires[SUM_OUT].connectTo(outputWires, SUM_OUT);
		 fullAdder.outputWires[C_OUT].connectTo(outputWires, C_OUT);

	}

}
