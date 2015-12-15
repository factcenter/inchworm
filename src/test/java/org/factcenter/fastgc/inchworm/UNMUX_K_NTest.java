package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import test.categories.Slow;

import java.math.BigInteger;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

public class UNMUX_K_NTest extends GenericOpTest {

    static class MuxParams {
        int blockSize; // K
        int numBlocks; // N
        int indexSize; // N <= 2^indexSize

        MuxParams(int blockSize, int numBlocks,  int indexSize) {
            this.blockSize = blockSize;
            this.numBlocks = numBlocks;
            this.indexSize = indexSize;
        }
    }

	@Override
    Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		return null; // unused.
	}

    @Override
    Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode, Object param) {
        MuxParams muxParams = (MuxParams) param;
        return new UNMUX_K_N(globals, muxParams.blockSize, muxParams.numBlocks, muxParams.indexSize);
    }

	@Override
	int getNumberOfInputs() {
		return 0; // unused
	}

    @Override
    int getNumberOfInputs(Object param) {
        MuxParams muxParams = (MuxParams) param;
        return (muxParams.blockSize * muxParams.numBlocks + muxParams.blockSize + muxParams.indexSize) / 2;
    }


    public void testRandom(int repeatCount, int blockSize, int numBlocks, int indexSize) throws Exception {
        MuxParams p = new MuxParams(blockSize, numBlocks, indexSize);

        int dataBits = blockSize * numBlocks;

        int partyBits = getNumberOfInputs(p);

        BigInteger mask = BigInteger.ONE.shiftLeft(partyBits).subtract(BigInteger.ONE);

        BigInteger blockMask = BigInteger.ONE.shiftLeft(blockSize).subtract(BigInteger.ONE);


        for (int i = 0; i < repeatCount; i++) {
            BigInteger randData = new BigInteger(dataBits, rand);
            BigInteger newBlock = new BigInteger(blockSize, rand);

            int controlIdx = rand.nextInt(numBlocks);
            BigInteger control = BigInteger.valueOf(controlIdx);

            BigInteger completeData = control.shiftLeft(blockSize).or(newBlock).shiftLeft(dataBits).or(randData);


            BigInteger clientData = completeData.shiftRight(partyBits);
            BigInteger serverData = completeData.and(mask);

            Future<BigInteger> clientThread = runClient(clientData, p);
            BigInteger result = runServer(serverData, p);
            clientThread.get();

            BigInteger expected = randData.andNot(blockMask.shiftLeft(controlIdx * blockSize)).
                    or(newBlock.shiftLeft(controlIdx * blockSize));

//            logger.debug("{}: complete=0x{} ({},0x{},0x{}), result=0x{}", i, completeData.toString(16),
//                    controlIdx, newBlock.toString(16), randData.toString(16), result.toString(16));
            assertEquals("" + i + ": x=0x" + completeData.toString(16), expected, result);

        }
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
    public void test_8_4_2() throws Exception {
        testRandom(5, 8, 4, 2);
   }

    @Test
    public void test_8_64_6() throws Exception {
        testRandom(5, 8, 64, 6);
    }

    @Test
    @Category({Slow.class})
    public void test_128_256_8() throws Exception {
        testRandom(5, 128, 256, 8);
    }



}
