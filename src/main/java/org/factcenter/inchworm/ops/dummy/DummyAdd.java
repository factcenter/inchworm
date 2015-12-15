package org.factcenter.inchworm.ops.dummy;

import java.math.BigInteger;

public class DummyAdd extends GenericArithmeticAction {
    public DummyAdd(ProtocolInfo info) {
        super(info);
    }

    @Override
    public BigInteger[] doArithmetic(int wordSize, BigInteger... inputs) {
        BigInteger sum = inputs[0].add(inputs[1]);
        BigInteger carry = sum.bitLength() > wordSize ? BigInteger.ONE : BigInteger.ZERO;
        int lowSetBit = sum.getLowestSetBit();

        BigInteger zero = (lowSetBit < 0 || lowSetBit >= wordSize) ? BigInteger.ONE : BigInteger.ZERO;

        BigInteger sign = sum.testBit(wordSize-1) ? BigInteger.ONE : BigInteger.ZERO;
        
        BigInteger overflow = (
        		(inputs[0].testBit(wordSize-1) == inputs[1].testBit(wordSize-1)) && 
        		(sum.testBit(wordSize-1) != (inputs[0].testBit(wordSize-1)))) 
        			? 
        					BigInteger.ONE : 
        					BigInteger.ZERO;
        
        BigInteger flags = BigInteger.ZERO;
        if (carry.equals(BigInteger.ONE)) flags = flags.setBit(0);
        if (zero.equals(BigInteger.ONE)) flags = flags.setBit(1);
        if (sign.equals(BigInteger.ONE)) flags = flags.setBit(2);
        if (overflow.equals(BigInteger.ONE)) flags = flags.setBit(3);
        
        
        BigInteger[] results = { sum, carry, zero, sign, overflow, flags};
        return results;
    }
}