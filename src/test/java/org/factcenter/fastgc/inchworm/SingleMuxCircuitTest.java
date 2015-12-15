package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.inchworm.SingleMuxCircuit.Result;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import test.categories.KnownBad;

import java.math.BigInteger;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;


// Currently not working after refactoring
@Category(KnownBad.class)
public class SingleMuxCircuitTest extends GenericOpTest {

	/*
	 * Data for testing the op.
	 */
	final static int indexWidth = 8;

	final static int dataWidth = 16;

	final static BigInteger INDEX_MASK = BigInteger.ONE.shiftLeft(indexWidth)
			.subtract(BigInteger.ONE);
	final static BigInteger DATA_MASK = BigInteger.ONE.shiftLeft(dataWidth)
			.subtract(BigInteger.ONE);

	SingleMuxCircuit myCircuit;
	
	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		myCircuit = new SingleMuxCircuit(globals, indexWidth, dataWidth);
		return myCircuit;
	}

	@Override
	int getNumberOfInputs() {
		//return indexWidth * 2 + dataWidth * 2 + 1;
		return SingleMuxCircuit.calcInDegree(dataWidth, indexWidth)/2;
	}

	/*-
	 * ----------------------------------------------------------------
	 *                      Data Handlers 
	 * ----------------------------------------------------------------
	 */

	@Test
	public void testPositive() throws Exception {
		BigInteger idxLeft = BigInteger.valueOf(0xaa); // idx == idxTag
		BigInteger idxRight = BigInteger.valueOf(0xbb);
		
		BigInteger indexLeftTag = BigInteger.valueOf(0xbb);
		BigInteger indexRightTag = BigInteger.valueOf(0xaa);

		BigInteger dataLeft = BigInteger.valueOf(0x00ff);
		BigInteger dataRight = BigInteger.valueOf(0xff00);

		BigInteger rShareLeft = BigInteger.ZERO;//BigInteger.valueOf(0x1234);
		BigInteger rShareRight = BigInteger.ZERO;//BigInteger.valueOf(0x9874);

		BigInteger validLeft = BigInteger.ONE;
		BigInteger validRight = BigInteger.ZERO;
		
		BigInteger x = myCircuit.packData(idxLeft, indexLeftTag, dataLeft, validLeft, rShareLeft);
		BigInteger y = myCircuit.packData(idxRight, indexRightTag, dataRight, validRight, rShareRight);

		Result expected = computeResult(idxLeft, idxRight,
				indexLeftTag, indexRightTag, 
				dataLeft, dataRight,
				validLeft, validRight,
				rShareLeft, rShareRight);
		
		Future<BigInteger> clientThread = runClient(x);
		BigInteger resultBits = runServer(y);
		logger.debug("resultBits = " + resultBits.toString(16));
		clientThread.get();
		Result actual = myCircuit.parseResult(resultBits);
		logger.debug("expected = " + expected.toString());
		logger.debug("actual   = " + actual.toString());
		assertEquals(expected.data.toString(16), actual.data.toString(16));
		assertEquals(expected.validBit, actual.validBit);
	}

	@Test
	public void testIndexOffsetBug() throws  Exception {
		BigInteger idxLeft = BigInteger.valueOf(0xc3); 
		BigInteger idxRight = BigInteger.valueOf(0x0);
		
		BigInteger indexLeftTag = BigInteger.valueOf(0xc3);
		BigInteger indexRightTag = BigInteger.valueOf(0x0);

		BigInteger dataLeft = BigInteger.valueOf(0x03bd);
		BigInteger dataRight = BigInteger.valueOf(0x0000);

		BigInteger rShareLeft = BigInteger.valueOf(0x0000);
		BigInteger rShareRight = BigInteger.valueOf(0x0000);

		BigInteger validLeft = BigInteger.ZERO;
		BigInteger validRight = BigInteger.ZERO;
		
		BigInteger x = myCircuit.packData(idxLeft, indexLeftTag, dataLeft, validLeft, rShareLeft);
		BigInteger y = myCircuit.packData(idxRight, indexRightTag, dataRight, validRight, rShareRight);

		Result expected = computeResult(idxLeft, idxRight,
				indexLeftTag, indexRightTag, 
				dataLeft, dataRight,
				validLeft, validRight,
				rShareLeft, rShareRight);
		
		Future<BigInteger> clientThread = runClient(x);
		BigInteger resultBits = runServer(y);
		logger.debug("resultBits = " + resultBits.toString(16));
		clientThread.get();
		Result actual = myCircuit.parseResult(resultBits);
		logger.debug("expected = " + expected.toString());
		logger.debug("actual   = " + actual.toString());
		assertEquals(expected.data.toString(16), actual.data.toString(16));
		assertEquals(expected.validBit, actual.validBit);
	}
	
	@Test
	public void testNegative() throws Exception {
		BigInteger idxLeft = BigInteger.valueOf(0xaf); // idx!=idxTag
		BigInteger idxRight = BigInteger.valueOf(0xbb);
		BigInteger indexLeftTag = BigInteger.valueOf(0xbb);
		BigInteger indexRightTag = BigInteger.valueOf(0xaa);

		BigInteger dataLeft = BigInteger.valueOf(0x00ff);
		BigInteger dataRight = BigInteger.valueOf(0xff00);

		BigInteger rShareLeft = BigInteger.valueOf(0x0000);
		BigInteger rShareRight = BigInteger.valueOf(0x0000);

		BigInteger validLeft = BigInteger.ONE;
		BigInteger validRight = BigInteger.ZERO;
		
		BigInteger x = myCircuit.packData(idxLeft, indexLeftTag, dataLeft, validLeft, rShareLeft);
		BigInteger y = myCircuit.packData(idxRight, indexRightTag, dataRight, validRight, rShareRight);

		Result expected = computeResult(idxLeft, idxRight,
				indexLeftTag, indexRightTag, 
				dataLeft, dataRight,
				validLeft, validRight,
				rShareLeft, rShareRight);
		
		Future<BigInteger> clientThread = runClient(x);
		BigInteger resultBits = runServer(y);
		logger.debug("resultBits = " + resultBits.toString(16));
		clientThread.get();
		Result actual = myCircuit.parseResult(resultBits);
		logger.debug("expected = " + expected.toString());
		logger.debug("actual   = " + actual.toString());
		assertEquals(expected.data.toString(16), actual.data.toString(16));
		assertEquals(expected.validBit, actual.validBit);
	}

	@Test
	public void multiTest() throws Exception {
		for (int i = 0; i < 10000; ++i) {
			BigInteger idxLeft = new BigInteger(indexWidth, rand);
			BigInteger idxRight = new BigInteger(indexWidth, rand);

			BigInteger indexLeftTag = new BigInteger(indexWidth, rand);
			BigInteger indexRightTag = new BigInteger(indexWidth, rand);

			BigInteger dataLeft = new BigInteger(dataWidth, rand);
			BigInteger dataRight = new BigInteger(dataWidth, rand);

			BigInteger rShareLeft = new BigInteger(myCircuit.getRandBitsLength(), rand);
			BigInteger rShareRight = new BigInteger(myCircuit.getRandBitsLength(), rand);

			BigInteger validLeft = new BigInteger(1, rand);
			BigInteger validRight = new BigInteger(1, rand);
			
			BigInteger x = myCircuit.packData(idxLeft, indexLeftTag, dataLeft, validLeft, rShareLeft);
			BigInteger y = myCircuit.packData(idxRight, indexRightTag, dataRight, validRight, rShareRight);

			Future<BigInteger> clientThread = runClient(x);
			BigInteger resultBits = runServer(y);
			clientThread.get();
			Result expected = computeResult(idxLeft, idxRight,
					indexLeftTag, indexRightTag, 
					dataLeft, dataRight,
					validLeft, validRight,
					rShareLeft, rShareRight);
			
			Result actual = myCircuit.parseResult(resultBits);
			logger.debug("expected = " + expected.toString());
			logger.debug("actual = " + actual.toString());
			assertEquals(expected.data.toString(16), actual.data.toString(16));
			assertEquals(expected.validBit, actual.validBit);
			
		}
	}

	private Result computeResult(BigInteger idxLeft, BigInteger idxRight,
			BigInteger indexLeftTag, BigInteger indexRightTag,
			BigInteger dataLeft, BigInteger dataRight,
			BigInteger validLeft, BigInteger validRight,
			BigInteger rShareLeft, BigInteger rShareRight) {
		BigInteger index = idxLeft.xor(idxRight);
		BigInteger indexTag = indexLeftTag.xor(indexRightTag);
		BigInteger valid = validLeft.xor(validRight);
		BigInteger data = dataLeft.xor(dataRight);
		BigInteger rBits= rShareLeft.xor(rShareRight).and(DATA_MASK);
		boolean validBitsRand = (rShareLeft.shiftRight(dataWidth).longValue() ^ rShareRight.shiftRight(dataWidth).longValue()) == 1;
		
		logger.debug("index ={} indexTag={} valid ={} data ={} rBits={}", index.toString(16), indexTag.toString(16), valid, data.toString(16), rBits.toString(16));
		logger.debug("LEFT: index ={} indexTag={} valid ={} data ={} rBits={}", idxLeft.toString(16), indexLeftTag.toString(16), validLeft, dataLeft.toString(16), rShareLeft.toString(16));
		logger.debug("RIGHT: index ={} indexTag={} valid ={} data ={} rBits={}", idxRight.toString(16), indexRightTag.toString(16), validRight, dataRight.toString(16), rShareRight.toString(16));
		
		if (!index.equals(indexTag) || valid.equals(BigInteger.ZERO)) {
			//return rBits;
			return new Result(rBits, false ^ validBitsRand);
		}

		//return rBits.xor(data);
		return new Result(rBits.xor(data).and(DATA_MASK), true ^ validBitsRand);
	}

}
