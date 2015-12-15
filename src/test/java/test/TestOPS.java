package test;


public class TestOPS extends TestCommon {
	@Override
	protected CodeDescription getCodeDescription() {
		return new CodeDescription(
				"/testOPS/op-test.txt",				// left-code
				null, 								// right-code
				"/testOPS/op-test-left-data.txt", 	// left data
				"/testOPS/op-test-right-data.txt",  // right data
				"/testOPS/op-test-result.txt" 		// result reference 
				);
	}

}
