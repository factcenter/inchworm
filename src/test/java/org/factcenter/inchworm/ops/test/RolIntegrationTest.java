package org.factcenter.inchworm.ops.test;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class RolIntegrationTest extends OpsTestCommon {

	/*-
	 * ----------------------------------------------------------------
	 *                  Test data members and setup.
	 * ----------------------------------------------------------------
	 */

	/**
	 * Assembly program for testing the rol op.
	 */
	String testCode = ".header\n" + "wordsize: 32 regptrsize: 6 romptrsize: 4 ramptrsize: 4\n"
			+ "instruction: zero xori rol halt next\n" + ".const\n   one = 1\n" + ".data\n"
			+ "%r0 = 0, 1, 2, 3, 4 \n" + "%r10 = 10, 20, 30\n" + ".code\n" + "   zero %ctrl\n"
			+ "   xori %ctrl < 0xFFFFFFFF\n" + "  rol %r0 < %r1, %r2\n" + "   halt\n" + "---\n"
			+ "";

	String dataLeftTest1 = ".data\n" + "%r0 = 0, 1, 2, 3, 4 \n";
	String dataLeftTest2 = ".data\n" + "%r0 = 0, 8, 30, 3, 4 \n";

	String dataRight = ".data    // Zeros\n" + "%r0 = 0\n";

	/*-
	 * ----------------------------------------------------------------
	 *                       Test Scenarios. 
	 * ----------------------------------------------------------------
	 */

	@Test
	public void Rol_1By2_ShouldReturnFour() throws IOException {
		
		System.out.println("Running RolIntegrationTests ...");
		// Run the test.
		runTest(getUseSecureOps(), getUseZeroRand(), testCode, dataLeftTest1, dataRight);

		// Test result of rol operation.
		long result = lp.getState().getReg(0) ^ rp.getState().getReg(0);
		assertEquals(4, result);

	}

	@Test
	public void Rol_8By30_ShouldReturnTwo() throws IOException {

		// Run the test.
		runTest(getUseSecureOps(), getUseZeroRand(), testCode, dataLeftTest2, dataRight);

		// Test result of rol operation.
		long result = lp.getState().getReg(0) ^ rp.getState().getReg(0);
		assertEquals(2, result);

	}

}
