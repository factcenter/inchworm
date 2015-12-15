package test;

import org.junit.experimental.categories.Category;
import test.categories.Integration;
import test.categories.Slow;


@Category({Slow.class, Integration.class})
public class TestAES extends TestCommon {

	@Override
	protected CodeDescription getCodeDescription() {
		return new CodeDescription(
				"/testAES/aes.txt",				// left-code
				null, 								// right-code
				"/testAES/aes-left-data.txt", 	// left data
				"/testAES/aes-right-data.txt",  // right data
				"/testAES/aes-result.txt" 		// result reference 
				);
	}
}
