package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.OR_L_1;
import org.junit.Test;

import java.math.BigInteger;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

public class OR_L_1Test extends GenericOpTest {

	final static int bitWidth = 6;

	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		return new OR_L_1(globals, bitWidth);
	}

	@Override
	int getNumberOfInputs() {
		return bitWidth / 2;
	}

	@Test
	public void test() throws Exception {

        final int totalBits = bitWidth ;
        BigInteger mask = BigInteger.ONE.shiftLeft(totalBits / 2).subtract(BigInteger.ONE);
		for (int i = 0; i < (1 << bitWidth); i++) {

            BigInteger completeData = BigInteger.valueOf(i);
            BigInteger clientData = completeData.shiftRight(totalBits / 2);
            BigInteger serverData = completeData.and(mask);

			Future<BigInteger> clientThread = runClient(clientData);
			BigInteger result = runServer(serverData);
			clientThread.get();
			logger.debug("value = {}, result=0x{}", i, result.toString(16));
			BigInteger expected = BigInteger.valueOf(i == 0 ? 0 : 1);
			assertEquals("x=0x" + clientData.toString(16) + ",y=" + serverData.toString(16),
					expected, result);
		}
	}

}
