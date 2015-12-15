package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.inchworm.Converters;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.util.BitMatrix;

import java.math.BigInteger;
import java.util.Random;

/**
 * Implementation of N bit bitwise and operator client / server circuit designed
 * to be used inside an Inchworm VM. This class can be used instead of the
 * separate AndOpClient / AndOpServer op classes.
 */
public class SingleWriteStashInchworm extends InchwormOpCommon {

	private int indexWidth;
	private int dataWidth;
	private SingleWriteStashCircuit myCircuit;

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
	 * @param indexWidth
	 *            -index width in bits
	 * @param dataWidth
	 *            - data width in bits
	 * 
	 */
	public SingleWriteStashInchworm(boolean serverOp, Random rand,
			int labelBitLength, int indexWidth, int dataWidth, Channel toPeer,
			OTExtender otExtender) {
		super(rand, labelBitLength);

		this.indexWidth = indexWidth;
		this.dataWidth = dataWidth;
		this.serverMode = serverOp;

		otNumOfPairs = SingleWriteStashCircuit.calcInDegree(indexWidth,
				dataWidth) / 2;
		setParameters(toPeer, otExtender);

		try {

			// Instantiate and build the bitwise and circuit.
			ccs = new Circuit[1];
			myCircuit = new SingleWriteStashCircuit(globals, indexWidth,
					dataWidth);

			ccs[0] = myCircuit;

			if (serverMode)
				generateLabelPairs();

			super.init();
			createCircuits(serverMode);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setData(long indexOld, long indexNew, BitMatrix dataOld,
			BitMatrix dataNew, boolean carry, boolean validBit, boolean blockValidBit, BigInteger random) {

		if ((dataWidth != dataOld.getNumCols())
				|| (dataWidth != dataNew.getNumCols())) {
			throw new IllegalArgumentException(
					"dataOld and dataNew must be dataWidth length = " + dataWidth
							+ " dataOld len:" + dataOld.getNumCols() + " dataNew len: "
							+ dataNew.getNumCols());
		}
		if (indexOld > (1 << indexWidth) || (indexNew > (1 << indexWidth))) {
			throw new IllegalArgumentException("Index too big old : "
					+ indexOld + " new: " + indexNew + " max: "
					+ (1 << indexWidth));
		}

		BigInteger _indexOld = BigInteger.valueOf(indexOld);
		BigInteger _indexNew = BigInteger.valueOf(indexNew);
		BigInteger _dataOld = Converters.toBigInteger(dataOld);
		BigInteger _dataNew = Converters.toBigInteger(dataNew);
		BigInteger _carry = carry ? BigInteger.ONE : BigInteger.ZERO;
		BigInteger _valid = validBit ? BigInteger.ONE : BigInteger.ZERO;
		BigInteger _inputBlockValidBit = blockValidBit ? BigInteger.ONE : BigInteger.ZERO;

		BigInteger c = myCircuit.packInputs(_indexOld, _indexNew, _dataOld,
				_dataNew, _carry, _valid, _inputBlockValidBit, random);

//		logger.debug(
//				"SingleWriteStash packed input {}. IndexOld={} IndexNew={} dataOld={} dataNew={} carry={} valid={}",
//				c.toString(16), _indexOld.toString(16), _indexNew.toString(16), _dataOld.toString(16), _dataNew.toString(16),
//				_carry, _valid);

		choices = c;
	}

	public SingleWriteStashCircuit.Result parseResult(BigInteger resultBits) {
		return myCircuit.parseResult(resultBits);
	}
}
