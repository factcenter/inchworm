package org.factcenter.inchworm.ops.dummy;

import java.math.BigInteger;


public class DummyDiv extends GenericArithmeticAction {
    public DummyDiv(ProtocolInfo info) {
        super(info);
    }

    @Override
    protected int getResultWidth(int wordSize, int resultIndex) {
        return  2 * wordSize;
    }

    @Override
    protected BigInteger[] doArithmetic(int wordSize, BigInteger... inputs) {

        BigInteger result;

        if (inputs[1].equals(BigInteger.ZERO))
            result = BigInteger.ZERO;
        else {
            BigInteger[] quotientAndRemainder = inputs[0].divideAndRemainder(inputs[1]);
            result = quotientAndRemainder[1].shiftLeft(wordSize).or(quotientAndRemainder[0]);
        }

        BigInteger[] results = { result };
        return results;
    }
}
