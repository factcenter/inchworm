// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC;

import org.factcenter.fastgc.Utils.Utils;

import java.io.IOException;
import java.math.BigInteger;

/**
 * Base class for simple gates with 2 input wires and a single output wire.
 */
public abstract class SimpleCircuit_2_1 extends Circuit {

	/**
	 * Gate truth table
	 */
	protected BigInteger[][] gtt;

	public SimpleCircuit_2_1(CircuitGlobals globals, String name) {
		super(globals, 2, 1, name);
	}

	@Override
	public void build(boolean isForGarbling)  {
		createInputWires();
		createOutputWires();
	}

	protected void createInputWires() {
		super.createInputWires();

		for (int i = 0; i < inDegree; i++) 
			inputWires[i].addObserver(this, new TransitiveObservable.Socket(inputWires, i));
	}

	protected void createOutputWires() {
        new Wire(globals, outputWires, 0);
	}

	protected void execute() {
		Wire inWireL = inputWires[0];
		Wire inWireR = inputWires[1];
		Wire outWire = outputWires[0];

		if (inWireL.value != Wire.UNKNOWN_SIG && inWireR.value != Wire.UNKNOWN_SIG) {
			compute();
		}
		else if (inWireL.value != Wire.UNKNOWN_SIG) {
			if (shortCut())
				outWire.invd = false;
			else {
				// We couldn't shortcut, but we know the output wire has the same value 
				// as the unknown input wire.
				outWire.value = Wire.UNKNOWN_SIG;
				outWire.invd = inWireR.invd;
				outWire.setLabel(inWireR.lbl);
			}
		}
		else if (inWireR.value != Wire.UNKNOWN_SIG) {
			if (shortCut()) 
				outWire.invd = false;
			else {
				// We couldn't shortcut, but we know the output wire has the same value 
				// as the unknown input wire.
				outWire.value = Wire.UNKNOWN_SIG;
				outWire.invd = inWireL.invd;
				outWire.setLabel(inWireL.lbl);
			}
		}
		else {
			outWire.value = Wire.UNKNOWN_SIG;
			outWire.invd = false;

			if (collapse()) {

			}
			else {
				execYao();
			}
		}

		outWire.setReady(Math.max(inWireL.execSerial, inWireR.execSerial));
	}

	protected abstract void execYao();

	/**
	 * Compute the output value directly from input values if possible.
	 * (e.g., if one of the input wires in an OR gate has value 1)
	 * @return true if shortcut was computed, false otherwise.
	 */
	protected abstract boolean shortCut();
	
	/**
	 * Compute output label for trivial circuit collapse 
	 * (e.g., if both input wires have the same label)
	 * @return true if circuit was collapsed, otherwise false.
	 */
	protected abstract boolean collapse();

	/**
	 * Send (Encrypted) Gate Truth Table to Client (should only be called by Server).
	 */
	protected void sendGTT() {
		try {
			// We write the serial number of the output wire because we can't be sure the receiver 
			// constructed circuits in the same order, and the serial number is a critical
			// component of the garbled circuit computation.
			oos.writeInt(outputWires[0].serialNum);
			Utils.writeBigInteger(gtt[0][1], 10, oos);
			Utils.writeBigInteger(gtt[1][0], 10, oos);
			Utils.writeBigInteger(gtt[1][1], 10, oos);

			oos.flush();
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Receive Encrypted Gate Truth Table from Server (should only be called by client)
	 */
	protected void receiveGTT() {
		try {
			// We update the serial number so that it will match the server's 
			// numbering.
			outputWires[0].serialNum = ois.readInt();
			
			gtt = new BigInteger[2][2];

			gtt[0][0] = BigInteger.ZERO;
			gtt[0][1] = Utils.readBigInteger(10, ois);
			gtt[1][0] = Utils.readBigInteger(10, ois);
			gtt[1][1] = Utils.readBigInteger(10, ois);
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	protected void encryptTruthTable() {
		Wire inWireL = inputWires[0];
		Wire inWireR = inputWires[1];
		Wire outWire = outputWires[0];

		BigInteger[] labelL = {inWireL.lbl, inWireL.conjugate()};
		if (inWireL.invd == true) {
			BigInteger tmp = labelL[0];
			labelL[0] = labelL[1];
			labelL[1] = tmp;
		}

		BigInteger[] labelR = {inWireR.lbl, inWireR.conjugate()};
		if (inWireR.invd == true) {
			BigInteger tmp = labelR[0];
			labelR[0] = labelR[1];
			labelR[1] = tmp;
		}

		int k = outWire.serialNum;

		int cL = inWireL.lbl.testBit(0) ? 1 : 0;
		int cR = inWireR.lbl.testBit(0) ? 1 : 0;

		if (cL != 0 || cR != 0)
			gtt[0 ^ cL][0 ^ cR] = globals.cipher.encrypt(labelL[0], labelR[0], k, gtt[0 ^ cL][0 ^ cR]);
		if (cL != 0 || cR != 1)
			gtt[0 ^ cL][1 ^ cR] = globals.cipher.encrypt(labelL[0], labelR[1], k, gtt[0 ^ cL][1 ^ cR]);
		if (cL != 1 || cR != 0)
			gtt[1 ^ cL][0 ^ cR] = globals.cipher.encrypt(labelL[1], labelR[0], k, gtt[1 ^ cL][0 ^ cR]);
		if (cL != 1 || cR != 1)
			gtt[1 ^ cL][1 ^ cR] = globals.cipher.encrypt(labelL[1], labelR[1], k, gtt[1 ^ cL][1 ^ cR]);
	}
}
