package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import test.categories.Slow;

import java.math.BigInteger;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

public class MUX_K_NTest extends GenericOpTest {

	final static int bitWidth = 4;
    final static int numBlocks = 4;
	final static int controlBits = 2;

    static class MuxParams {
        int blockSize; // K
        int numBlocks; // N
        int controlBits; // N <= 2^indexSize

        MuxParams(int blockSize, int numBlocks,  int controlBits) {
            this.blockSize = blockSize;
            this.numBlocks = numBlocks;
            this.controlBits = controlBits;
        }
    }

	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		return new MUX_K_N(globals, bitWidth, numBlocks, controlBits);
	}

    @Override
    Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode, Object param) {
        if (param == null)
            return new MUX_K_N(globals, bitWidth, numBlocks, controlBits);
        else {
            MuxParams muxParams = (MuxParams) param;
            return new MUX_K_N(globals, muxParams.blockSize, muxParams.numBlocks, muxParams.controlBits);
        }
    }

	@Override
	int getNumberOfInputs() {
		return (bitWidth * numBlocks + controlBits) / 2;
	}

    @Override
    int getNumberOfInputs(Object param) {
        if (param == null)
            return getNumberOfInputs();

        MuxParams muxParams = (MuxParams) param;
        return (muxParams.blockSize * muxParams.numBlocks + muxParams.controlBits) / 2;
    }


    @Test
	public void test() throws Exception {
		
		/*-
		 *        MUX_4_2 (select between 4 blocks of 4 bits each)
		 *    
		 *                        client             server
		 *                        cc-xxxx-xxx        x-xxxx-xxxx
		 * Data of both players: <00 0100 001>=0x21 <1 0010 0001> = 0x121
		 * Data at mux:           00 0100 0011 0010 0001
		 * 
		 * Data of both players: <01 0100 001>=0xA1
		 * Data at mux:           01 0100 0011 0010 0001
		 */
		
		BigInteger clientData = BigInteger.valueOf(0x21);
		BigInteger serverData = BigInteger.valueOf(0x121);
						
		Future<BigInteger> clientThread = runClient(clientData);
		BigInteger result = runServer(serverData);
		clientThread.get();
		logger.debug("result=0x{}", result.toString(16));
		BigInteger expected = BigInteger.valueOf(1);		
		assertEquals("Selection bits = 00", expected, result);
		
		clientData = BigInteger.valueOf(0xA1);
		clientThread = runClient(clientData);
		result = runServer(serverData);
		clientThread.get();
		logger.debug("result=0x{}", result.toString(16));
		expected = BigInteger.valueOf(2);
		assertEquals("Selection bits = 01", expected, result);
		
	}

    public void testRandom(int repeatCount, int blockSize, int numBlocks, int ctrlBits) throws Exception {
        MuxParams p = new MuxParams(blockSize, numBlocks, ctrlBits);

        int dataBits = blockSize * numBlocks;
        int partyBits = getNumberOfInputs(p);
        BigInteger mask = BigInteger.ONE.shiftLeft(partyBits).subtract(BigInteger.ONE);

        BigInteger blockMask = BigInteger.ONE.shiftLeft(blockSize).subtract(BigInteger.ONE);


        for (int i = 0; i < repeatCount; i++) {
            BigInteger randData = new BigInteger(dataBits, rand);
            int controlIdx = rand.nextInt(numBlocks);
            BigInteger control = BigInteger.valueOf(controlIdx);

            BigInteger completeData = control.shiftLeft(dataBits).or(randData);


            BigInteger clientData = completeData.shiftRight(partyBits);
            BigInteger serverData = completeData.and(mask);


            Future<BigInteger> clientThread = runClient(clientData, p);
            BigInteger result = runServer(serverData, p);
            clientThread.get();

            BigInteger expected = randData.shiftRight(controlIdx * blockSize).and(blockMask);
            assertEquals("" + i + "x=0x" + clientData.toString(16) + ",y=" + serverData.toString(16), expected, result);

        }
    }

    @Test
    public void test_256_16_4() throws Exception {
        testRandom(5, 256, 16, 4);
    }

    @Test
    @Category({Slow.class})
    public void test_1024_16_4() throws Exception {
        testRandom(5, 1024, 16, 4);
    }

    @Test
    @Category({Slow.class})
    public void test_1024_8_4() throws Exception {
        testRandom(5, 1024, 8, 4);
    }

    @Test
    public void test_32_256_8() throws Exception {
        testRandom(5, 32, 256, 8);
    }
}
