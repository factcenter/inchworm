package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.junit.Test;

import java.math.BigInteger;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

public class XOR_ML_LTest extends GenericOpTest {

	final static int nums = 4;
	final static int numWidth = 8;
	private XOR_ML_L myCircuit;
	
	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		myCircuit = new XOR_ML_L(globals, nums, numWidth);
		return myCircuit;
	}

	@Override
	int getNumberOfInputs() {
		return nums * numWidth/2;
	}

	@Test
	public void testSimple() throws Exception {
		BigInteger clientData = BigInteger.valueOf(0x0102);
		BigInteger serverData = BigInteger.valueOf(0x0304);
						
		Future<BigInteger> clientThread = runClient(clientData);
		BigInteger result = runServer(serverData);
		clientThread.get();
		logger.debug("result=0x{}", result.toString(16));
		BigInteger expected = BigInteger.valueOf(0x4);
			
		assertEquals("x=0x"+clientData.toString(16)+",y="+serverData.toString(16), expected, result);		
	}
	
	@Test
	public void testSimple2() throws Exception {
		BigInteger clientData = BigInteger.valueOf(0x0108);
		BigInteger serverData = BigInteger.valueOf(0x0377);
						
		Future<BigInteger> clientThread = runClient(clientData);
		BigInteger result = runServer(serverData);
		clientThread.get();
		logger.debug("result=0x{}", result.toString(16));
		BigInteger expected = BigInteger.valueOf(0x7d);
			
		assertEquals("x=0x"+clientData.toString(16)+",y="+serverData.toString(16), expected, result);		
	}
	
	
}
