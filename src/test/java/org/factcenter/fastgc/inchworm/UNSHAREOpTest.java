package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class UNSHAREOpTest extends GenericOpTest {
	
	final static int wordSize = 4;
	
	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		return new UNSHARE(globals, wordSize);
	}

	@Override
	int getNumberOfInputs() {
		return wordSize * 2;
	}

	@Test
	public void test() throws IOException {
		BigInteger x = BigInteger.valueOf(0xa5);
		BigInteger y = BigInteger.valueOf(0x5a);
		runClient(x);
		
		BigInteger result = runServer(y);
		
		assertEquals(x.xor(y), result);
	}
	
	@Test
	public void multiTest() throws IOException {
		for (int i = 0; i < 10; ++i) {
			BigInteger x = new BigInteger(wordSize * 2, rand);
			BigInteger y = new BigInteger(wordSize * 2, rand);

			runClient(x);
			BigInteger result = runServer(y);
			
			assertEquals("i="+i, x.xor(y), result);
		}
	} 

}
