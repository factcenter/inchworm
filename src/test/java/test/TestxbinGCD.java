package test;

import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import test.categories.Integration;
import test.categories.Slow;
@RunWith(Suite.class)
@Suite.SuiteClasses({
	TestxbinGCD.TestXbinGCD_32_128Bit.class,
	TestxbinGCD.TestXbinGCD_NEW_32_128Bit.class
})

@Category(Slow.class)
public class TestxbinGCD {
	final static boolean REAL_CRYPTO_OPS = true;

	@Category(Slow.class)
	public static class TestXbinGCD_32_128Bit extends TestCommon {
		@Override
		protected boolean useSecureOps() { return REAL_CRYPTO_OPS; };
		
		@Override
		protected CodeDescription getCodeDescription() {
			return new CodeDescription(
					"/testXBinGCD/xbinGCD-32-128bit.txt",	// left-code
					null,			 								// right-code
					null, 											// left data
					"/testXBinGCD/xbingcd-right-data.txt",  			// right data
					"/testXBinGCD/xbinGCD-32-128bit-result.txt" 		// result reference 
					);
		}
	}

	@Category({Slow.class, Integration.class})
	public static class TestXbinGCD_NEW_32_128Bit extends TestCommon {
		@Override
		protected boolean useSecureOps() { return REAL_CRYPTO_OPS; };
		
		@Override
		protected CodeDescription getCodeDescription() {
			return new CodeDescription(
					"/testXBinGCD/xbinGCD-NEW-32-128bit.txt",	// left-code
					null,			 								// right-code
					null, 											// left data
					"/testXBinGCD/xbingcd-right-data.txt",  			// right data
					"/testXBinGCD/xbinGCD-32-128bit-result.txt" 		// result reference 
					);
		}
	
	}

}
