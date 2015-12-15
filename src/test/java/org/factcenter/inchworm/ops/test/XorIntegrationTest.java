package org.factcenter.inchworm.ops.test;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class XorIntegrationTest extends OpsTestCommon{

	/*-
	 * ----------------------------------------------------------------
	 *                  Test data members and setup.
	 * ----------------------------------------------------------------
	 */

	/**
	 * Assembly program for testing the xor op.
	 */
	String testCode = ".header\n" + "wordsize: 32 regptrsize: 6 romptrsize: 4 ramptrsize: 4\n"
			+ "instruction: zero xori xor halt next\n" + ".const\n   one = 1\n" + ".data\n"
			+ "%r0 = 0, 1, 2, 3, 4 \n" + "%r10 = 10, 20, 30\n" + ".code\n" + "   zero %ctrl\n"
			+ "   xori %ctrl < 0xFFFFFFFF\n" + "  xor %r0 < %r1, %r2\n" + "   halt\n" + "---\n"
			+ "";

	String dataLeftTest1 = ".data\n" + "%r0 = 0, 0, 0, 3, 4 \n";
	String dataLeftTest2 = ".data\n" + "%r0 = 0, 1, 0, 3, 4 \n";
	String dataLeftTest3 = ".data\n" + "%r0 = 0, 1, 1, 3, 4 \n";

	String dataRight = ".data    // Zeros\n" + "%r0 = 0\n";

	/*-
	 * ----------------------------------------------------------------
	 *                       Test Scenarios. 
	 * ----------------------------------------------------------------
	 */

	@Test
	public void Xor_ZeroAndZero_ShouldReturnZero() throws IOException {
		// Run the test.
		runTest(getUseSecureOps(), getUseZeroRand(), testCode, dataLeftTest1, dataRight);

		// Test result of xor operation.
		long result = lp.getState().getReg(0) ^ rp.getState().getReg(0);

		// assert.
		assertEquals(0, result);

	}

	@Test
	public void Xor_OneAndZero_ShouldReturnOne() throws IOException {
		// Run the test.
		runTest(getUseSecureOps(), getUseZeroRand(), testCode, dataLeftTest2, dataRight);

		// Test result of xor operation.
		long result = lp.getState().getReg(0) ^ rp.getState().getReg(0);
		// assert.
		assertEquals(1, result);

	}

	@Test
	public void Xor_OneAndOne_ShouldReturnZero() throws IOException {
		// Run the test.
		runTest(getUseSecureOps(), getUseZeroRand(), testCode, dataLeftTest3, dataRight);

		// Test result of xor operation.
		long result = lp.getState().getReg(0) ^ rp.getState().getReg(0);

		// assert.
		assertEquals(0, result);

	}

}
