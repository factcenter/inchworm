package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;

import java.util.Random;

/**
 * Implementation of N bit bitwise and operator client / server circuit
 * designed to be used inside an Inchworm VM. This class can be used
 * instead of the separate AndOpClient / AndOpServer op classes.
 */
public class AndOpInchworm extends InchwormOpCommon {
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
	public AndOpInchworm(boolean serverOp, Random rand, int labelBitLength,
			int bitWidth, Channel toPeer, OTExtender otExtender) {
		super(rand, labelBitLength);

		this.serverMode = serverOp;
		otNumOfPairs = 3 * bitWidth;
		setParameters(toPeer, otExtender);

		try {

			// Instantiate and build the bitwise and circuit.
			ccs = new Circuit[1];
			ccs[0] = new AndOpCircuit(globals, bitWidth);

			if (serverMode)
				generateLabelPairs();

			super.init();
			createCircuits(serverMode);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
