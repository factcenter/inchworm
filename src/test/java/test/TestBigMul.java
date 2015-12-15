package test;

import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import test.categories.Integration;
import test.categories.Slow;
@RunWith(Suite.class)
@Suite.SuiteClasses({
	TestBigMul.TestBigMul8_32Bit.class,
	TestBigMul.TestBigMul16_64Bit.class,
	TestBigMul.TestBigMul32_128Bit.class,
})
@Category({Slow.class, Integration.class})
public class TestBigMul {

	final static boolean REAL_CRYPTO_OPS = true;
	
	public static class TestBigMul8_32Bit extends TestCommon {
		@Override
		protected boolean useSecureOps() { return REAL_CRYPTO_OPS; };
		
		@Override
		protected CodeDescription getCodeDescription() {
			return new CodeDescription(
					"/testBigMul/big-mul-new-8-32bit-counter9.txt",	// left-code
					null,			 								// right-code
					null, 											// left data
					"/testBigMul/big-mul-right-data.txt",  			// right data
					"/testBigMul/big-mul-8-32bit-result.txt" 		// result reference 
					);
		}
	}
	
	public static class TestBigMul16_64Bit extends TestCommon {
		@Override
		protected boolean useSecureOps() { return REAL_CRYPTO_OPS; };
		
		@Override
		protected CodeDescription getCodeDescription() {
			return new CodeDescription(
					"/testBigMul/big-mul-new-16-64bit-counter9.txt",	// left-code
					null,			 								// right-code
					null, 											// left data
					"/testBigMul/big-mul-right-data.txt",  			// right data
					"/testBigMul/big-mul-16-64bit-result.txt" 		// result reference 
					);
		}
	}
	

	public static class TestBigMul32_128Bit extends TestCommon {
		@Override
		protected boolean useSecureOps() { return REAL_CRYPTO_OPS; };
		
		@Override
		protected CodeDescription getCodeDescription() {
			return new CodeDescription(
					"/testBigMul/big-mul-new-32-128bit-counter9.txt",	// left-code
					null,			 								// right-code
					null, 											// left data
					"/testBigMul/big-mul-right-data.txt",  			// right data
					"/testBigMul/big-mul-32-128bit-result.txt" 		// result reference 
					);
		}
	}
	
	
//	@Test
//	public void timingBigMul16_128Bit() {
//		
//		// NOTE: No verification - just timing.
//		
//		String srcFileLeft = "examples/big-mul-new-16-128bit-counter9.txt";
//		String dataFileLeft = null;
//		String srcFileRight = null;
//		String dataFileRight = "examples/big-mul-right-data.txt";
//		
//		// Create two players.
//		TwoPcApplication tpc = new TwoPcApplication(true, srcFileLeft, dataFileLeft, srcFileRight, dataFileRight);
//		// Run the secure computation.
//		tpc.run2PC(Level.DEBUG);
//		
//	}
	
//	@Test
//	public void testBigMul16_256Bit() {
//		
//		// NOTE: No verification - just timing.
//		
//		String srcFileLeft = "examples/big-mul-new-16-256bit-counter9.txt";
//		String dataFileLeft = null;
//		String srcFileRight = null;
//		String dataFileRight = "examples/big-mul-right-data.txt";
//		
//		// Create two players.
//		TwoPcApplication tpc = new TwoPcApplication(true, srcFileLeft, dataFileLeft, srcFileRight, dataFileRight);
//		// Run the secure computation.
//		tpc.run2PC(Level.DEBUG);
//
//	}
	
	
//	@Test
//	public void testBigMul32_256Bit() {
//		
//		// NOTE: No verification - just timing.
//		
//		String srcFileLeft = "examples/big-mul-new-32-256bit-counter9.txt";
//		String dataFileLeft = null;
//		String srcFileRight = null;
//		String dataFileRight = "examples/big-mul-right-data.txt";
//		
//		// Create two players.
//		TwoPcApplication tpc = new TwoPcApplication(true, srcFileLeft, dataFileLeft, srcFileRight, dataFileRight);
//		// Run the secure computation.
//		tpc.run2PC(Level.DEBUG);
//		
//	}	
	
//	@Test
//	public void testBigMul32_512Bit() {
//		
//		// NOTE: No verification - just timing.
//		
//		String srcFileLeft = "examples/big-mul-new-32-512bit-counter9.txt";
//		String dataFileLeft = null;
//		String srcFileRight = null;
//		String dataFileRight = "examples/big-mul-right-data.txt";
//		
//		// Create two players.
//		TwoPcApplication tpc = new TwoPcApplication(REAL_CRYPTO_OPS, srcFileLeft, dataFileLeft, srcFileRight, dataFileRight);
//		// Run the secure computation.
//		tpc.run2PC(Level.DEBUG);
//		
//	}	
	
}
