package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class SHARETest extends GenericOpTest {

	/**
	 * Op word size in bits. 
	 */
	final static int wordSize = 8;

	final static BigInteger MASK = BigInteger.ONE.shiftLeft(wordSize).subtract(BigInteger.ONE);
	final static BigInteger HalfMASK = BigInteger.ONE.shiftLeft(wordSize / 2).subtract(BigInteger.ONE);
	

	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		return new SHARE(globals, wordSize);
	}

	@Override
	int getNumberOfInputs() {
		return 3 * wordSize / 2;
	}

	
	BigInteger packServerInputs(BigInteger r1, BigInteger msg) {
		return r1.and(HalfMASK).shiftLeft(wordSize).or(msg.and(MASK));
	}
	
	BigInteger packClientInputs(BigInteger r0, BigInteger r1) {
		return r0.and(MASK).shiftLeft(wordSize / 2).or(r1.and(MASK).shiftRight(wordSize / 2));
	}

	/**
	 * What the computation should return
	 * 
	 */
	BigInteger compute(BigInteger x, BigInteger y) {
		BigInteger r0 = x.and(MASK).shiftRight(wordSize / 2);
		BigInteger r1 = x.and(HalfMASK).shiftLeft(wordSize / 2);
		r1 = r1.or(y.shiftRight(wordSize).and(MASK));
		BigInteger msg = y.and(MASK);

		return r0.xor(r1).xor(msg);
	}

	@Test
	public void test() throws IOException {
		BigInteger x = packClientInputs(BigInteger.valueOf(5), BigInteger.valueOf(0xA));
		BigInteger y = packServerInputs(BigInteger.valueOf(0xA), BigInteger.valueOf(5));

		runClient(x);

		BigInteger result = runServer(y);
		BigInteger expected = compute(x, y);

		assertEquals(expected, result);
	}

	@Test
	public void multiTest() throws IOException {
		for (int i = 0; i < 20; ++i) {
			BigInteger x = new BigInteger(wordSize, rand);
			BigInteger y = new BigInteger(wordSize, rand);

			runClient(x);
			BigInteger result = runServer(y);
			BigInteger expected = compute(x, y);

			assertEquals("x=" + x.toString(16) + ",y=" + y.toString(16),
					expected, result);
		}
	}

}
