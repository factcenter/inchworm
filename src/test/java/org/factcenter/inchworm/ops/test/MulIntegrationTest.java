package org.factcenter.inchworm.ops.test;

import org.factcenter.inchworm.MemoryArea;
import org.factcenter.qilin.util.BitMatrix;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MulIntegrationTest extends OpsTestCommon {

	/*-
	 * ----------------------------------------------------------------
	 *                  Test data members and setup.
	 * ----------------------------------------------------------------
	 */

	/**
	 * Assembly program for testing the mul op.
	 */


    String getTestHeader(int wordsize, int regptrsize, int romptrsize, int ramptrsize) {
        return String.format(".header\n wordsize: %d regptrsize: %d romptrsize: %d ramptrsize: %d\n",
                wordsize, regptrsize, romptrsize, ramptrsize);
    }

    String getTestCode(int wordsize, int regptrsize, int romptrsize, int ramptrsize) {
        return getTestHeader(wordsize, regptrsize, romptrsize, ramptrsize) +
                "instruction: zero xori mul halt next\n" +
                ".const\n   one = 1\n" +
                ".data\n" +
                "%r0 = 0, 1, 2, 3, 4 \n" +
                "%r10 = 10, 20, 30\n" +
                ".code\n" +
                "   zero %ctrl\n" +
                "   xori %ctrl < 0xFFFFFFFF\n" +
                "   mul %r0 < %r1, %r2\n" +
                "   halt\n" +
                "---\n" +
                "";
    }
//
	String testCode = getTestCode(32, 6, 4, 4);

	String dataLeftTest1 = ".data\n" + "%r0 = 0, 1, 2, 3, 4 \n";
	String dataLeftTest2 = ".data\n"
			+ "%r0 = 0, 0xAFFFFFFF, 0xAFFFFFFF, 3, 4 \n";

    String getData(BigInteger a, BigInteger b) {
        return String.format(".data\n  %%r0 = 0, 0x%s, 0x%s, 3, 4 \n", a.toString(16), b.toString(16));
    }


	String dataRight = ".data    // Zeros\n" + "%r0 = 0\n";

	/*-
	 * ----------------------------------------------------------------
	 *                       Test Scenarios. 
	 * ----------------------------------------------------------------
	 */

	@Test
	public void Mul_1By2_ShouldReturnTwo() throws IOException {
		// Run the test.
		runTest(getUseSecureOps(), getUseZeroRand(), testCode, dataLeftTest1, dataRight);
		
		// Test result of mul operation.
		long result = lp.getState().getReg(0) ^ rp.getState().getReg(0);
		assertEquals(2, result);
		// High-word should be zero.
		result = lp.getState().getReg(1) ^ rp.getState().getReg(1);
		assertTrue(0 == result);

	}

	@Test
	public void Mul_TwoBigNumbers_HighWordShouldNotBeZero() throws IOException {
		// Run the test.
		runTest(getUseSecureOps(), getUseZeroRand(), testCode, dataLeftTest2, dataRight);

		// Test result of mul operation high-word.
		long result = lp.getState().getReg(1) ^ rp.getState().getReg(1);
		assertTrue(0 != result);

	}


    @Test
    public void Longword_Mul_TwoBigNumbers_HighWordShouldNotBeZero() throws IOException {
        // Run the test.
        java.util.Random rand = new Random(0);

        BigInteger a = new BigInteger(512, rand);
        BigInteger b = new BigInteger(512, rand);

        runTest(getUseSecureOps(), getUseZeroRand(), getTestCode(512, 6, 4, 3), getData(a, b), dataRight);

        BitMatrix res = lp.getState().getMemory(MemoryArea.Type.TYPE_REG).load(0, 2);
        res.xor (rp.getState().getMemory(MemoryArea.Type.TYPE_REG).load(0, 2));

        BigInteger result = res.toBigInteger();

        assertEquals(a.multiply(b), result);
    }

}
