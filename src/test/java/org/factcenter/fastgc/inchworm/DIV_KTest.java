package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.junit.Test;

import java.math.BigInteger;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

public class DIV_KTest extends GenericOpTest {

	final static int bitWidth = 4;

	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		return new DIV_K(globals, bitWidth);
	}

	@Override
	int getNumberOfInputs() {
		return (bitWidth);
	}

	@Test
	public void test() throws Exception {

		/*-
		 *        DIV_4 (4 bit divider)
		 *                        client    server
		 * Data of both players:  2         8
		 *                        4         8
		 * 
		 */
		
		// No remainder.
		BigInteger clientData = BigInteger.valueOf(2);
		BigInteger serverData = BigInteger.valueOf(8);
		Future<BigInteger> clientThread = runClient(clientData);
		BigInteger result = runServer(serverData);
		clientThread.get();
		logger.debug("result=0x{}", result.toString(16));
		BigInteger expected = serverData.divide(clientData);		
		assertEquals("Div result (8 / 2) ", expected, result);
		
		clientData = BigInteger.valueOf(4);
		serverData = BigInteger.valueOf(8);
		clientThread = runClient(clientData);
		result = runServer(serverData);
		clientThread.get();
		logger.debug("result=0x{}", result.toString(16));
		expected = serverData.divide(clientData);	
		assertEquals("Div result (8 / 4) ", expected, result);		
		
		// Output bits (MS nibble - LS nibble): Remainder, Quotient.
		clientData = BigInteger.valueOf(3);
		serverData = BigInteger.valueOf(5);
		clientThread = runClient(clientData);
		result = runServer(serverData);
		clientThread.get();
		logger.debug("result=0x{}", result.toString(16));
		expected = BigInteger.valueOf(0x21);
		assertEquals("Div result (5 / 3) ", expected, result);	
		
		clientData = BigInteger.valueOf(3);
		serverData = BigInteger.valueOf(10);
		clientThread = runClient(clientData);
		result = runServer(serverData);
		clientThread.get();
		logger.debug("result=0x{}", result.toString(16));
		expected = BigInteger.valueOf(0x13);
		assertEquals("Div result (10 / 3) ", expected, result);	
		
		clientData = BigInteger.valueOf(5);
		serverData = BigInteger.valueOf(13);
		clientThread = runClient(clientData);
		result = runServer(serverData);
		clientThread.get();
		logger.debug("result=0x{}", result.toString(16));
		expected = BigInteger.valueOf(0x32);
		assertEquals("Div result (13 / 5) ", expected, result);	
		
		
	}

}
