package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;

import java.util.Random;

/**
 * Implementation of K bit combinational divider client / server circuit
 * designed to be used inside an Inchworm VM.
 */
public class DivOpInchworm extends InchwormOpWrapper {
    public DivOpInchworm(boolean serverOp, Random rand, int labelBitLength,
			final int bitWidth, Channel toPeer, OTExtender otExtender) {
        super(serverOp, rand, labelBitLength, toPeer, otExtender,
           new CircuitFactory() {
               @Override
               public Circuit createCircuit(CircuitGlobals globals) {
                   return new DIV_K_ZERO_DETECT(globals, bitWidth);
               }
           });
    }
//
//	/**
//	 * Width of op (in bits).
//	 */
//	private int bitWidth;
//
//
//	/**
//	 * Constructor for using the op circuit from an Inchworm secure computation
//	 * session, using Inchworm OT-Extension and channel object input / output
//	 * streams.
//	 *
//	 * @param serverOp - true if server, false otherwise.
//	 * @param rand -  - a secure random number generator.
//	 * @param labelBitLength - Length in bits of a single label.
//	 * @param bitWidth - Op bit width.
//	 * @param toPeer - Communication channel.
//	 * @param otExtender - OT Extender.
//	 */
//	public DivOpInchworm(boolean serverOp, Random rand, int labelBitLength,
//			int bitWidth, Channel toPeer, OTExtender otExtender) {
//		super(rand, labelBitLength);
//
//		this.bitWidth = bitWidth;
//		this.serverMode = serverOp;
//		otNumOfPairs = 4 * bitWidth;
//		setParameters(toPeer, otExtender);
//
//		try {
//
//			// Instantiate and build the divider circuit.
//			ccs = new Circuit[1];
//			ccs[0] = new DivOpCircuit(globals, bitWidth);
//
//			if (serverMode)
//				generateLabelPairs();
//
//			super.init();
//			createCircuits(serverMode);
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
}
