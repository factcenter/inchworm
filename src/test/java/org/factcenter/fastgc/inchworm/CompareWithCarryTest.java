package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

public class CompareWithCarryTest extends GenericOpTest {

	final static int dataWidth = 16;

	final static BigInteger DATA_MASK = BigInteger.ONE.shiftLeft(dataWidth)
			.subtract(BigInteger.ONE);

	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		return new CompareWithCarryCircuit(globals, dataWidth);
	}

	@Override
	int getNumberOfInputs() {
		return dataWidth + 1;
	}

	/*-
	 * ----------------------------------------------------------------
	 *                      Data Handlers 
	 * ----------------------------------------------------------------
	 */

	BigInteger getData(BigInteger input, BigInteger carry) {
		return carry.
				shiftLeft(dataWidth).or(input);
	}

	@Test
	public void testPositive() throws IOException {
		BigInteger inputA = BigInteger.valueOf(0x1111);
		BigInteger carryA = BigInteger.ZERO;

		BigInteger inputB = BigInteger.valueOf(0x1111);
		BigInteger carryB = BigInteger.ZERO;
		
		BigInteger x = getData(inputA, carryA);
		BigInteger y = getData(inputB, carryB);

		runClient(x);
		BigInteger result = runServer(y);
		logger.debug("result=0x{}", result.toString(16));
		BigInteger expected = BigInteger.valueOf(3);//0b11
		assertEquals("x=0x" + x.toString(16) + ",y=" + y.toString(16),
				expected, result);
	}

	@Test
	public void testNegative() throws IOException {
		BigInteger inputA = BigInteger.valueOf(0x54c6);
		BigInteger carryA = BigInteger.ONE;

		BigInteger inputB = BigInteger.valueOf(0x1674);
		BigInteger carryB = BigInteger.ONE;
		
		BigInteger x = getData(inputA, carryA);
		BigInteger y = getData(inputB, carryB);

		runClient(x);
		BigInteger result = runServer(y);
		logger.debug("result=0x{}", result.toString(16));
		BigInteger expected = BigInteger.ZERO;
		assertEquals("x=0x" + x.toString(16) + ",y=" + y.toString(16),
				expected, result);
	}

	@Test
	public void multiTest() throws Exception {
		for (int i = 0; i < 20; ++i) {
			logger.debug("TryNumber {}",i);
			BigInteger inputA = new BigInteger(dataWidth, rand);
			BigInteger carryA = new BigInteger(1, rand);

			BigInteger inputB = new BigInteger(dataWidth, rand);
			BigInteger carryB = new BigInteger(1, rand);
			
			BigInteger x = getData(inputA, carryA);
			BigInteger y = getData(inputB, carryB);

			Future<BigInteger> clientThread = runClient(x);
			BigInteger result = runServer(y);
			BigInteger expected = computeAndTest(inputA, carryA, inputB, carryB);
			logger.debug("inputA=0x{}", inputA.toString(16));
			logger.debug("inputB=0x{}", inputB.toString(16));
			logger.debug("CarryA=0x{}", carryA.toString(16));
			logger.debug("CarryB=0x{}", carryB.toString(16));
			
			
			logger.debug("result=0x{}", result.toString(16));
			logger.debug("expected=0x{}", expected.toString(16));
			assertEquals("x=0x" + x.toString(16) + ",y=" + y.toString(16),
					expected, result);
			clientThread.get();
		}
	}

	private BigInteger computeAndTest(BigInteger inputA, BigInteger carryA,
			BigInteger inputB, BigInteger carryB) {

		BigInteger carry = carryA.xor(carryB);
		BigInteger writeBit = BigInteger.ZERO;
		BigInteger carryBit = BigInteger.ZERO;
		
		if (carry.equals(BigInteger.ZERO)){
			if (inputA.equals(inputB))
			{
				// carry zero and inputs equal
				writeBit = BigInteger.ONE;
			}
		}
		carryBit = carry.or(writeBit);
		return carryBit.multiply(BigInteger.valueOf(2)).add(writeBit);
	}
}
