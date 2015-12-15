package org.factcenter.inchworm.ops.dummy;

import java.math.BigInteger;


public class DummyMul extends GenericArithmeticAction {
    public DummyMul(ProtocolInfo info) {
        super(info);
    }

    @Override
    protected int getResultWidth(int wordSize, int resultIndex) {
        return  2 * wordSize;
    }

    @Override
    protected BigInteger[] doArithmetic(int wordSize, BigInteger... inputs) {

        BigInteger result = inputs[0].multiply(inputs[1]);

        BigInteger[] results = { result };
        return results;
    }
}

