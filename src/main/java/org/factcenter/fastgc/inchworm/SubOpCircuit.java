package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.*;

/**
 * N bit adder with carry-out output port circuit implementation. Input ports
 * order is: [Player0_xShare, Player0_yShare, Player0_rValue, Player1_xShare,
 * Player1_yShare, Player1_rValue].
 * 
 * Both sides - client (right player) and server(left player) should instantiate
 * the op client/server class, set data, and run the object.
 * 
 */
public class SubOpCircuit extends CompositeCircuit {

	private final int adderBitWidth;

	private UNSHARE unshareComponent;
	private ADD_2L_Lplus1 adder;
	private SHARE shareComponent;
	private OR_L_1 orZf;
	private INVERTER negZfOr;
	private Wire[] adderXInputs;
	private Wire[] adderYInputs;
/*	AND_2_1 g1;
	INVERTER g2;
	INVERTER g3;
	INVERTER g4;
	AND_2_1 g5;
	AND_2_1 g6;
	AND_2_1 g7;
	OR_2_1 g8;
*/
	XOR_2_1 g1;
	INVERTER g2;
	XOR_2_1 g3;
	AND_2_1 g4;
	INVERTER[] rhsInverters;
	XOR_2_1 borrowXor;
	INVERTER borrowNot;
	/**
	 * Constructs a new {@code AddOpCircuit} object.
	 * @param globals - Global circuit parameters.
	 * @param cntBits - op word size.
	 */
	public SubOpCircuit(CircuitGlobals globals, int cntBits) {
		super(globals, 6 * cntBits + 8, cntBits + 4, 5 + 4 + 2 + cntBits, "Add_" + cntBits
				+ "bits");
		// 8 bits added to the inputs for the wider random values.
		// 4 bits added to the output for the CF, ZF, SF and OF.
		adderBitWidth = cntBits;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {

		// Create the sub circuits.
		unshareComponent = new UNSHARE(globals, adderBitWidth);
		adder = new ADD_2L_Lplus1(globals, adderBitWidth + 1);
		adderXInputs = new Wire[adderBitWidth + 1];
		new Wire(globals, adderXInputs, adderBitWidth);
		adderYInputs = new Wire[adderBitWidth + 1];
		new Wire(globals, adderYInputs, adderBitWidth);
		// Add 4 bits to the share component for handling CF, ZF, SF and OF.
		shareComponent = new SHARE(globals, adderBitWidth + 4);
		orZf = new OR_L_1(globals, adderBitWidth);
		negZfOr = new INVERTER(globals);
		
		g1 = new XOR_2_1(globals);
		g2 = new INVERTER(globals);
		g3 = new XOR_2_1(globals);
		g4 = AND_2_1.newInstance(globals, isForGarbling);
		borrowXor = new XOR_2_1(globals);
		borrowNot = new INVERTER(globals);
		/*g1 = AND_2_1.newInstance(globals, isForGarbling);
		g2 = new INVERTER(globals);
		g3 = new INVERTER(globals);
		g4 = new INVERTER(globals);
		g5 = AND_2_1.newInstance(globals, isForGarbling);
		g6 = AND_2_1.newInstance(globals, isForGarbling);
		g7 = AND_2_1.newInstance(globals, isForGarbling);
		g8 = OR_2_1.newInstance(globals, isForGarbling);
*/
		int i = 0;
		subCircuits[i++] = unshareComponent;
		subCircuits[i++] = adder;
		subCircuits[i++] = shareComponent;
		subCircuits[i++] = orZf;
		subCircuits[i++] = negZfOr;
		subCircuits[i++] = g1;
		subCircuits[i++] = g2;
		subCircuits[i++] = g3;
		subCircuits[i++] = g4;
		rhsInverters = new INVERTER[adderBitWidth];
		for (int j=0; j<adderBitWidth; ++j) {
			rhsInverters[j] = new INVERTER(globals);
			subCircuits[i++] = rhsInverters[j]; 
		}
		subCircuits[i++] = borrowXor;
		subCircuits[i++] = borrowNot;
/*		subCircuits[i++] = g5;
		subCircuits[i++] = g6;
		subCircuits[i++] = g7;
		subCircuits[i++] = g8;
		*/
	}

	@Override
	protected void connectWires() {

		// Wire the 4 shared inputs to the un-share component.
		unshareComponent.connectWiresToXY(inputWires, 0, inputWires,
				3 * adderBitWidth + 4);

		for (int i = 0; i < adderBitWidth; ++i) {
			unshareComponent.outputWires[adderBitWidth + i].connectTo(rhsInverters[i].inputWires, 0);
		}
		
		// todo: should the low bit be first or the high bit?
		for (int i = 0; i < adderBitWidth; ++i) {
			adderXInputs[i] = unshareComponent.outputWires[i];
			adderYInputs[i] = rhsInverters[i].outputWires[0];
		}
		
		// Wire the un-share outputs to the op circuit inputs.
		adder.connectWiresToXY(adderXInputs, 0,
				adderYInputs, 0);

		// Wire the or inputs to the adder adderBitWidth lsb bits (for handling
		// the ZF).
		orZf.connectWiresToInputs(adder.outputWires, 0);


		Wire[] adderResultWires = new Wire[adderBitWidth];
		// Wire the player's random values and the adder outputs to the share
		// component inputs.
		// we widened the adder by one bit to compute a borrow bit, and 
		// connectWiresToInputs() looks at the length of the array,
		// so we pass a wire array containing only the result, without
		// the extra bit and without the carry.
		for (int i = 0; i < adderBitWidth; ++i) {
			adderResultWires[i] = adder.outputWires[i];
		}
		
		adderXInputs[adderBitWidth].connectTo(borrowXor.inputWires, 0);
		adder.outputWires[adderBitWidth].connectTo(borrowXor.inputWires, 1);
		borrowXor.outputWires[0].connectTo(borrowNot.inputWires, 0);
		borrowNot.outputWires[0].connectTo(shareComponent.inputWires,
				3 * adderBitWidth + 8);
		
		shareComponent.connectWiresToInputs(inputWires, 2 * adderBitWidth,
				inputWires, 5 * adderBitWidth + 4, adderResultWires, 0);
		// Wire the orZf to the inverter.
		orZf.outputWires[0].connectTo(negZfOr.inputWires, 0);
		// Wire the ZF to the next free input.
		negZfOr.outputWires[0].connectTo(shareComponent.inputWires,
				3 * adderBitWidth + 9);
		// Write the MSB (sign bit) to the SF input of the share component output.
		adder.outputWires[adderBitWidth-1].connectTo(shareComponent.inputWires, 3 * adderBitWidth + 10);

		/*
		 * These seem to be correct for sub. Need to make a version for add.
		 * X_MSB   Y_MSB      SIGN     X_MSB  Y_MSB   SIGN   
		 *    |      |          ~         ~     ~      |
		      ----&---        G2|       G3|     |G4    |
		        G1|             |          --&--       |
		          -------&-------          G5|         |
		               G6|                    ----&-----
		                 |                      G7|
		                 ------------+-------------
		                            G8 
		 */
		boolean isForGarbling = true;
		
		int xMsbIndex = adderBitWidth;
		Wire xSign = unshareComponent.outputWires[adderBitWidth-1];
		Wire ySign = unshareComponent.outputWires[2*adderBitWidth-1];
		Wire sumSign = adder.outputWires[adderBitWidth-1];
		sumSign.connectTo(g1.inputWires, 0);
		ySign.connectTo(g1.inputWires, 1);
		g1.outputWires[0].connectTo(g2.inputWires, 0);
		xSign.connectTo(g3.inputWires, 0);
		ySign.connectTo(g3.inputWires, 1);
		g2.outputWires[0].connectTo(g4.inputWires, 0);
		g3.outputWires[0].connectTo(g4.inputWires, 1);
		g4.outputWires[0].connectTo(shareComponent.inputWires, 3 * adderBitWidth + 11);
	}

	protected void defineOutputWires() {

		/*-
		 * NOTE: Result is 2 bits wider than the adder width.
		 *       CF is bit # adderBitWidth
		 *       ZF is bit # adderBitWidth + 1
		 *       SF is bit # adderBitWidth + 2
		 *       OF is bit # adderBitWidth + 3
		 */

		// Wire the share outputs.
		for (int i = 0; i < adderBitWidth + 4; i++)
            shareComponent.outputWires[i].connectTo(outputWires, i);

	}
	
	@Override
	protected void fixInternalWires() {
		adder.inputWires[2*(adderBitWidth + 1)].fixWire(1);
		adderXInputs[adderBitWidth].fixWire(1);
		adderYInputs[adderBitWidth].fixWire(0);
	}
}
