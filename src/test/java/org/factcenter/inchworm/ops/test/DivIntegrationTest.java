package org.factcenter.inchworm.ops.test;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class DivIntegrationTest extends OpsTestCommon {


	/*-
	 * ----------------------------------------------------------------
	 *                  Test data members and setup.
	 * ----------------------------------------------------------------
	 */

    /**
     * Assembly program for testing the div op.
     */
    String testCode = ".header\n" + "wordsize: 32 regptrsize: 6 romptrsize: 4 ramptrsize: 4\n"
            + "instruction: zero xori div halt next\n" + ".const\n   one = 1\n" + ".data\n"
            + "%r0 = 0, 1, 2, 3, 4 \n" + "%r10 = 10, 20, 30\n" + ".code\n" + "   zero %ctrl\n"
            + "   xori %ctrl < 0xFFFFFFFF\n" + "   div %r10 < %r1, %r2\n" + "   halt\n" + "---\n"
            + "";

    String dataLeftTest1 = ".data\n" + "%r0 = 0, 1, 1, 3, 4 \n";
    String dataLeftTest2 = ".data\n" + "%r0 = 0, 3, 2, 3, 4 \n";
    String dataLeftTest3 = ".data\n" + "%r0 = 0, 3, 0, 3, 4 \n";
    String dataLeftTest4 = ".data\n" + "%r0 = 0, 0x43211123, 0x12345a, 3, 4 \n";

    String dataRight = ".data    // Zeros\n" + "%r0 = 0\n";

	/*-
	 * ----------------------------------------------------------------
	 *                       Test Scenarios. 
	 * ----------------------------------------------------------------
	 */

    @Test
    public void Div_1By1_ShouldReturnOneWithRemainderZero() throws IOException {
        runTest(getUseSecureOps(), getUseZeroRand(), testCode, dataLeftTest1, dataRight);

        // Test result of div operation.
        long result = lp.getState().getReg(10) ^ rp.getState().getReg(10);
        assertEquals(1, result);
        result = lp.getState().getReg(11) ^ rp.getState().getReg(11);
        assertEquals(0, result);

    }

    @Test
    public void Div_3by2_ShouldReturnOneWithRemainderOne() throws IOException {
        // Run the test.
        runTest(getUseSecureOps(), getUseZeroRand(), testCode, dataLeftTest2, dataRight);

        // Test result of div operation.
        long result = lp.getState().getReg(10) ^ rp.getState().getReg(10);
        assertEquals(1, result);
        result = lp.getState().getReg(11) ^ rp.getState().getReg(11);
        assertEquals(1, result);

    }

    @Test
    public void Div_NumberbyZero_ShouldReturnZero() throws IOException {
        // Run the test.
        runTest(getUseSecureOps(), getUseZeroRand(), testCode, dataLeftTest3, dataRight);

        // Test result of div operation.
        long result = lp.getState().getReg(10) ^ rp.getState().getReg(10);
        assertEquals(0, result);
        result = lp.getState().getReg(11) ^ rp.getState().getReg(11);
        assertEquals(0, result);

    }

    @Test
    public void Div_32bit_numbers() throws IOException {
        // Run the test.
        runTest(getUseSecureOps(), getUseZeroRand(), testCode, dataLeftTest4, dataRight);

        long dividend = lp.getState().getReg(1) ^ rp.getState().getReg(1);
        long divisor = lp.getState().getReg(2) ^ rp.getState().getReg(2);

        // Test result of div operation.
        long quotient = lp.getState().getReg(10) ^ rp.getState().getReg(10);
        assertEquals(dividend / divisor, quotient);
        long remainder = lp.getState().getReg(11) ^ rp.getState().getReg(11);
        assertEquals(dividend % divisor, remainder);
    }
}
