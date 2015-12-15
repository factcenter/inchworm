package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;

import java.math.BigInteger;
import java.util.Random;



/**
 * Implementation of N bit bitwise and operator client / server circuit designed
 * to be used inside an Inchworm VM. This class can be used instead of the
 * separate AndOpClient / AndOpServer op classes.
 */
public class PopStashEntrySharedInchworm extends InchwormOpCommon {

	private int entryLenBits;
	private int prefixLenBits;
	private PopStashEntrySharedCircuit myCircuit;

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
	public PopStashEntrySharedInchworm(boolean serverOp, Random rand,
			int labelBitLength, int entryLenBits,
			int prefixLenBits, int stashCapacity, Channel toPeer,
			OTExtender otExtender) {
		super(rand, labelBitLength);

		this.entryLenBits = entryLenBits;
		this.prefixLenBits = prefixLenBits;
		this.serverMode = serverOp;
		
		otNumOfPairs = PopStashEntrySharedCircuit.calcInputBits(entryLenBits, prefixLenBits, stashCapacity)/2;
		setParameters(toPeer, otExtender);

		try {

			// Instantiate and build the bitwise and circuit.
			ccs = new Circuit[1];
			myCircuit = new PopStashEntrySharedCircuit(globals, entryLenBits,
					prefixLenBits,stashCapacity);
		
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
	public void setData(BigInteger pathPrefix, 
			BigInteger[] stashEntries, 
			BigInteger[] entryPrefixes, 
			boolean[] validBits, 
			BigInteger randValidBits,
			BigInteger randEntryBits, 
			BigInteger randEntryValidBit){ 
		BigInteger c = myCircuit.packInput(pathPrefix, stashEntries, entryPrefixes, validBits, randValidBits, randEntryBits, randEntryValidBit);
		choices = c;
	}
	
	/**
	 * See myCircuit.parseResult
	 */
	public PopStashEntryCircuit.Result parseResult(BigInteger resultBits) {
		return myCircuit.parseResult(resultBits);
	}
}
