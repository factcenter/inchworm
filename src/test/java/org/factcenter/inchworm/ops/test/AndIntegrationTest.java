package org.factcenter.inchworm.ops.test;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class AndIntegrationTest extends OpsTestCommon{

	/*-
	 * ----------------------------------------------------------------
	 *                  Test data members and setup.
	 * ----------------------------------------------------------------
	 */

	/**
	 * Assembly program for testing the and op.
	 */
	String testCode = ".header\n" + "wordsize: 32 regptrsize: 6 romptrsize: 4 ramptrsize: 4\n"
			+ "instruction: zero xori and halt next\n" + ".const\n   one = 1\n" + ".data\n"
			+ "%r0 = 0, 1, 2, 3, 4 \n" + "%r10 = 10, 20, 30\n" + ".code\n" + "   zero %ctrl\n"
			+ "   xori %ctrl < 0xFFFFFFFF\n" + "   and %r1 < %r1, %r2\n" + "   halt\n" + "---\n"
			+ "";

	String dataLeftTest1 = ".data\n" + "%r0 = 0, 1, 2, 3, 4 \n";
	String dataLeftTest2 = ".data\n" + "%r0 = 0, 1, 3, 3, 4 \n";
	String dataLeftTest3 = ".data\n" + "%r0 = 0, -1, 3, 3, 4 \n";

	String dataRight = ".data    // Zeros\n" + "%r0 = 0\n";

	/*-
	 * ----------------------------------------------------------------
	 *                       Test Scenarios. 
	 * ----------------------------------------------------------------
	 */

	@Test
	public void And_OneAndTwo_ShouldReturnZero() throws IOException {
		// Run the test.
		runTest(getUseSecureOps(), getUseZeroRand(), testCode, dataLeftTest1, dataRight);
		
		// Test result of and operation.
		long result = lp.getState().getReg(1) ^ rp.getState().getReg(1);
		assertEquals(0, result);

	}

	@Test
	public void And_OneAndThree_ShouldReturnOne() throws IOException {

		// Run the test.
		runTest(getUseSecureOps(), getUseZeroRand(), testCode, dataLeftTest2, dataRight);
		
		// Test result of and operation.
		long result = lp.getState().getReg(1) ^ rp.getState().getReg(1);
		assertEquals(1, result);

	}
	
	@Test
	public void And_MinusOneAndThree_ShouldReturnThree() throws IOException {

		// Run the test.
		runTest(getUseSecureOps(), getUseZeroRand(), testCode, dataLeftTest3, dataRight);
		
		// Test result of and operation.
		long result = lp.getState().getReg(1) ^ rp.getState().getReg(1);
		assertEquals(3, result);

	}


}
