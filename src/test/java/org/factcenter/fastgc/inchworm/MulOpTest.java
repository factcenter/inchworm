package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class MulOpTest extends GenericOpTest {

	/*
	 * Data for testing the op.
	 */
	final static int bitWidth = 4;
	
	final static BigInteger MASK = BigInteger.ONE.shiftLeft(bitWidth).subtract(BigInteger.ONE);
	final static BigInteger ComputeMASK = BigInteger.ONE.shiftLeft(2 * bitWidth).subtract(BigInteger.ONE);
	
	final static BigInteger aShareLeft = BigInteger.valueOf(0x08);
	final static BigInteger aShareRight = BigInteger.valueOf(0);
	
	final static BigInteger bShareLeft = BigInteger.valueOf(0x03);
	final static BigInteger bShareRight = BigInteger.valueOf(0);
	
	final static BigInteger rShareLeft = BigInteger.valueOf(0);
	final static BigInteger rShareRight = BigInteger.valueOf(0);
	
	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
        return new UnshareOpReshareCircuit(globals, new MUL_K(globals, bitWidth));
	}
	
	@Override
	int getNumberOfInputs() {
		return bitWidth * 4;
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
	
	BigInteger compute(BigInteger aShare0, BigInteger bShare0, BigInteger rShare0,
			BigInteger aShare1, BigInteger bShare1, BigInteger rShare1) {
		BigInteger a = aShare0.xor(aShare1);
		BigInteger b = bShare0.xor(bShare1);
		BigInteger r = rShare0.xor(rShare1);
		
		return (a.multiply(b).xor(r)).and(ComputeMASK);
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
		
		// ============================================
		
		BigInteger bShareLeftNew = BigInteger.valueOf(0x06);
		x = getData(aShareLeft, bShareLeftNew, rShareLeft);
		y = getData(aShareRight, bShareRight, rShareRight);
		
		runClient(x);
		result = runServer(y);
		logger.debug("result=0x{}", result.toString(16));
		expected = compute(x, y);
		assertEquals("x=0x"+x.toString(16)+",y="+y.toString(16), expected, result);
		
	}

}
