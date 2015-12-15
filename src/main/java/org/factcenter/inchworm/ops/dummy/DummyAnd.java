package org.factcenter.inchworm.ops.dummy;

import java.math.BigInteger;


public class DummyAnd extends GenericArithmeticAction {
    public DummyAnd(ProtocolInfo info) {
        super(info);
    }

    @Override
    protected BigInteger[] doArithmetic(int wordSize, BigInteger... inputs) {
        BigInteger result = inputs[0].and(inputs[1]);

        BigInteger[] results = { result };
        return results;
    }
}