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
public class SingleMuxInchworm extends InchwormOpCommon {

	private int indexWidth;
	private int dataWidth;

	private SingleMuxCircuit singleMuxCircuit;
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
	public SingleMuxInchworm(boolean serverOp, Random rand, int labelBitLength,
			int indexWidth, int dataWidth, Channel toPeer, OTExtender otExtender) {
		super(rand, labelBitLength);

		this.indexWidth = indexWidth;
		this.dataWidth = dataWidth;
		this.serverMode = serverOp;
		otNumOfPairs = SingleMuxCircuit.calcInDegree(dataWidth, indexWidth) / 2;
		setParameters(toPeer, otExtender);

		try {

			// Instantiate and build the bitwise and circuit.
			ccs = new Circuit[1];
			singleMuxCircuit = new SingleMuxCircuit(globals, indexWidth, dataWidth); 
			ccs[0] = singleMuxCircuit;

			if (serverMode)
				generateLabelPairs();

			super.init();
			createCircuits(serverMode);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setData(long index, long indexTag, BitMatrix data, boolean valid, BigInteger randValue) {
		if ((dataWidth != data.getNumCols())){
				//|| (dataWidth != randValue.bitLength())){
			throw new IllegalArgumentException(
					"data and rand must be dataWidth length = " + dataWidth
							+ " data len:" + data.getNumCols() //+ " rand len: "+ randValue.length
							);
		}
		if (index > (1 << indexWidth)) {
			throw new IllegalArgumentException("Index too big : " + index
					+ " max: " + (1 << indexWidth));
		}

		BigInteger validBit = valid == true ? BigInteger.ONE : BigInteger.ZERO;
		
		BigInteger packed = singleMuxCircuit.packData(BigInteger.valueOf(index), BigInteger.valueOf(indexTag), Converters.toBigInteger(data), validBit, randValue);
		
		choices = packed;
	}
	
	public int getRandBitsLength(){
		return singleMuxCircuit.getRandBitsLength();
	}
	
	public SingleMuxCircuit.Result parseResult(BigInteger resultBits){
		return singleMuxCircuit.parseResult(resultBits);
	}
}
