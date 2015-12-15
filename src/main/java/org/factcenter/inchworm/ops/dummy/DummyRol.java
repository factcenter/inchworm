package org.factcenter.inchworm.ops.dummy;

import java.math.BigInteger;


public class DummyRol extends GenericArithmeticAction {
    public DummyRol(ProtocolInfo info) {
        super(info);
    }

    @Override
    protected BigInteger[] doArithmetic(int wordSize, BigInteger... inputs) {

        int rolVal = inputs[1].intValue() % wordSize;

        BigInteger mask = BigInteger.ONE.shiftLeft(rolVal).subtract(BigInteger.ONE);

        BigInteger result = inputs[0].shiftLeft(rolVal).or(inputs[0].shiftRight(wordSize - rolVal).and(mask));

        BigInteger[] results = { result.and(BigInteger.ONE.shiftLeft(wordSize).subtract(BigInteger.ONE)) };
        logger.debug("ROL[{}] {}, {} = {}", wordSize, inputs[0], inputs[1], results[0] );
        return results;
    }
}
