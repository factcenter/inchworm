package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;

import java.util.Random;

/**
 * Implementation of the multiplier client / server circuit designed to be
 * used inside an Inchworm VM.
 */
public class MulOpInchworm extends InchwormOpWrapper {
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
	public MulOpInchworm(boolean serverOp, Random rand, int labelBitLength,
			final int bitWidth, Channel toPeer, OTExtender otExtender) {
        super(serverOp, rand, labelBitLength, toPeer, otExtender,
                new CircuitFactory() {
                    @Override
                    public Circuit createCircuit(CircuitGlobals globals) {
                        return new MUL_K(globals, bitWidth);
                    }
                });
    }
}
