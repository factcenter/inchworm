package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.inchworm.SingleChooseCircuit.Result;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;

import java.math.BigInteger;
import java.util.Random;


/**
 * Implementation of N bit bitwise and operator client / server circuit designed
 * to be used inside an Inchworm VM. This class can be used instead of the
 * separate AndOpClient / AndOpServer op classes.
 */
public class SingleChooseInchworm extends InchwormOpCommon {

	private int entryLenBits;
	private int prefixLenBits;
	private SingleChooseCircuit myCircuit;

	/**
	 * Constructor for using the op circuit from an Inchworm secure computation
	 * session, using Inchworm OT-Extension and channel object input / output
	 * streams.
	 * 
	 * @param serverOp
	 *            - true if server, false otherwise.
	 * @param rand
	 *            - a secure random number generator.
	 * @param labelBitLength
	 *            - Length in bits of a single label.
	 * @param toPeer
	 *            - Communication channel.
	 * @param otExtender
	 *            - OT Extender.
	 * 
	 */
	public SingleChooseInchworm(boolean serverOp, Random rand,
			int labelBitLength, int entryLenBits,
			int prefixLenBits, Channel toPeer,
			OTExtender otExtender) {
		super(rand, labelBitLength);

		this.entryLenBits = entryLenBits;
		this.prefixLenBits = prefixLenBits;
		this.serverMode = serverOp;
		
		otNumOfPairs = SingleChooseCircuit.calcInDegree(entryLenBits, prefixLenBits) / 2;
		setParameters(toPeer, otExtender);

		try {

			// Instantiate and build the bitwise and circuit.
			ccs = new Circuit[1];
			myCircuit = new SingleChooseCircuit(globals, entryLenBits,
					prefixLenBits);
		
			ccs[0] = myCircuit;

			if (serverMode)
				generateLabelPairs();

			super.init();
			createCircuits(serverMode);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * See myCircuit pack inputs
	 *
	 */
	public void setData(BigInteger entry, 
			BigInteger entryNodePrefix, 
			BigInteger carry, 
			BigInteger pathPrefix,
			boolean validBit) {
		BigInteger c = myCircuit.packInputs(entry, entryNodePrefix, carry, pathPrefix, validBit ? BigInteger.ONE : BigInteger.ZERO);

		choices = c;
	}
	
	/**
	 * See myCircuit.parseResult
	 */
	public Result parseResult(BigInteger resultBits) {
		return myCircuit.parseResult(resultBits);
	}
}
