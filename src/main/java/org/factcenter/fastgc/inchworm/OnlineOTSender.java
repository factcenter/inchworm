package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.OT.Sender;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.protocols.concrete.DefaultOTExtender;
import org.factcenter.qilin.util.BitMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;

public class OnlineOTSender extends Sender {
	OTExtender otExtender;

	final Logger logger = LoggerFactory.getLogger(getClass());
	
	public OnlineOTSender(int numOfPairs, int msgBitLength) {
		super(numOfPairs, msgBitLength, null, null);
	}

	/**
	 * Set associated OT Extender.
	 * {@link DefaultOTExtender#setParameters(Channel, Random)} and
	 * {@link DefaultOTExtender#setServerParameters(Channel, Random)} should be set for
	 * this extender.
	 * 
	 * @param otExtender
	 */
	public void setParameters(OTExtender otExtender) {
		this.otExtender = otExtender;
	}

	
	public void execProtocol(BigInteger[][] msgPairs) throws IOException {
		
		
    	BitMatrix x0 = new BitMatrix(msgBitLength, numOfPairs);
    	BitMatrix x1 = new BitMatrix(msgBitLength, numOfPairs);
    	
    	for (int i = 0; i < numOfPairs; ++i) {
            BitMatrix val0 = BitMatrix.valueOf(msgPairs[i][0], msgBitLength);
            BitMatrix val1 = BitMatrix.valueOf(msgPairs[i][1], msgBitLength);
            x0.copyRow(i, val0, 0);
            x1.copyRow(i, val1, 0);

//    		writeBigIntegerToRow(msgPairs[i][0], x0, i);
//    		writeBigIntegerToRow(msgPairs[i][1], x1, i);
    		logger.trace("Sending input labels for wire {}: 0x{}, 0x{}", i, 
    				msgPairs[i][0].toString(16), msgPairs[i][1].toString(16));
    	}
    	
		otExtender.send(x0, x1);
	}
}
