package org.factcenter.inchworm.ops.test;

import org.junit.Test;

import java.io.IOException;

import static org.factcenter.inchworm.Constants.NamedReg.R_CARRY;
import static org.factcenter.inchworm.Constants.NamedReg.R_ZERO;
import static org.junit.Assert.assertEquals;

public class AddIntegrationTest extends OpsTestCommon {

	/*-
	 * ----------------------------------------------------------------
	 *                  Test data members and setup.
	 * ----------------------------------------------------------------
	 */

	/**
	 * Assembly program for testing the add op.
	 */
	String testCode = getTestCode(32, 6, 4, 4);


    String getTestHeader(int wordsize, int regptrsize, int romptrsize, int ramptrsize) {
        return String.format(".header\n wordsize: %d regptrsize: %d romptrsize: %d ramptrsize: %d\n",
                wordsize, regptrsize, romptrsize, ramptrsize);
    }

    String getTestCode(int wordsize, int regptrsize, int romptrsize, int ramptrsize) {
        return getTestHeader(wordsize, regptrsize, romptrsize, ramptrsize) +
                "instruction: zero xori add halt next\n" +
                ".const\n   one = 1\n" +
                ".data\n" +
                    "%r0 = 0, 1, 2, 3, 4 \n" +
                    "%r10 = 10, 20, 30\n" +
                ".code\n" +
                "   zero %ctrl\n" +
                "   xori %ctrl < 0xFFFFFFFF\n" +
                "   add %r1 < %r2, %r3\n" +
                "   halt\n" +
                "---\n" +
                "";
    }

	String dataLeftTest1 = ".data\n" + "%r0 = 0, 1, 2, 3, 4 \n";
	String dataLeftTest2 = ".data\n" + "%r0 = 0, 1, 0xFFFFFFFF, 0x5000, 4 \n";
	String dataLeftTest3 = ".data\n" + "%r0 = 0, 1, 2, -2, 4 \n";
	String dataLeftTest4 = ".data\n" + "%r0 = 0, 1, 2, -1, 4 \n";

	String dataRight = ".data    // Zeros\n" + "%r0 = 0\n";


	/*-
	 * ----------------------------------------------------------------
	 *                       Test Scenarios. 
	 * ----------------------------------------------------------------
	 */
    long getCarry() throws IOException {
        return  lp.getState().getNamedReg(R_CARRY).toInteger(1) ^ rp.getState().getNamedReg(R_CARRY).toInteger(1);
    }

    long getZero() throws IOException {
        return  lp.getState().getNamedReg(R_ZERO).toInteger(1) ^ rp.getState().getNamedReg(R_ZERO).toInteger(1);
    }


	@Test
	public void Add_2PositiveSmallNumbers_ShouldReturnSumAndFlagsNotUp() throws IOException {
		// Run the test.
		runTest(getUseSecureOps(), getUseZeroRand(), testCode, dataLeftTest1, dataRight);

		// Test sum.
		long result = lp.getState().getReg(1) ^ rp.getState().getReg(1);
		assertEquals(5, result);
		// Test flags.
		long carry = getCarry();
		assertEquals(0, carry);
		long zero = getZero();
		assertEquals(0, zero);

	}

	@Test
	public void Add_2PositiveBigNumbers_OverflowFlagUp() throws IOException {
		// Run the test.
		runTest(getUseSecureOps(), getUseZeroRand(), testCode, dataLeftTest2, dataRight);
		
		// Test sum.
		long result = lp.getState().getReg(1) ^ rp.getState().getReg(1);
		assertEquals(0x4FFF, result);
		// Test flags.
        long carry = getCarry();
		assertEquals(1, carry);
        long zero = getZero();
		assertEquals(0, zero);

	}

	@Test
	public void Add_NumberToItsNegative_BothFlagUp() throws IOException {
		// Run the test.
		runTest(getUseSecureOps(), getUseZeroRand(), testCode, dataLeftTest3, dataRight);
		
		// Test sum.
		long result = lp.getState().getReg(1) ^ rp.getState().getReg(1);
		assertEquals(0, result);
		// Test flags.
        long carry = getCarry();
		assertEquals(1, carry);
        long zero = getZero();
		assertEquals(1, zero);

	}

	@Test
	public void Sub_One_From_Number_CarryFlagUp() throws IOException {
		// Run the test.
		runTest(getUseSecureOps(), getUseZeroRand(), testCode, dataLeftTest4, dataRight);

		// Test sum.
		long result = lp.getState().getReg(1) ^ rp.getState().getReg(1);
		assertEquals(1, result);
		// Test flags.
        long carry = getCarry();
		assertEquals(1, carry);
        long zero = getZero();
		assertEquals(0, zero);

	}

    @Test
    public void LongWord_Sub_One_From_Number_CarryFlagUp() throws IOException {
        // Run the test.
        runTest(getUseSecureOps(), getUseZeroRand(), getTestCode(512, 6, 4, 4), dataLeftTest4, dataRight);

        // Test sum.
        long result = lp.getState().getReg(1) ^ rp.getState().getReg(1);
        assertEquals(1, result);
        // Test flags.
        long carry = getCarry();
        assertEquals(1, carry);
        long zero = getZero();
        assertEquals(0, zero);

    }

}
