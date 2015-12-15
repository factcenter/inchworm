package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.junit.Test;

import java.math.BigInteger;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

public class UNARY_CTR_K_Test extends GenericOpTest {

    static class Params {
        int binaryCtrLen; // K
        int unaryCtrLen; // N

        Params(int binaryCtrLen, int unaryCtrLen) {
            this.binaryCtrLen = binaryCtrLen;
            this.unaryCtrLen = unaryCtrLen;
        }
    }

	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		return null;
	}

    @Override
    Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode, Object param) {
        Params params = (Params) param;
        return new UNARY_CTR_K(globals, params.binaryCtrLen, params.unaryCtrLen);
    }

	@Override
	int getNumberOfInputs() {
		return 0;
	}

    @Override
    int getNumberOfInputs(Object param) {
        Params params = (Params) param;
        return params.binaryCtrLen / 2;
    }



    public void testRandom(int repeatCount, int binaryCtrLen, int unaryCtrLen) throws Exception {
        Params p = new Params(binaryCtrLen, unaryCtrLen);

        int binaryCtrBits = binaryCtrLen;
        int partyBits = getNumberOfInputs(p);
        BigInteger mask = BigInteger.ONE.shiftLeft(partyBits).subtract(BigInteger.ONE);

        BigInteger blockMask = BigInteger.ONE.shiftLeft(binaryCtrLen).subtract(BigInteger.ONE);


        for (int i = 0; i < repeatCount; i++) {
            int counter = rand.nextInt(unaryCtrLen);

            BigInteger completeData = BigInteger.valueOf(counter);

            BigInteger clientData = completeData.shiftRight(partyBits);
            BigInteger serverData = completeData.and(mask);

            Future<BigInteger> clientThread = runClient(clientData, p);
            BigInteger result = runServer(serverData, p);
            clientThread.get();

            BigInteger expected = BigInteger.ONE.shiftLeft(counter);
            assertEquals("" + i + "x=0x" + clientData.toString(16) + ",y=" + serverData.toString(16), expected, result);

        }
    }

    @Test
    public void test_2_4() throws Exception {
        testRandom(5, 2, 4);
    }


    @Test
    public void test_4_16() throws Exception {
        testRandom(5, 4, 16);
    }

    @Test
    public void test_10_1024() throws Exception {
        testRandom(5, 4, 16);
    }
}
