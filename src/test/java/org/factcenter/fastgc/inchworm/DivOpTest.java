package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class DivOpTest extends GenericOpTest {

	/*
	 * Data for testing the op.
	 */
	final static int bitWidth = 4;
	
	BigInteger aShareLeft = BigInteger.valueOf(0x08);
	final static BigInteger aShareRight = BigInteger.valueOf(0);
	
	BigInteger bShareLeft = BigInteger.valueOf(0x02);
	final static BigInteger bShareRight = BigInteger.valueOf(0);
	
	final static BigInteger rShareLeft = BigInteger.valueOf(0);
	final static BigInteger rShareRight = BigInteger.valueOf(0);
	
	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		return new UnshareOpReshareCircuit(globals, new DIV_K_ZERO_DETECT(globals, bitWidth));
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
	
	
	@Test
	public void test() throws IOException {
		
		// No remainder (8 / 2).
		BigInteger x = getData(aShareLeft, bShareLeft, rShareLeft);
		BigInteger y = getData(aShareRight, bShareRight, rShareRight);
						
		runClient(x);
		BigInteger result = runServer(y);
		logger.debug("result=0x{}", result.toString(16));
		BigInteger expected = BigInteger.valueOf(4);
		assertEquals("x=0x"+x.toString(16)+",y="+y.toString(16), expected, result);
		
		// Test case of divisor = 0;
		bShareLeft = BigInteger.valueOf(0);
		x = getData(aShareLeft, bShareLeft, rShareLeft);
		y = getData(aShareRight, bShareRight, rShareRight);
						
		runClient(x);
		result = runServer(y);
		logger.debug("result=0x{}", result.toString(16));
		assertEquals("x=0x"+x.toString(16)+",y="+y.toString(16), BigInteger.ZERO, result);
		
		// No remainder (8 / 4).
		bShareLeft = BigInteger.valueOf(0x04);
		x = getData(aShareLeft, bShareLeft, rShareLeft);
		y = getData(aShareRight, bShareRight, rShareRight);
						
		runClient(x);
		result = runServer(y);
		logger.debug("result=0x{}", result.toString(16));
		expected = BigInteger.valueOf(2);
		assertEquals("x=0x"+x.toString(16)+",y="+y.toString(16), expected, result);
		
		// Output bits (MS nibble - LS nibble): Remainder, Quotient.
		// Remainder (5 / 3).
		aShareLeft = BigInteger.valueOf(5);
		bShareLeft = BigInteger.valueOf(3);
		x = getData(aShareLeft, bShareLeft, rShareLeft);
		y = getData(aShareRight, bShareRight, rShareRight);
						
		runClient(x);
		result = runServer(y);
		logger.debug("result=0x{}", result.toString(16));
		expected = BigInteger.valueOf(0x21);
		assertEquals("x=0x"+x.toString(16)+",y="+y.toString(16), expected, result);
		
		
		aShareLeft = BigInteger.valueOf(10);
		bShareLeft = BigInteger.valueOf(3);
		x = getData(aShareLeft, bShareLeft, rShareLeft);
		y = getData(aShareRight, bShareRight, rShareRight);
						
		runClient(x);
		result = runServer(y);
		logger.debug("result=0x{}", result.toString(16));
		expected = BigInteger.valueOf(0x13);
		assertEquals("x=0x"+x.toString(16)+",y="+y.toString(16), expected, result);		
		
		
		aShareLeft = BigInteger.valueOf(13);
		bShareLeft = BigInteger.valueOf(5);
		x = getData(aShareLeft, bShareLeft, rShareLeft);
		y = getData(aShareRight, bShareRight, rShareRight);
						
		runClient(x);
		result = runServer(y);
		logger.debug("result=0x{}", result.toString(16));
		expected = BigInteger.valueOf(0x32);
		assertEquals("x=0x"+x.toString(16)+",y="+y.toString(16), expected, result);	
		
	}
	
}
