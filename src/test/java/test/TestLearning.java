package test;

import org.junit.experimental.categories.Category;
import test.categories.Integration;


/**
 * @author mikegarts
 * Learning test to get famliar with the project
 */
@Category({Integration.class})
public class TestLearning extends TestCommon {

	final static boolean REAL_CRYPTO_OPS = true;

	@Override
	protected boolean useSecureOps() { return REAL_CRYPTO_OPS; }
	
	
	@Override
	protected CodeDescription getCodeDescription() {
		return new CodeDescription(
				"/testLearning/learning-asm.txt", 
				null, 
				null,
				"/testLearning/big-mul-right-data.txt", 
				"/testLearning/learning-result.txt"
				);
	}
}
