package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class MUL_BLOCKTest extends GenericOpTest {

	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		return new MUL_BLOCK(globals);
	}

	@Override
	int getNumberOfInputs() { return 2; }

	@Test
	public void test01() throws IOException {
		
		/*-
		 *             MUL_BLOCK (1 bit multiplier)
		 *                       MSB    ->    LSB
		 *                   SUM_IN, C_IN     Y, X       
		 *                        client      server        combined input data
		 * Data of both players:  0            1                 0001
		 *                        1            1                 0101
		 *                        3            1                 1101
		 */
		
		runClient(BigInteger.valueOf(0));
		int result = runServer(BigInteger.valueOf(1)).intValue();
		assertEquals(0, result);
		
		
		runClient(BigInteger.valueOf(1));
		result = runServer(BigInteger.valueOf(1)).intValue();
		assertEquals(1, result);	
		
		runClient(BigInteger.valueOf(3));
		result = runServer(BigInteger.valueOf(1)).intValue();
		assertEquals(2, result);	
		
	}
	
	
	@Test
	public void test11() throws IOException {
		
		/*-
		 *             MUL_BLOCK (1 bit multiplier)
		 *                       MSB    ->    LSB
		 *                   SUM_IN, C_IN     Y, X       
		 *                        client      server        combined input data
		 * Data of both players:  0            3                 0011
		 *                        1            3                 0111
		 *                        3            3                 1111
		 */
		
		runClient(BigInteger.valueOf(0));
		int result = runServer(BigInteger.valueOf(3)).intValue();
		assertEquals(1, result);
		
		
		runClient(BigInteger.valueOf(1));
		result = runServer(BigInteger.valueOf(3)).intValue();
		assertEquals(2, result);	
		
		runClient(BigInteger.valueOf(3));
		result = runServer(BigInteger.valueOf(3)).intValue();
		assertEquals(3, result);	
		
	}

}
