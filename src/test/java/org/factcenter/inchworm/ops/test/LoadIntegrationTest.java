package org.factcenter.inchworm.ops.test;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class LoadIntegrationTest extends OpsTestCommon{

	/*-
	 * ----------------------------------------------------------------
	 *                  Test data members and setup.
	 * ----------------------------------------------------------------
	 */
	

	/**
	 * Assembly program for testing the Load(reg) op.
	 */
	String testCode = ".header\n"
			+ "wordsize: 12 regptrsize: 6 romptrsize: 4 ramptrsize: 0\n"
			+ "instruction: zero xori loadreg halt  \n"  
			+ ".data\n" 
			+ ".code\n" 
			+ "   zero %ctrl\n" 
			+ "   xori %ctrl < 0xFFFF\n"
			+ "   loadreg %r0, %r3\n" 
			+ "   halt\n" 
			+ "---\n" + "";

	String dataLeftTest1 = ".data\n" + "%r0 = 0, 1, 2, 4, 4 \n";
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
		long left = lp.getState().getReg(0);
		long right = rp.getState().getReg(0);
		long result =  left ^ right ;
		
//		for (int i = 0 ; i < lp.getState().getNumRegs() ; ++i)
//		{
//			System.out.printf("reg %d left = %d right=%d\n", i, lp.getState().getReg(i), rp.getState().getReg(i));
//		}
		
		// assert.
		assertEquals(4, result);
		
	}


}
