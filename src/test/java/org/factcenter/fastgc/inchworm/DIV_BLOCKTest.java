package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class DIV_BLOCKTest extends GenericOpTest {

	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		return new DIV_BLOCK(globals);
	}

	@Override
	int getNumberOfInputs() {
		return 2;
	}

		@Test
	public void test() throws IOException {
		
		/*-
		 *             DIV_BLOCK (modified 1 bit subtractor)
		 *                       MSB    ->    LSB                              EXPECTED RESULT
		 *                        OS, Li      B, A                                DTag, Lo
		 *                        client      server    combined input data
		 * Data of both players:  0            1               0001                (10 =2)
		 *                        0            2               0010                (11 =3)
		 *                        0            3               0011                (00 =0)
		 *                        1            2               0110                (01 =1)
		 *                        2            1               1001                (10 =2)
		 *                        2            0               1000                (00 =0)
		 */
		
		// OS, Li = 00
		runClient(BigInteger.valueOf(0));
		int result = runServer(BigInteger.valueOf(1)).intValue();
		assertEquals(2, result);
		
		runClient(BigInteger.valueOf(0));
		result = runServer(BigInteger.valueOf(2)).intValue();
		assertEquals(3, result);	
		
		runClient(BigInteger.valueOf(0));
		result = runServer(BigInteger.valueOf(3)).intValue();
		assertEquals(0, result);	
		
		// OS, Li = 01
		runClient(BigInteger.valueOf(1));
		result = runServer(BigInteger.valueOf(2)).intValue();
		assertEquals(1, result);	
		
		// OS, Li = 10
		runClient(BigInteger.valueOf(2));
		result = runServer(BigInteger.valueOf(1)).intValue();
		assertEquals(2, result);	
		
		runClient(BigInteger.valueOf(2));
		result = runServer(BigInteger.valueOf(0)).intValue();
		assertEquals(0, result);	
		
		
		
	}

}
