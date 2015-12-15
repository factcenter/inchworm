package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.util.BitMatrix;

import java.util.Random;

/**
 * Implementation of MUX that selects a K-bit block from N blocks, where
 * the inputs are shares of the memory blocks and shares of the index.
 *
 * To set client data, call {@link #setData(BitMatrix...)} as
 * setData(randomness, ctrlBits, databits)
 */
//public class MuxKNOpInchworm extends InchwormOpCommon {
public class MuxKNOpInchworm extends InchwormOpWrapper {
    public MuxKNOpInchworm(boolean serverOp, Random rand, int labelBitLength,
        final int blockSize, final int numBlocks, final int ctrlBits, Channel toPeer, OTExtender otExtender) {
        super(serverOp, rand, labelBitLength, toPeer, otExtender,
                new CircuitFactory() {
                    @Override
                    public Circuit createCircuit(CircuitGlobals globals) {
                        return new MUX_K_N(globals, blockSize, numBlocks, ctrlBits);
                    }
                });
    }

//
//	/*
//	 * Width of mux21-op (in bits).
//	 */
//
//        private int blockSize;
//
//        private int numBlocks;
//
//        private int ctrlBits;
//
//
//        /**
//         * Constructor for using the op circuit from an Inchworm secure computation
//         * session, using Inchworm OT-Extension and channel object input / output
//         * streams.
//         *
//         * @param serverOp - true if server, false otherwise.
//         * @param rand -  - a secure random number generator.
//         * @param labelBitLength - Length in bits of a single label.
//         * @param blockSize - Op bit width.
//         * @param toPeer - Communication channel.
//         * @param otExtender - OT Extender.
//         */
//        public MuxKNOpInchworm(boolean serverOp, Random rand, int labelBitLength,
//        int blockSize, int numBlocks, int ctrlBits, Channel toPeer, OTExtender otExtender) {
//            super(rand, labelBitLength);
//
//            this.blockSize = blockSize;
//            this.numBlocks = numBlocks;
//            this.ctrlBits = ctrlBits;
//
//            this.serverMode = serverOp;
//            MUX_K_N mux_k_n = new MUX_K_N(globals, blockSize, numBlocks, ctrlBits);
//
//            ccs[0] = new UnshareOpReshareCircuit(globals, mux_k_n);
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
