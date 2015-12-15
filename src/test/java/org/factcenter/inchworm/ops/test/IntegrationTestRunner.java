package org.factcenter.inchworm.ops.test;


import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;


public class IntegrationTestRunner {
	
	//
	// NOTE: The ops to test and the debug level
	//       are set in the TestCommon class.
	//
	
	public static void main(String[] args) {
        @SuppressWarnings("rawtypes")
		Class[] classes = {AddIntegrationTest.class, 
        	               AndIntegrationTest.class,
        	               CallRetIntegrationTest.class, 
        	               DivIntegrationTest.class,
        	               LoadIntegrationTest.class,
        	               MulIntegrationTest.class,
        	               MuxIntegrationTest.class,
        	               OrIntegrationTest.class, 
        	               RolIntegrationTest.class,
        	               XorIntegrationTest.class};
       
 		Result result = JUnitCore.runClasses(classes);
		for (Failure failure : result.getFailures()) {
			System.out.println(failure.toString());
		}
		if (result.wasSuccessful()){
			System.out.println("\nAll tests passed OK !");
		}
	}

}
