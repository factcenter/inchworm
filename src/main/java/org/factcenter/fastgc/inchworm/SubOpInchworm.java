package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.util.BitMatrix;

import java.util.Random;

/**
 * Implementation of N bit adder with carry-out client / server circuit
 * designed to be used inside an Inchworm VM. This class can be used
 * instead of the separate AddOpClient / AddOpServer op classes.
 */
public class SubOpInchworm extends InchwormOpCommon {
	
	/**
	 * Width of adder (in bits).
	 */
	private int bitWidth;

	
	/**
	 * Constructor for using the op circuit from an Inchworm secure computation
	 * session, using Inchworm OT-Extension and channel object input / output
	 * streams.
	 * 
	 * @param serverOp - true if server, false otherwise.
	 * @param rand -  - a secure random number generator.
	 * @param labelBitLength - Length in bits of a single label.
	 * @param bitWidth - Op bit width.
	 * @param toPeer - Communication channel.
	 * @param otExtender - OT Extender.
	 */
	public SubOpInchworm(boolean serverOp, Random rand, int labelBitLength,
			int bitWidth, Channel toPeer, OTExtender otExtender) {
		super(rand, labelBitLength);

		this.bitWidth = bitWidth;
		this.serverMode = serverOp;
		otNumOfPairs = 3 * bitWidth + 4;
		setParameters(toPeer, otExtender);

		try {

			// Instantiate and build the adder circuit.
			ccs = new Circuit[1];
			ccs[0] = new AddOpCircuit(globals, bitWidth);

			if (serverMode)
				generateLabelPairs();

			super.init();
			createCircuits(serverMode);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sets the client / server data inputs into the and circuit.
	 * 
	 * @param xShare - value share of player x.
	 * @param yShare - value share of player x.
	 * @param rValue - random value for sharing the result.
	 */
	public void setData(BitMatrix xShare, BitMatrix yShare, BitMatrix rValue) {
		choices = rValue.toBigInteger(bitWidth + 2).shiftLeft(bitWidth + 2)
				.add(yShare.toBigInteger(bitWidth));
		choices = choices.shiftLeft(bitWidth).add(xShare.toBigInteger(bitWidth));
	}
	
	
}
