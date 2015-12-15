package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.junit.Test;

import java.math.BigInteger;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

public class BINARY_CONSTANT_L_Test extends GenericOpTest {

    static class Params {
        int bitWidth; // K
        int index; // N

        Params(int bitWidth, int index) {
            this.bitWidth = bitWidth;
            this.index = index;
        }
    }

	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		return null; // unused.
	}

    @Override
    Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode, Object param) {
        Params params = (Params) param;
        return new BINARY_CONSTANT_L(globals, params.bitWidth, params.index);
    }

	@Override
	int getNumberOfInputs() {
		return 0; // unused
	}

    @Override
    int getNumberOfInputs(Object param) {
        Params params = (Params) param;
        return 0;
    }


    public void testRandom(int bitWidth, int index) throws Exception {
        Params p = new Params(bitWidth, index);


        Future<BigInteger> clientThread = runClient(BigInteger.ZERO, p);
        BigInteger result = runServer(BigInteger.ZERO, p);
        clientThread.get();

        BigInteger expected = BigInteger.valueOf(index);

        assertEquals("index=" + index, expected, result);
    }

//    @Test
//    public void test_1024_16_4() throws Exception {
//        testRandom(5, 1024, 16, 4);
//    }
//
//    @Test
//    public void test_1024_8_4() throws Exception {
//        testRandom(5, 1024, 8, 4);
//    }
//
//    @Test
//    public void test_32_256_8() throws Exception {
//        testRandom(5, 32, 256, 8);
//    }

    @Test
    public void test_8_a7() throws Exception {
        testRandom(8, 0xa7);
    }

    @Test
    public void test_32_a7e456() throws Exception {
        testRandom(32, 0xa7e456);
    }

}
