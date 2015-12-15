package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

public class AndOpTest extends GenericOpTest{

	/*
	 * Data for testing the op.
	 */
	final static int bitWidth = 8;
	
	final static BigInteger MASK = BigInteger.ONE.shiftLeft(bitWidth).subtract(BigInteger.ONE);
	
	final static BigInteger aShareLeft = BigInteger.valueOf(0xfe);
	final static BigInteger aShareRight = BigInteger.valueOf(0);
	
	final static BigInteger bShareLeft = BigInteger.valueOf(0x2);
	final static BigInteger bShareRight = BigInteger.valueOf(0);
	
	final static BigInteger rShareLeft = BigInteger.valueOf(0);
	final static BigInteger rShareRight = BigInteger.valueOf(0);
	
	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		return new AndOpCircuit(globals, bitWidth);
	}
	
	@Override
	int getNumberOfInputs() {
		return bitWidth * 3;
	}


	/*-
	 * ----------------------------------------------------------------
	 *                      Data Handlers 
	 * ----------------------------------------------------------------
	 */
	
	/**
	 * Combine data from one side into inputs for circuit
	 * @param aShare
	 * @param bShare
	 * @param rShare
	 */
	BigInteger getData(BigInteger aShare, BigInteger bShare, BigInteger rShare) {
		return rShare.shiftLeft(bitWidth).or(bShare).shiftLeft(bitWidth).or(aShare);
	}
	
	BigInteger getData(long aShare, long bShare, long rShare) {
		return getData(BigInteger.valueOf(aShare), BigInteger.valueOf(bShare), BigInteger.valueOf(rShare));
	}
	
//	static BigInteger bitReverse(BigInteger x, int len) {
//		BigInteger xrev = BigInteger.ZERO;
//		
//		for (int i = 0; i < len; ++i) {
//			xrev = xrev.shiftLeft(1).or(x.testBit(len - i) ? BigInteger.ONE : BigInteger.ZERO);
//		}
//		return xrev;
//	}
	
	BigInteger compute(BigInteger aShare0, BigInteger bShare0, BigInteger rShare0,
			BigInteger aShare1, BigInteger bShare1, BigInteger rShare1) {
		BigInteger a = aShare0.xor(aShare1);
		BigInteger b = bShare0.xor(bShare1);
		BigInteger r = rShare0.xor(rShare1);
		
		return (a.and(b).xor(r)).and(MASK);
	}
	
	BigInteger compute(BigInteger x, BigInteger y) {
		BigInteger r0 = x.shiftRight(bitWidth * 2);
		BigInteger b0 = x.shiftRight(bitWidth).and(MASK);
		BigInteger a0 = x.and(MASK);
		BigInteger r1 = y.shiftRight(bitWidth * 2);
		BigInteger b1 = y.shiftRight(bitWidth).and(MASK);
		BigInteger a1 = y.and(MASK);
		
		return compute(a0, b0, r0, a1, b1, r1);
	}
		

	@Test
	public void test() throws IOException {
		BigInteger x = getData(aShareLeft, bShareLeft, rShareLeft);
		BigInteger y = getData(aShareRight, bShareRight, rShareRight);
						
		runClient(x);
		BigInteger result = runServer(y);
		logger.debug("result=0x{}", result.toString(16));
		BigInteger expected = compute(x, y);
		assertEquals("x=0x"+x.toString(16)+",y="+y.toString(16), expected, result);
	}
	
	@Test
	public void multiTest() throws Exception {
		for (int i = 0; i < 20; ++i) {
			BigInteger x = new BigInteger(bitWidth * 3, rand);
			BigInteger y = new BigInteger(bitWidth * 3, rand);
						
			Future<BigInteger> clientThread = runClient(x);
			BigInteger result = runServer(y);
			BigInteger expected = compute(x, y);
			assertEquals("x=0x"+x.toString(16)+",y="+y.toString(16), expected, result);
			clientThread.get();
		}
	}	

}
