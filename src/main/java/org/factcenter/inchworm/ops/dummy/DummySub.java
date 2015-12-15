package org.factcenter.inchworm.ops.dummy;

import java.math.BigInteger;

public class DummySub extends GenericArithmeticAction {
    public DummySub(ProtocolInfo info) {
        super(info);
    }

    @Override
    public BigInteger[] doArithmetic(int wordSize, BigInteger... inputs) {
        // todo: carry and overflow need tests.
        BigInteger difference = inputs[0].subtract(inputs[1]).and(BigInteger.ONE.shiftLeft(wordSize-1).or(BigInteger.ONE.shiftLeft(wordSize-1).subtract(BigInteger.ONE)));
        
        int lowSetBit = difference.getLowestSetBit();

        BigInteger zero = (lowSetBit < 0 || lowSetBit >= wordSize) ? BigInteger.ONE : BigInteger.ZERO;

        BigInteger sign = difference.testBit(wordSize-1) ? BigInteger.ONE : BigInteger.ZERO;
        // according to this:
        // http://teaching.idallen.com/dat2343/10f/notes/040_overflow.txt
        BigInteger overflow = (
        		(inputs[0].testBit(wordSize-1) != inputs[1].testBit(wordSize-1)) && 
        		(difference.testBit(wordSize-1) != (inputs[0].testBit(wordSize-1))))
        			? 
        					BigInteger.ONE : 
        					BigInteger.ZERO;
        
        BigInteger borrow = inputs[0].setBit(wordSize).subtract(inputs[1]).testBit(wordSize) ? BigInteger.ZERO : BigInteger.ONE;
        BigInteger carry = borrow; // carry is set if a bit had to be borrowed to perform the subtraction.
        
        BigInteger flags = BigInteger.ZERO;
        if (carry.equals(BigInteger.ONE)) flags = flags.setBit(0);
        if (zero.equals(BigInteger.ONE)) flags = flags.setBit(1);
        if (sign.equals(BigInteger.ONE)) flags = flags.setBit(2);
        if (overflow.equals(BigInteger.ONE)) flags = flags.setBit(3);
        
        
        BigInteger[] results = { difference, carry, zero, sign, overflow, flags};
        return results;
    }
}