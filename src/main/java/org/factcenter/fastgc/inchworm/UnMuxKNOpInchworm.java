package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
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
public class UnMuxKNOpInchworm extends InchwormOpCommon {

	/*
	 * Width of mux21-op (in bits).
	 */

	private int blockSize;

    private int numBlocks;

    private int ctrlBits;


	/**
	 * Constructor for using the op circuit from an Inchworm secure computation
	 * session, using Inchworm OT-Extension and channel object input / output
	 * streams.
	 *
	 * @param serverOp - true if server, false otherwise.
	 * @param rand -  - a secure random number generator.
	 * @param labelBitLength - Length in bits of a single label.
	 * @param blockSize - Op bit width.
	 * @param toPeer - Communication channel.
	 * @param otExtender - OT Extender.
	 */
	public UnMuxKNOpInchworm(boolean serverOp, Random rand, int labelBitLength,
                             int blockSize, int numBlocks, int ctrlBits, Channel toPeer, OTExtender otExtender) {
		super(rand, labelBitLength);

		this.blockSize = blockSize;
        this.numBlocks = numBlocks;
        this.ctrlBits = ctrlBits;

		this.serverMode = serverOp;
		otNumOfPairs = blockSize * numBlocks + blockSize + ctrlBits + blockSize * numBlocks;
		setParameters(toPeer, otExtender);

		try {

            UNMUX_K_N unmux_k_n = new UNMUX_K_N(globals, blockSize, numBlocks, ctrlBits);


			ccs = new Circuit[1];

            ccs[0] = new UnshareOpReshareCircuit(globals, unmux_k_n);

			if (serverMode)
				generateLabelPairs();

			super.init();
			createCircuits(serverMode);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
