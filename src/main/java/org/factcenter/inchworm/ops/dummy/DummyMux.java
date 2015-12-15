package org.factcenter.inchworm.ops.dummy;

import java.math.BigInteger;


public class DummyMux extends GenericArithmeticAction {
    public DummyMux(ProtocolInfo info) {
        super(info);
    }

    @Override
    protected BigInteger[] doArithmetic(int wordSize, BigInteger... inputs) {
        BigInteger result = inputs[0].testBit(0) ? inputs[2] : inputs[1];

        BigInteger[] results = { result };
        return results;
    }
}