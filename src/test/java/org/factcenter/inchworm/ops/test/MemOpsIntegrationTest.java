
package org.factcenter.inchworm.ops.test;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class MemOpsIntegrationTest extends OpsTestCommon {

	/*-
	 * ----------------------------------------------------------------
	 *                  Test data members and setup.
	 * ----------------------------------------------------------------
	 */
	

	/**
	 * Assembly program for testing the mem ops.
	 */
	String programTemplate = ".header\n"
			+ "wordsize: %d regptrsize: 6 romptrsize: 4 ramptrsize: %d\n"
			+ "instruction: zero xori store load halt \n" + ".const\n   one = 1\n"
			+ ".data\n" + "%%r0 = 0, 1, 2, 3, 4 \n" + "%%r10 = 10, 20, 30\n"
			+ ".code\n" 
			+ "   zero %%ctrl\n" 
			+ "   xori %%ctrl < 0xFFFFFFFF\n"
			+ "   store %%r2,%%r4\n" 
			+ "   load %%r0, %%r3\n" 
			+ "   halt\n" + "---\n" + "";
	
	int memAddLen = 10;
	String testCode = String.format(programTemplate, memAddLen, memAddLen);
	
	// this means - r0=0;r1=1;r2=4;r3=4;r4=18;
	String dataLeftTest1 = ".data\n" + "%r0 = 0, 1, 4, 4, 77 \n";
	// register initialization list!
    String dataRight = ".data    // Zeros\n" + "%r0 = 0\n";	

	
	/*-
	 * ----------------------------------------------------------------
	 *                       Test Scenarios. 
	 * ----------------------------------------------------------------
	 */
	
	
	public void store_load_simple_test(boolean useSecure) throws IOException {

		long startTime = System.currentTimeMillis();
		//System.out.println("Running LoadIntegrationTests ...\n");
		// Run the test.
		runTest(useSecure, getUseZeroRand(), testCode, dataLeftTest1, dataRight);
		
		logger.info("Time in seconds: " + (System.currentTimeMillis() - startTime)/1000);

		// Test result of loadReg operation.
		long result = lp.getState().getReg(0) ^ rp.getState().getReg(0);
		
		assertEquals(77, result);	
		
	}

	@Test
	public void runAllMemorySizes() throws IOException{
		for (int size=8; size <= 16 ; size++){
			runForSingleSize(size, true);
			//runForSingleSize(size, false);
		}
	}
	
	private void runForSingleSize(int size, boolean secure) throws IOException{
		testCode = String.format(programTemplate, size,size);
		logger.info("Running for size = {} secure = {}", size, secure);
		store_load_simple_test(secure);
	}

	@Test
	public void run_dummy() throws IOException{
		store_load_simple_test(false);
	}
	
	@Test
	public void run_secure() throws IOException{
		store_load_simple_test(true);
	}
	
	//@Ignore
	@Test
	public void run_many_times() throws IOException{
		for (int i = 0; i < 16; i++) {
			logger.info("run #{}", i);
			run_dummy();
			//run_secure();
		}
	}
	
}
