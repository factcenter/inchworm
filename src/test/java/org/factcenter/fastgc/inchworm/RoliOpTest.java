package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class RoliOpTest extends GenericOpTest {

	final static int bitWidth = 10;

	final static BigInteger MASK = BigInteger.ONE.shiftLeft(bitWidth).subtract(
			BigInteger.ONE);

	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		return new RoliOpCircuit(globals, bitWidth);
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
	 * 
	 * @param aShare
	 * @param bShare
	 * @param rShare
	 * @return
	 */
	BigInteger getData(BigInteger aShare, BigInteger bShare, BigInteger rShare) {
		return rShare.shiftLeft(bitWidth).or(bShare).shiftLeft(bitWidth)
				.or(aShare);
	}

	BigInteger getData(long aShare, long bShare, long rShare) {
		return getData(BigInteger.valueOf(aShare), BigInteger.valueOf(bShare),
				BigInteger.valueOf(rShare));
	}

	BigInteger compute(BigInteger aShare0, BigInteger bShare0,
			BigInteger rShare0, BigInteger aShare1, BigInteger bShare1,
			BigInteger rShare1) {
		BigInteger a = aShare0.xor(aShare1);
		BigInteger b = bShare0.xor(bShare1);
		BigInteger r = rShare0.xor(rShare1);

		return (a.shiftLeft(b.intValue())).xor(r).and(MASK);
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
		
		/*-
		 * Data for testing the ROL op.
		 * 
		 * Server aShare, bShare       = 43, 1   (6)  (7)  (5)
		 * Client aShare, bShare       = 2,  2 
		 * xored value(value, shift)   = 41, 3   (4)  (5)  (7)
		 *                               101001
		 *                                   101001000 = 328
		 *                                        1010010000 = 656
		 * Expected:                                   0100100001 = 289
		 *                                                  0010000101 = 133
		 *                                        
		 */
		
		// Test #1.
		BigInteger aShareLeft = BigInteger.valueOf(43);
		BigInteger aShareRight = BigInteger.valueOf(2);
		BigInteger bShareLeft = BigInteger.valueOf(1);
		BigInteger bShareRight = BigInteger.valueOf(2);
		BigInteger rShareLeft = BigInteger.valueOf(0);
        BigInteger rShareRight = BigInteger.valueOf(0);

		BigInteger x = getData(aShareLeft, bShareLeft, rShareLeft);
		BigInteger y = getData(aShareRight, bShareRight, rShareRight);

		logger.debug("client(x)=0x{}", x.toString(16));
		logger.debug("server(y)=0x{}", y.toString(16));
		runClient(x);
		BigInteger result = runServer(y);
		logger.debug("result=0x{}", result.toString(16));
		BigInteger expected = compute(x, y);
		assertEquals("(x=0x" + x.toString(16) + " ^ y=0x" + y.toString(16)
				+ ") ROL 3 ", expected, result);
		
		// Test #2.
		bShareLeft = BigInteger.valueOf(6);
		x = getData(aShareLeft, bShareLeft, rShareLeft);
		y = getData(aShareRight, bShareRight, rShareRight);

		logger.debug("client(x)=0x{}", x.toString(16));
		logger.debug("server(y)=0x{}", y.toString(16));
		runClient(x);
		result = runServer(y);
		logger.debug("result=0x{}", result.toString(16));
		expected = compute(x, y);
		assertEquals("(x=0x" + x.toString(16) + " ^ y=0x" + y.toString(16)
				+ ") ROL 3 ", expected, result);		
		
		// Test #3.
		bShareLeft = BigInteger.valueOf(7);
		x = getData(aShareLeft, bShareLeft, rShareLeft);
		y = getData(aShareRight, bShareRight, rShareRight);

		logger.debug("client(x)=0x{}", x.toString(16));
		logger.debug("server(y)=0x{}", y.toString(16));
		runClient(x);
		result = runServer(y);
		logger.debug("result=0x{}", result.toString(16));
		expected = BigInteger.valueOf(289);
		assertEquals("(x=0x" + x.toString(16) + " ^ y=0x" + y.toString(16)
				+ ") ROL 3 ", expected, result);	
		
		// Test #4.
		bShareLeft = BigInteger.valueOf(5);
		x = getData(aShareLeft, bShareLeft, rShareLeft);
		y = getData(aShareRight, bShareRight, rShareRight);

		logger.debug("client(x)=0x{}", x.toString(16));
		logger.debug("server(y)=0x{}", y.toString(16));
		runClient(x);
		result = runServer(y);
		logger.debug("result=0x{}", result.toString(16));
		expected = BigInteger.valueOf(133);
		assertEquals("(x=0x" + x.toString(16) + " ^ y=0x" + y.toString(16)
				+ ") ROL 3 ", expected, result);	
		
	}

}
