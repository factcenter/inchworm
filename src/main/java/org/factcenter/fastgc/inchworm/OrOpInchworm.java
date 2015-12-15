package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.Utils.Utils;
import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;

import java.math.BigInteger;
import java.util.Random;
/**
 * Implementation of N bits bitwise or operator client / server circuit
 * designed to be used inside an Inchworm VM. This class can be used
 * instead of the separate OrOpClient / OrOpServer op classes.
 */
public class OrOpInchworm extends InchwormOpCommon {
	
	/*
	 * Width of mux21-op (in bits).
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
	public OrOpInchworm(boolean serverOp, Random rand, int labelBitLength,
			int bitWidth, Channel toPeer, OTExtender otExtender) {
		super(rand, labelBitLength);

		this.bitWidth = bitWidth;
		this.serverMode = serverOp;
		otNumOfPairs = 3 * bitWidth;
		setParameters(toPeer, otExtender);

		try {

			// Instantiate and build the bitwise or circuit.
			ccs = new Circuit[1];
			ccs[0] = new OrOpCircuit(globals, bitWidth);

			if (serverMode)
				generateLabelPairs();

			super.init();
			createCircuits(serverMode);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * Sets the client data inputs into the op circuit.
	 * 
	 * @param xShare - value share of player x.
	 * @param yShare - value share of player x.
	 * @param rValue - random value for sharing the result.
	 */
	public void setData(long xShare, long yShare, long rValue) {
		choices = BigInteger.valueOf(rValue).shiftLeft(bitWidth)
				.add(Utils.getUnsignedBigInteger(yShare, bitWidth));
		choices = choices.shiftLeft(bitWidth).add(Utils.getUnsignedBigInteger(xShare, bitWidth));
	}
	
}
