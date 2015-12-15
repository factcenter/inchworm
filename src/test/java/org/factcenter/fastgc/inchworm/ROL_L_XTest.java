package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.junit.Test;

import java.math.BigInteger;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

public class ROL_L_XTest extends GenericOpTest {

	final static int bitWidth = 32;
	final static int shiftSize = 4;
	
	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		return new ROL_L_X(globals, bitWidth, shiftSize);
	}

	@Override
	int getNumberOfInputs() {
		return bitWidth / 2;
	}

	@Test
	public void test() throws Exception {
		BigInteger clientData = BigInteger.valueOf(0x1234);
		BigInteger serverData = BigInteger.valueOf(0x5678);
						
		Future<BigInteger> clientThread = runClient(clientData);
		BigInteger result = runServer(serverData);
		clientThread.get();
		logger.debug("result=0x{}", result.toString(16));
		BigInteger expected = BigInteger.valueOf(0x23456781);
			
		assertEquals("x=0x"+clientData.toString(16)+",y="+serverData.toString(16), expected, result);
		
		
		clientData = BigInteger.valueOf(0xC3C3);
		serverData = BigInteger.valueOf(0xC3C3);
						
		clientThread = runClient(clientData);
		result = runServer(serverData);
		clientThread.get();
		logger.debug("result=0x{}", result.toString(16));
		expected = BigInteger.valueOf(0x3C3C3C3C);
			
		assertEquals("x=0x"+clientData.toString(16)+",y="+serverData.toString(16), expected, result);		
		
		
	}
}
