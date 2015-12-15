package org.factcenter.inchworm.ops;

import org.factcenter.qilin.util.BitMatrix;
import org.junit.Test;

import java.math.BigInteger;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

/**
 * Created by talm on 09/12/15.
 */
public abstract class RolOpTest extends GenericOpTest {
    public void testRol(final BitMatrix num, BitMatrix rolVal) throws Exception {
        // Peer shares are zeroes
        final BitMatrix num1 = new BitMatrix(wordSize);
        final BitMatrix bits1 = new BitMatrix(wordSize);

        logger.debug("Testing {} ROL {}", num, rolVal);

        Future<BitMatrix[]> peerFuture = pool.submit(new Callable<BitMatrix[]>() {
            @Override
            public BitMatrix[] call() throws Exception {
                return vms[1].op.doOp(vms[1].state, num1, bits1);
            }
        });

        BitMatrix[] outputs = vms[0].op.doOp(vms[0].state, num, rolVal);

        BitMatrix[] peerOutput = peerFuture.get();

        for (int i = 0; i < outputs.length; ++i ) {
            outputs[i].xor(peerOutput[i]);
        }


        BigInteger numInt = num.toBigInteger();
        int rolInt = (int) rolVal.toInteger(wordSize);


        BigInteger mask = BigInteger.ONE.shiftLeft(rolInt).subtract(BigInteger.ONE);
        BigInteger wordMask = BigInteger.ONE.shiftLeft(wordSize).subtract(BigInteger.ONE);

        BigInteger expected = numInt.shiftLeft(rolInt).or(numInt.shiftRight(wordSize - rolInt).and(mask)).and(wordMask);

        assertEquals("Rol output is incorrect", expected, outputs[0].toBigInteger());
    }

    @Test
    public void testRandom() throws Exception {
        for (int i = 0; i < 20; ++i) {
            BitMatrix num = new BitMatrix(wordSize);
            num.fillRandom(rand);

            BitMatrix rolVal = BitMatrix.valueOf(rand.nextInt(wordSize), wordSize);

            testRol(num, rolVal);
        }
    }
}
