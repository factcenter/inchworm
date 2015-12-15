package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.*;

/**
 * 
 * @author mikegarts
 *
 * <pre>
 *  {@code
 *         INPUT: [INPUT_A, CARRY_A, INPUT_B, CARRY_B]
 *         OUTPUT: bit0 = 1 if INPUT_A==INPUT_B and CARRY==0, 0 else
 *         		   bit1 = bit0 OR (CARRY_A XOR CARRY_B)
 *  }
 * </pre>
 *
 *         Note:  bit1 is carry out bit.
 */
public class CompareWithCarryCircuit extends CompositeCircuit {

	private final int compareWidth;
	
	// OFFSETS in input bits
	private final int INPUT_A;
	private final int INPUT_B;
	private final int CARRY_A;
	private final int CARRY_B;
	
	private XOR_2_1 xorCarry;
	
	private XOR_2L_L xorComparePrefixStep1;
	private OR_L_1 orCompareIndicesStep2; 

	private OR_2_1 orCompareWithCarry;
	
	private INVERTER negateOr;
	
	private OR_2_1 orCalcNextCarry;
	
	static int NUM_SUBCIRCUITS = 6;


	public CompareWithCarryCircuit(CircuitGlobals globals, int inputWidth) {
		super(globals,
				2 * inputWidth +     //(inputA, inputB)  
					   2, // (carryA, carryB)
				2, // compare result, carry to next
				NUM_SUBCIRCUITS, // sub circuits because somebody was too lazy
									// to calculate with reflection
				"CompareWithCarry" + inputWidth );
		this.compareWidth = inputWidth;

		// calculate offsets
		INPUT_A = 0;
		CARRY_A = INPUT_A + inputWidth;
		INPUT_B = CARRY_A + 1;
		CARRY_B = INPUT_B + inputWidth;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		xorCarry = new XOR_2_1(globals);
		xorComparePrefixStep1 = new XOR_2L_L(globals, compareWidth);
		orCompareIndicesStep2 = new OR_L_1(globals, compareWidth);
		orCompareWithCarry = OR_2_1.newInstance(globals, isForGarbling);
		negateOr = new INVERTER(globals);
		orCalcNextCarry = OR_2_1.newInstance(globals, isForGarbling);
		int i = 0;
		subCircuits[i++] = xorCarry;
		subCircuits[i++] = xorComparePrefixStep1;
		subCircuits[i++] = orCompareIndicesStep2;
		subCircuits[i++] = orCompareWithCarry;
		subCircuits[i++] = negateOr;
		subCircuits[i++] = orCalcNextCarry;
	}

	@Override
	protected void connectWires() {
		for (int i = 0; i < compareWidth; ++i) {
			// XOR INPUTS
			inputWires[i + INPUT_A].connectTo(xorComparePrefixStep1.inputWires, xorComparePrefixStep1.X(i));
			inputWires[i + INPUT_B].connectTo(xorComparePrefixStep1.inputWires, xorComparePrefixStep1.Y(i));
			
			// OR INPUTS
			xorComparePrefixStep1.outputWires[i].connectTo(orCompareIndicesStep2.inputWires, i);
		}
		
		// XOR CARRY
		inputWires[CARRY_A].connectTo(xorCarry.inputWires, 0);
		inputWires[CARRY_B].connectTo(xorCarry.inputWires, 1);
		
		// COMPARE CARRY WITH inputs comparison 
		xorCarry.outputWires[0].connectTo(orCompareWithCarry.inputWires, 0);
		orCompareIndicesStep2.outputWires[0].connectTo(orCompareWithCarry.inputWires, 1);
		
		// negate or!
		orCompareWithCarry.outputWires[0].connectTo(negateOr.inputWires, 0);
		
		// calc next carry
		negateOr.outputWires[0].connectTo(orCalcNextCarry.inputWires, 0);
		xorCarry.outputWires[0].connectTo(orCalcNextCarry.inputWires, 1);

	}

	public int carryIndexX()
	{
		return compareWidth;
	}
	public int carryIndexY()
	{
		return compareWidth*2 + 1;
	}
	
	@Override
	protected void defineOutputWires() {
        negateOr.outputWires[0].connectTo(outputWires, 0);
        orCalcNextCarry.outputWires[0].connectTo(outputWires, 1);
	}
}
