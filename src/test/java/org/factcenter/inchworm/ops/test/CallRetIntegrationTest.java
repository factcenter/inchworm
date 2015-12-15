package org.factcenter.inchworm.ops.test;

import org.factcenter.inchworm.VMState;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class CallRetIntegrationTest extends OpsTestCommon {

	/*-
	 * ----------------------------------------------------------------
	 *                  Test data members and setup.
	 * ----------------------------------------------------------------
	 */

	/**
	 * Assembly program for testing the call / return ops.
	 */
	String testCode = "\n"
			+ ".header\n"
	        + "wordsize: 8 regptrsize: 8 romptrsize: 4 ramptrsize: 0 counters: 5 9 framesize: 4 stackptrsize: 4\n"
			+ "instruction: zero add halt next call return\n"
	        + ".const cone = 1 \n"
			+ ".data %r0 db 0, 1, 2, 3, 4, 5, 6, -1\n"
			+ ".code\n" 
			+ "    zero %zero\n"
			+ "    add %local[2] < %r2, %r3        // INIT %local2: %local2 = 5\n"
			+ "    next %r0, %r3                   // Advance to last statement (after the call)\n"
			+ "    call codelabel1                 // Jump to sub in ip = 1\n"
			+ "--- // 0 \n"
			+ "codelabel1:                         // sub #1. \n"			
			+ "    zero %local[2]                  // OVERWRITE %local2: %local2 = 0\n"
			+ "    add %r8 < %r5, %r1              // CHANGE %r8: result: %r8 = 6\n"
			+ "    next %ip, %r1                   // Advance to next statement (should NOT happen)\n"
			+ "    return                          // return to the predefined ip (= 3)\n"
			+ "--- // 1 \n"
			+ "    zero %r8                        // This should NOT be executed.\n"
			+ "    next %ip, %r1                   // Advance to next statement (or the return statement)\n"
			+ "--- // 2    \n"
			+ "    zero %ctrl\n"
			+ "    add %ctrl < %r0, %r7            // load ctrl-reg with the exit value (stop condition).\n"
			+ "    halt\n"
			+ "    next %ip, %r6                   // Advance to next statement\n"
			+ "--- // 3 \n"
			+ "";
	
	String dataLeft  = ".data %r0 db 0, 1, 2, 3, 4, 5, 6, -1\n";
	String dataRight = ".data\n   // Zeros\n";

//    @Override
//    public boolean getUseZeroRand() { return true; }

//    @Override
//    public boolean getUseSecureOps() { return false; }

	@Test
	public void test() throws IOException {
		// Run the test.
		runTest(getUseSecureOps(), getUseZeroRand(), testCode, dataLeft, dataRight);

        VMState lState = lp.getState();
        VMState rState = rp.getState();
        // Test result at %r[8] (should be 6) and %local[2] (should remain 5).
		long result = lState.getReg(8) ^ rState.getReg(8);
		assertEquals(6, result);

		result = lState.getReg(lState.getLocalRegisterLocation(2)) ^ rState.getReg(rState.getLocalRegisterLocation(2));
		assertEquals(5, result);
		
	}
	
	String testCode1 = "\n"
			+ ".header\n"
	        + "wordsize: 8 regptrsize: 8 romptrsize: 4 ramptrsize: 0 counters: 5 9 framesize: 4 stackptrsize: 4\n"
			+ "instruction: zero loadreg add add mux storereg halt next call return\n"
	        + ".const cone = 1 \n"
			+ ".data %r0 db 0, 1, 2, 3, 4, 5, 6, -1, 0, 5, 0\n"
			+ ".code\n"
			+ "    zero %zero\n"
			+ "    add %local[2] < %r2, %r3        // INIT %local2: %local2 = 5\n"
			+ "    next %ip, %r1                   // Advance to next statement\n"
			+ "--- // 0 \n"
			+ "    next %ip, %r4                   // We should return to last statement (#5) on end\n"
			+ "    call codelabel1                 // Jump to sub in ip = 2\n"
			+ "--- // 1 \n"
			+ "// ================================================================\n"
		  	+ "codelabel1:                         // sub #1. \n"
			+ "    zero %local[2]                  // OVERWRITE %local2: %local2 = 0\n"
			+ "    loadreg %r10, %r9               // %r10 contains the value stored in (%r[5]).\n"
			+ "    add %r8 < %r8, %r10             // %r8 = %r8 + %r10\n"
			+ "    add %r9 < %r9, %r7              // decrement %r9 (number of rounds).\n"
			+ "    mux %r10 < %zero, %r1, %r2      // zero is set if %r9 = 0 (last reg op is assigned to %r_regRet).\n"
			+ "    next %r2, %r10                  // Advance to next statement (3 recurse or 4 end)\n"
			+ "--- // 2 \n"
			+ "// ================================================================\n"
			+ "    next %ip, %r1                   // Next statement = #4\n"
			+ "    call codelabel1                 // Recursive call \n"
			+ "--- // 3    \n"
			+ "// ================================================================\n"
			+ "    return                          // return to the predefined ip\n"
			+ "--- // 4    \n"
			+ "// ================================================================\n"
			+ "    zero %ctrl\n"
			+ "    add %ctrl < %r0, %r7            // load ctrl-reg with the exit value (stop condition).\n"
			+ "    halt\n"
			+ "    next %ip, %r6                   // Advance to next statement\n"
			+ "--- // 5 \n"			
			+ "";
	
	String dataLeft1  = ".data %r0 db 0, 1, 2, 3, 4, 5, 6, -1, 0, 5, 0\n";
	String dataRight1 = ".data\n   // Zeros\n";
	
	@Test
	public void testRecursion() throws IOException {
		// Run the test.
		runTest(getUseSecureOps(), getUseZeroRand(), testCode1, dataLeft1, dataRight1);

        VMState lState = lp.getState();
        VMState rState = rp.getState();

		// Test result at %r[8] (should be 15) and %local[2] (should remain 5).
		long result = lState.getReg(8) ^ rState.getReg(8);
		assertEquals(15, result);
		result = lState.getReg(lState.getLocalRegisterLocation(2)) ^ rState.getReg(rState.getLocalRegisterLocation(2));
		assertEquals(5, result);
		
	}

}
