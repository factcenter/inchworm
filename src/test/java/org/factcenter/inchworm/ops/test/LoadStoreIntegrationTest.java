package org.factcenter.inchworm.ops.test;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class LoadStoreIntegrationTest extends OpsTestCommon{

	/*-
	 * ----------------------------------------------------------------
	 *                  Test data members and setup.
	 * ----------------------------------------------------------------
	 */
	

	/**
	 * Assembly program for testing the load and store ops
	 */
	String testCode = ".header\n"
			+ "wordsize: 8 regptrsize: 6 romptrsize: 4 ramptrsize: 8\n"
			+ "instruction: zero xori storereg loadreg halt \n" + ".const\n   one = 1\n"
	//		+ ".data\n" + "%r0 = 0, 1, 2, 3, 4 \n" + "%r10 = 10, 20, 30\n"
			+ ".code\n" 
			+ " zero %ctrl\n" 
			+ " xori %ctrl < 0xFFFF\n"
			+ " storereg %r2, %r4\n" // REGS[4] <- 18
			+ " loadreg %r0, %r3\n"  // r0 <- REGS[4]  
			+ " halt\n" + "---\n" + "";
	
	// this means - r0=0;r1=1;r2=4;r3=4;r4=18;
	String dataLeftTest1 = ".data\n" + "%r0 = 0, 1, 4, 4, 18 \n";
	// register initialization list!
    String dataRight = ".data    // Zeros\n" + "%r0 = 0\n";	

	
	/*-
	 * ----------------------------------------------------------------
	 *                       Test Scenarios. 
	 * ----------------------------------------------------------------
	 */
	
	
	@Test
	public void LoadReg_LoadReg0FromReg3_ShouldReturnFour() throws IOException {
		// Run the test.
		runTest(getUseSecureOps(), getUseZeroRand(), testCode, dataLeftTest1, dataRight);

		// Test result of loadReg operation.
		long result = lp.getState().getReg(0) ^ rp.getState().getReg(0);
		
		// assert.
		assertEquals(18, result);
		
	}


}
