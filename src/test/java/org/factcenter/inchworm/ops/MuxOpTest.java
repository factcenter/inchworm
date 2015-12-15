package org.factcenter.inchworm.ops;

import org.factcenter.qilin.util.BitMatrix;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

/**
 * Test Mux operation.
 */
public abstract class MuxOpTest extends GenericOpTest {
    public void testMux(final BitMatrix choice, BitMatrix input0, BitMatrix input1) throws Exception {
        // Peer shares are zeroes
        final BitMatrix choiceZero = new BitMatrix(wordSize);
        final BitMatrix input0Zero = new BitMatrix(wordSize);
        final BitMatrix input1Zero = new BitMatrix(wordSize);

        logger.debug("Testing {} & 1 == 0 ? {} : {}", choice, input0, input1);

        Future<BitMatrix[]> peerFuture = pool.submit(new Callable<BitMatrix[]>() {
            @Override
            public BitMatrix[] call() throws Exception {
                return vms[1].op.doOp(vms[1].state, choiceZero, input0Zero, input1Zero);
            }
        });

        BitMatrix[] outputs = vms[0].op.doOp(vms[0].state, choice, input0, input1);

        BitMatrix[] peerOutput = peerFuture.get();

        for (int i = 0; i < outputs.length; ++i ) {
            outputs[i].xor(peerOutput[i]);
        }



        BitMatrix expected = choice.getBit(0) == 0 ? input0 : input1;

        assertEquals("Mux output is incorrect", expected, outputs[0]);
    }

    @Test
    public void test1() throws Exception {
        BitMatrix choice = BitMatrix.valueOf(10, wordSize);
        BitMatrix input0 = BitMatrix.valueOf(3, wordSize);
        BitMatrix input1 = BitMatrix.valueOf(4, wordSize);

        testMux(choice, input0, input1);
    }

    @Test
    public void testRandom() throws Exception {
        for (int i = 0; i < 20; ++i) {
            BitMatrix choice = new BitMatrix(wordSize);
            choice.fillRandom(rand);

            BitMatrix input0 = new BitMatrix(wordSize);
            input0.fillRandom(rand);

            BitMatrix input1 = new BitMatrix(wordSize);
            input1.fillRandom(rand);

            testMux(choice, input0, input1);
        }
    }
}
