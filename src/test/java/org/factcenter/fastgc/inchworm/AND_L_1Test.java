package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.junit.Test;

import java.math.BigInteger;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

public class AND_L_1Test extends GenericOpTest {

	final static int bitWidth = 6;

	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		return new AND_L_1(globals, bitWidth);
	}

	@Override
	int getNumberOfInputs() {
		return bitWidth / 2;
	}

	@Test
	public void test() throws Exception {

		for (int i = 0; i < 8; i++) {

			BigInteger clientData = BigInteger.valueOf(i);
			BigInteger serverData = BigInteger.valueOf(i);

			Future<BigInteger> clientThread = runClient(clientData);
			BigInteger result = runServer(serverData);
			clientThread.get();
			logger.debug("value = {}, result=0x{}", i, result.toString(16));
			BigInteger expected = BigInteger.valueOf(i == 7 ? 1 : 0);
			assertEquals("x=0x" + clientData.toString(16) + ",y=" + serverData.toString(16),
					expected, result);
		}
	}

}
