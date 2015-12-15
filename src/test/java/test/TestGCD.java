package test;

import org.junit.experimental.categories.Category;
import test.categories.Integration;


@Category(Integration.class)
public class TestGCD extends TestCommon {
	/**
	 * Flag for testing the case of both players loading program code.
	 */
	final static boolean useRightPlayerCode = false;

    @Override
    protected boolean useSecureOps() { return true; }




	@Override
	protected CodeDescription getCodeDescription() {
		return new CodeDescription(
				"/testGCD/gcd.txt", 
				null,
				"/testGCD/gcd-left-data.txt",
				"/testGCD/gcd-right-data.txt", 
				"/testGCD/gcd-reference-result.txt"
				);
	}
}
