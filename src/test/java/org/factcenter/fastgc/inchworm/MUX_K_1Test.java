package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.junit.Test;

import java.math.BigInteger;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

public class MUX_K_1Test extends GenericOpTest {

	final static int bitWidth = 4; // 10;
	final static int controlBits = 2; // 4;

    static class Param {
        int bitWidth;
        int controlBits;

        Param(int bitWidth, int controlBits) {
            this.bitWidth = bitWidth;
            this.controlBits = controlBits;
        }
    }

	@Override
    Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		return new MUX_K_1(globals, bitWidth, controlBits);
	}

    @Override
    Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode, Object param) {
        if (param == null)
            return new MUX_K_1(globals, bitWidth, controlBits);

        Param p = (Param) param;
        return new MUX_K_1(globals, p.bitWidth, p.controlBits);
    }

	@Override
	int getNumberOfInputs() {
		return (bitWidth + controlBits) / 2;
	}

    @Override
    int getNumberOfInputs(Object param) {
        if (param == null)
            return (bitWidth + controlBits) / 2;

        Param p = (Param) param;
        return (p.bitWidth + p.controlBits) / 2;
    }

	@Test
	public void test() throws Exception {

		/*-
		 * MUX4_1
		 *                        client    server
		 * Data of both players: <001> = 1, <010> = 2
		 * Data at mux:               1010 
		 * Selection bits at mux:  00    
		 */
		for (int i = 0; i < 4; i++) {
			BigInteger clientData = BigInteger.valueOf(1 + 2 * i);
			BigInteger serverData = BigInteger.valueOf(2);

			Future<BigInteger> clientThread = runClient(clientData);
			BigInteger result = runServer(serverData);
			clientThread.get();
			logger.debug("expected={}, result=0x{}", i % 2, result.toString(16));
			BigInteger expected = BigInteger.valueOf(i % 2);
			assertEquals("x=0x" + clientData.toString(16) + ",y=" + serverData.toString(16), expected, result);
		}


	}

    @Test
    public void testRandom() throws Exception {

        final int bitWidth = 8;
        final int controlBits = 6;

        Param p = new Param(bitWidth, controlBits);

        final int totalBits = bitWidth + controlBits;
        BigInteger mask = BigInteger.ONE.shiftLeft(totalBits / 2).subtract(BigInteger.ONE);

        BigInteger randData = new BigInteger(bitWidth, rand);

        for (int i = 0; i < (1 << controlBits); i++) {
            BigInteger control = BigInteger.valueOf(i);
            BigInteger completeData = control.shiftLeft(bitWidth).or(randData);

            BigInteger clientData = completeData.shiftRight(totalBits / 2);
            BigInteger serverData = completeData.and(mask);


            Future<BigInteger> clientThread = runClient(clientData, p);
            BigInteger result = runServer(serverData, p);
            clientThread.get();
            logger.debug("expected={}, result=0x{}", randData.testBit(i) ? 1 : 0, result.toString(16));

            BigInteger expected = BigInteger.valueOf(randData.testBit(i) ? 1 : 0);
            assertEquals("i=" + i + " x=0x" + clientData.toString(16) + ",y=" + serverData.toString(16), expected, result);

        }

    }


    @Test
    public void testRandomLong() throws Exception {

        final int bitWidth = 1024;
        final int controlBits = 10;

        Param p = new Param(bitWidth, controlBits);

        final int totalBits = bitWidth + controlBits;
        BigInteger mask = BigInteger.ONE.shiftLeft(totalBits / 2).subtract(BigInteger.ONE);

        for (int i = 0; i < 20; i++) {
            BigInteger randData = new BigInteger(bitWidth, rand);
            int controlIdx = rand.nextInt(bitWidth);
            BigInteger control = BigInteger.valueOf(controlIdx);
            BigInteger completeData = control.shiftLeft(bitWidth).or(randData);

            BigInteger clientData = completeData.shiftRight(totalBits / 2);
            BigInteger serverData = completeData.and(mask);


            Future<BigInteger> clientThread = runClient(clientData, p);
            BigInteger result = runServer(serverData, p);
            clientThread.get();

            BigInteger expected = BigInteger.valueOf(randData.testBit(controlIdx) ? 1 : 0);
            assertEquals("" + i + "x=0x" + clientData.toString(16) + ",y=" + serverData.toString(16), expected, result);

        }

    }

}
