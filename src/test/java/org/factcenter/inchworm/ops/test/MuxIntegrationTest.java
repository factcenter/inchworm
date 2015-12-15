package org.factcenter.inchworm.ops.test;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class MuxIntegrationTest extends OpsTestCommon{

	/*-
	 * ----------------------------------------------------------------
	 *                  Test data members and setup.
	 * ----------------------------------------------------------------
	 */

	/**
	 * Assembly program for testing the mux op.
	 */
	String testCode = ".header\n"
			+ "wordsize: 32 regptrsize: 6 romptrsize: 4 ramptrsize: 4\n"
			+ "instruction: zero xori mux halt next\n" + ".const\n   one = 1\n"
			+ ".data\n" + "%r0 = 0, 1, 2, 3, 4 \n" + "%r10 = 10, 20, 30\n"
			+ ".code\n" + "   zero %ctrl\n" + "   xori %ctrl < 0xFFFFFFFF\n"
			+ "   mux %r0 < %r1, %r2, %r3\n" + "   halt\n" + "---\n" + "";

	String dataLeftTest1 = ".data\n" + "%r0 = 0, 5, 2, 3, 4 \n";
	String dataLeftTest2 = ".data\n" + "%r0 = 0, 4, 2, 3, 4 \n";

	String dataRight = ".data    // Zeros\n" + "%r0 = 0\n";

	/*-
	 * ----------------------------------------------------------------
	 *                       Test Scenarios. 
	 * ----------------------------------------------------------------
	 */

	@Test
	public void Mux_FiveTwoAndThree_ShouldReturnThree() throws IOException {
		// Run the test.
		runTest(getUseSecureOps(), getUseZeroRand(), testCode, dataLeftTest1, dataRight);

		// Test result of mux operation.
		long result = lp.getState().getReg(0) ^ rp.getState().getReg(0);
		assertEquals(3, result);

	}

	@Test
	public void Mux_FourTwoAndThree_ShouldReturnTwo() throws IOException {
		// Run the test.
		runTest(getUseSecureOps(), getUseZeroRand(), testCode, dataLeftTest2, dataRight);
		
		// Test result of mux operation.
		long result = lp.getState().getReg(0) ^ rp.getState().getReg(0);
		assertEquals(2, result);

	}

}
