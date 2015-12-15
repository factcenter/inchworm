package org.factcenter.inchworm.ops;

import org.factcenter.qilin.util.BitMatrix;
import org.junit.Test;

import java.math.BigInteger;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

/**
 * Created by talm on 9/5/14.
 */
public abstract class DivOpTest extends GenericOpTest {
    public void testDiv(final BitMatrix dividend, BitMatrix divisor) throws Exception {
        // Peer shares are zeroes
        final BitMatrix dividend1 = new BitMatrix(wordSize);
        final BitMatrix divisor1 = new BitMatrix(wordSize);

        logger.debug("Testing {}/{}", dividend, divisor);

        Future<BitMatrix[]> peerFuture = pool.submit(new Callable<BitMatrix[]>() {
            @Override
            public BitMatrix[] call() throws Exception {
                return vms[1].op.doOp(vms[1].state, dividend1, divisor1);
            }
        });

        BitMatrix[] outputs = vms[0].op.doOp(vms[0].state, dividend, divisor);

        BitMatrix[] peerOutput = peerFuture.get();

        for (int i = 0; i < outputs.length; ++i ) {
            outputs[i].xor(peerOutput[i]);
        }

        BitMatrix quotient = outputs[0].getSubMatrixCols(0, wordSize);
        BitMatrix remainder = outputs[0].getSubMatrixCols(wordSize, wordSize);

        BigInteger iDividend = dividend.toBigInteger();
        BigInteger iDivisor = divisor.toBigInteger();

        BigInteger[] expected;

        if (iDivisor.equals(BigInteger.ZERO)) {
            BigInteger[] out = { BigInteger.ZERO, BigInteger.ZERO };
            expected = out;
        } else {
            expected = iDividend.divideAndRemainder(iDivisor);
        }

        assertEquals("Quotient is incorrect (" + quotient + ")", expected[0], quotient.toBigInteger(wordSize));
        assertEquals("Remainder is incorrect (" + remainder + ")", expected[1], remainder.toBigInteger(wordSize));
    }

    @Test
    public void test1() throws Exception {
        BitMatrix dividend = BitMatrix.valueOf(10, wordSize);
        BitMatrix divisor = BitMatrix.valueOf(3, wordSize);

        testDiv(dividend, divisor);
    }

    @Test
    public void testDivideByZero() throws Exception {
        BitMatrix dividend = BitMatrix.valueOf(3, wordSize);
        BitMatrix divisor = BitMatrix.valueOf(0, wordSize);

        testDiv(dividend, divisor);
    }


    @Test
    public void testRandom() throws Exception {
        for (int i = 0; i < 20; ++i) {
            BitMatrix dividend = new BitMatrix(wordSize);
            dividend.fillRandom(rand);

            BitMatrix divisor = new BitMatrix(wordSize);

            if (rand.nextInt(10) > 2)
                divisor.fillRandom(rand);

            testDiv(dividend, divisor);
        }
    }

}
