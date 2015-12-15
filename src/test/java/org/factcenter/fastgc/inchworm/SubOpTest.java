package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.inchworm.ops.dummy.DummySub;
import org.factcenter.inchworm.ops.dummy.ProtocolInfo;
import org.junit.Test;

import java.math.BigInteger;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;


public class SubOpTest extends GenericOpTest {

	/*
	 * Data for testing the op.
	 */
	final static int bitWidth = 2;
	
	final static BigInteger MASK = BigInteger.ONE.shiftLeft(bitWidth).subtract(BigInteger.ONE);
	
	final static BigInteger aShareLeft = BigInteger.valueOf(0xffffffff);
	final static BigInteger aShareRight = BigInteger.valueOf(0);
	
	final static BigInteger bShareLeft = BigInteger.valueOf(0x5000);
	final static BigInteger bShareRight = BigInteger.valueOf(0);
	
	final static BigInteger rShareLeft = BigInteger.valueOf(0);
	final static BigInteger rShareRight = BigInteger.valueOf(0);
	
	
	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		return new SubOpCircuit(globals, bitWidth);
	}
	
	@Override
	int getNumberOfInputs() {
		return bitWidth * 3 + 4;
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
	
	static BigInteger bitReverse(BigInteger x, int len) {
		BigInteger xrev = BigInteger.ZERO;
		
		for (int i = 0; i < len; ++i) {
			xrev = xrev.shiftLeft(1).or(x.testBit(len - i) ? BigInteger.ONE : BigInteger.ZERO);
		}
		return xrev;
	}
	
	BigInteger compute(BigInteger aShare0, BigInteger bShare0, BigInteger rShare0,
			BigInteger aShare1, BigInteger bShare1, BigInteger rShare1) {
		BigInteger a = aShare0.xor(aShare1);
		BigInteger b = bShare0.xor(bShare1);
		BigInteger r = rShare0.xor(rShare1);
		
		return (a.add(b).xor(r)).and(MASK);
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
	public void truthTable() throws Exception {
		DummySub dsub = new DummySub(new ProtocolInfo());
		BigInteger input = BigInteger.ZERO;
		do {
			BigInteger a = input.and(BigInteger.valueOf(((1 & 0xffffffff) << bitWidth) - 1));
			BigInteger b = input.shiftRight(bitWidth);
			input = input.add(BigInteger.ONE);
			BigInteger expectedFlags = dsub.doArithmetic(bitWidth, new BigInteger[]{a, b})[5];
			BigInteger expectedResult = dsub.doArithmetic(bitWidth, new BigInteger[]{a, b})[0];
			BigInteger x = getData(a, b, rShareLeft);
			BigInteger y = getData(BigInteger.ZERO, BigInteger.ZERO, rShareRight);
			Future<BigInteger> clientThread = runClient(x);
			BigInteger result = runServer(y);
			BigInteger flags = result.shiftRight(bitWidth);
			System.out.println(String.format("a:%4s", a.toString(2)));
			System.out.println(String.format("b:%4s", b.toString(2)));
			System.out.println(String.format("r:%4s", result.and(MASK).toString(2)));
			System.out.println(String.format("expected r:%4s", expectedResult.toString(2)));
			System.out.println(String.format("flags:         %4s", flags.toString(2)));
			System.out.println(String.format("expected flags:%4s", expectedFlags.toString(2)));
			assertEquals(expectedFlags, flags);
			assertEquals(expectedResult, result.and(MASK));
		} while(input.compareTo(BigInteger.valueOf((1 & 0xffffffff) << (2*bitWidth))) < 0);
	}
}

