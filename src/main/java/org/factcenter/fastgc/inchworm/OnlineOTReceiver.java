package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.OT.Receiver;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.util.BitMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;

public class OnlineOTReceiver extends Receiver {
	OTExtender  otExtender;
	
	Logger logger = LoggerFactory.getLogger(getClass());

	public OnlineOTReceiver(int numOfChoices) {
		super(numOfChoices, null, null);
		data = new BigInteger[numOfChoices];
	}

	/**
	 * Set associated OT Extender. All parameters should already have been set for this extender.
	 * @param otExtender
	 */
	public void setParameters(OTExtender otExtender) {
		this.otExtender = otExtender;
	}

	
	public void execProtocol(BigInteger choices) throws IOException {

        BitMatrix choiceBits = BitMatrix.valueOf(choices, numOfChoices);
		
		BitMatrix received = otExtender.receive(choiceBits);
		for (int i = 0; i < numOfChoices; ++i) {
            assert(received.isZeroPadded());
            data[i] = received.getSubMatrix(i, 1).toBigInteger();
			logger.trace("Received input label for wire {}: 0x{}", i, data[i].toString(16));
		}
	}
	
	public void init() throws IOException, InterruptedException {
		otExtender.init();
	}
}
