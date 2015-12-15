package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.junit.Test;

import java.math.BigInteger;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

public class MUL_KTest extends GenericOpTest {
	
	final static int bitWidth = 8;

	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		return new MUL_K(globals, bitWidth);
	}

	@Override
	int getNumberOfInputs() {
		return (bitWidth);
	}

	@Test
	public void test() throws Exception {
		
		/*-
		 *        MUL_8 (8 bit multiplier)
		 *                        client    server
		 * Data of both players:  8         3
		 * 
		 */
		
		BigInteger clientData = BigInteger.valueOf(8);
		BigInteger serverData = BigInteger.valueOf(3);
						
		Future<BigInteger> clientThread = runClient(clientData);
		BigInteger result = runServer(serverData);
		clientThread.get();
		logger.debug("result=0x{}", result.toString(16));
		BigInteger expected = clientData.multiply(serverData);		
		assertEquals("Mul result (8 * 3) ", expected, result);
		
		
		clientData = BigInteger.valueOf(8);
		serverData = BigInteger.valueOf(6);
						
		clientThread = runClient(clientData);
		result = runServer(serverData);
		clientThread.get();
		logger.debug("result=0x{}", result.toString(16));
		expected = clientData.multiply(serverData);		
		assertEquals("Mul result (8 * 6) ", expected, result);
		

	}

}
