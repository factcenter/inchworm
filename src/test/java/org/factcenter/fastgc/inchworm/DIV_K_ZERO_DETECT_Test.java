package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import test.categories.Slow;

import java.math.BigInteger;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

public class DIV_K_ZERO_DETECT_Test extends GenericOpTest {


    static class Params {
        int wordSize; // K

        Params(int wordSize) {
            this.wordSize = wordSize;
        }
    }

	@Override
    Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		return null;
	}

    @Override
    Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode, Object param) {
        Params Params = (Params) param;
        return new DIV_K_ZERO_DETECT(globals, Params.wordSize);
    }

    @Override
    int getNumberOfInputs() {
        return -1;
    }


    @Override
    int getNumberOfInputs(Object param) {
        Params Params = (Params) param;
        return Params.wordSize;
    }

    public void testInputs(int wordSize, BigInteger a, BigInteger b, String prefix) throws Exception {
        Params p = new Params(wordSize);

        BigInteger completeData = b.shiftLeft(wordSize).or(a);

        BigInteger mask = BigInteger.ONE.shiftLeft(wordSize).subtract(BigInteger.ONE);

        BigInteger clientData = completeData.shiftRight(wordSize);
        BigInteger serverData = completeData.and(mask);


        Future<BigInteger> clientThread = runClient(clientData, p);
        BigInteger result = runServer(serverData, p);
        clientThread.get();

        BigInteger expected;
        if (b.equals(BigInteger.ZERO)) {
            expected = BigInteger.ZERO;
        } else {
            BigInteger[] qandr = a.divideAndRemainder(b);
            expected = qandr[1].shiftLeft(wordSize).or(qandr[0]);
        }
        assertEquals((prefix == null ? "" : prefix) +
                "0x" + a.toString(16) + "/0x" + b.toString(16) +
                        " = 0x" + result.toString(16) +
                        " != 0x" + expected.toString(16),
                expected, result);
    }

    public void testRandom(int repeatCount, int wordSize) throws Exception {
        for (int i = 0; i < repeatCount; i++) {
            BigInteger randA = new BigInteger(wordSize, rand);
            BigInteger randB = new BigInteger(wordSize, rand);

            testInputs(wordSize, randA, randB, "iteration " + i + ": ");
        }
    }

    @Test
    public void test_div_by_zero() throws Exception {
        testInputs(8, BigInteger.ONE, BigInteger.ZERO, "");
    }


    @Test
    public void test_8() throws Exception {
        testRandom(5, 8);
    }

    @Test
    public void test_16() throws Exception {
        testRandom(5, 16);
    }

    @Test
    public void test_32() throws Exception {
        testRandom(5, 32);
    }

    @Category({Slow.class})
    @Test
    public void test_256() throws Exception {
        testRandom(1, 256);
    }


}
