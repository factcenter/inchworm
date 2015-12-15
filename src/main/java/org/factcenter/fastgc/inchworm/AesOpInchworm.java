package org.factcenter.fastgc.inchworm;

import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.util.BitMatrix;

import java.util.Random;

/**
 * Implementation of AES encryption client / server op designed to be used
 * inside an Inchworm VM. The client side op provides the text to encrypt,
 * the server side op provides the encryption key.
 */
public class AesOpInchworm extends AesOpCommon {

	/**
	 * Constructor for using the op circuit from an Inchworm secure computation session,
	 * using Inchworm OT-Extension and channel object input / output streams.
	 * 
	 * @param serverOp
	 *            - true if server, false otherwise.
	 * @param rand
	 *            - a secure random number generator.
	 * @param labelBitLength
	 *            - Length in bits of a single label.
	 * @param Nk
	 *            - Number of 32-bit words comprising the Cipher Key (Nk = 4, 6, or 8).
	 * @param toPeer
	 *            - Communication channel.
	 * @param otExtender
	 *            - OT Extender.
	 */
	public AesOpInchworm(boolean serverOp, Random rand, int labelBitLength, int Nk,
			Channel toPeer, OTExtender otExtender) {
		super(serverOp, rand, labelBitLength, Nk);

		// Client sets otNumOfPairs (size of block to cipher = 128 bits).
		otNumOfPairs = Nb * 32;
		setParameters(toPeer, otExtender);
		
		try {
			if (serverMode) {
				generateLabelPairs();
			}

			super.init();
			createCircuits(serverMode);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Sets the client(text) / server (key) data into the op.
	 * 
	 * @param block
	 *             - BitMatrix containing the key or a 128 bit block to encrypt.
	 */
	public void setData(BitMatrix block) {
		if (serverMode) {
			// Server gets the key.
			initServer(block);
		} else {
			// Client gets the message.
			msg = block;
		}
	}


}
