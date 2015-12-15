package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.Utils.Utils;
import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;

import java.util.Random;

/**
 * Implementation of N bit data mux21 selector client / server circuit
 * designed to be used inside an Inchworm VM. This class can be used
 * instead of the separate MuxOpClient / MuxOpServer op classes.
 */
public class MuxOpInchworm extends InchwormOpCommon {

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
	public MuxOpInchworm(boolean serverOp, Random rand, int labelBitLength,
			int bitWidth, Channel toPeer, OTExtender otExtender) {
		super(rand, labelBitLength);

		this.bitWidth = bitWidth;
		this.serverMode = serverOp;
		otNumOfPairs = 3 * bitWidth + 1;
		setParameters(toPeer, otExtender);

		try {

			// Instantiate and build the Mux21 circuit.
			ccs = new Circuit[1];
			ccs[0] = new MuxOpCircuit(globals, bitWidth);

			if (serverMode)
				generateLabelPairs();

			super.init();
			createCircuits(serverMode);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sets the client data inputs into the mux circuit. The setup places the
	 * mux choice bit (bit #0 of aShare) in the highest position.
	 * 
	 * Input wires order: (MSB to LSB) share of A input (first bit) + share of B
	 * input + share of C input + random value).
	 * 
	 * @param aShare - value share of input b (lsb is the selection bit).
	 * @param bShare - value share of input b.
	 * @param cShare - value share of input c.
	 * @param rValue - random value for sharing the result.
	 */
	public void setData(long aShare, long bShare, long cShare, long rValue) {
		//aShare = makeParamPositive(aShare);
		//bShare = makeParamPositive(bShare);
		//cShare = makeParamPositive(cShare);
		//rValue = makeParamPositive(rValue);
		choices = Utils.getUnsignedBigInteger(aShare, bitWidth).shiftLeft(bitWidth)
				.add(Utils.getUnsignedBigInteger(bShare, bitWidth));
		choices = choices.shiftLeft(bitWidth).add(Utils.getUnsignedBigInteger(cShare, bitWidth));
		choices = choices.shiftLeft(bitWidth).add(Utils.getUnsignedBigInteger(rValue, bitWidth));
	}

}
