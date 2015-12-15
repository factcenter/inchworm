package org.factcenter.inchworm.ops.dummy;

import java.math.BigInteger;

public class DummyOr extends GenericArithmeticAction {
    public DummyOr(ProtocolInfo info) {
        super(info);
    }

    @Override
    protected BigInteger[] doArithmetic(int wordSize, BigInteger... inputs) {
        BigInteger result = inputs[0].or(inputs[1]);

        BigInteger[] results = { result };
        return results;
    }
}