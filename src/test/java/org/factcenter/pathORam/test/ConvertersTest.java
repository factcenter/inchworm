package org.factcenter.pathORam.test;

import org.bouncycastle.util.Arrays;
import org.factcenter.inchworm.Converters;
import org.factcenter.pathORam.Block;
import org.factcenter.qilin.util.BitMatrix;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConvertersTest {

	@Test
	public void testConverter(){
		int indLen = 8;
		int dataLen = 4*8;
		
		BitMatrix tmp = new BitMatrix(dataLen);
		tmp.setBits(0, dataLen, 0xabcd);
		
		Block expected = Block.create(0x55,tmp,true);
		BigInteger bigInt = Converters.blockToBigInteger(expected, dataLen,indLen);
		Block actual = Converters.toBlock(bigInt, indLen, dataLen,true);
		assertEquals(expected.getId(), actual.getId());
		assertEquals(expected.getData().getNumCols(), actual.getData().getNumCols());
		System.out.printf("expected = %s actual = %s\n", Converters.toHexString(expected.getData()), Converters.toHexString(actual.getData()));
		assertTrue(Arrays.areEqual(expected.getData().getPackedBits(false), actual.getData().getPackedBits(false)));
	}

	@Test
	public void testToBitMatrixBBBB(){
		BigInteger bbbb = BigInteger.valueOf(0xbbbb);
		BitMatrix bmVal = Converters.toBitMatrix(bbbb, 16);
		System.out.println(Converters.toHexString(bmVal) + " " + bbbb.toString(16));
	}
	
	@Test
	public void testToBigInteger(){
		int bitLen = 50;
		BitMatrix actual = new BitMatrix(bitLen);
		actual.setBits(0, bitLen, 0xabcd);
		
		BigInteger expected = BigInteger.valueOf(0xabcd);
		assertEquals(expected.longValue(), actual.getBits(0, 50));
	}
	
	@Test
	public void testToBitMatrix(){
		int bitLen = 32;
		long value = 0xabcd;
		
		BigInteger expected = BigInteger.valueOf(value);		
		BitMatrix bm = new BitMatrix(bitLen);
		bm.setBits(0, bitLen, value);
		
		long actual = bm.getBits(0, bitLen);
		
		assertEquals(expected.longValue(), actual);
		
		System.out.printf("expected = %s actual = %s\n", expected.toString(16), Converters.toHexString(bm.getPackedBits(false)));
		
	}
	
	@Test
	public void testToBitMatrixAndBack(){
		int bitLen = 100;
		BigInteger expected = BigInteger.valueOf(0xabcdef);
		
		BitMatrix bm = Converters.toBitMatrix(expected, bitLen);
		System.out.println(Converters.toHexString(bm));
		
		BigInteger actual = Converters.toBigInteger(bm);
		
		System.out.printf("expected=%s actual=%s \n", expected.toString(16), actual.toString(16));
		
		assertEquals(expected.toString(16), actual.toString(16));
	}
	
	@Test
	public void toBigIntegerAndBack(){
		int bitLen = 50;
		BitMatrix expected = new BitMatrix(bitLen);
		expected.setBits(0, bitLen, 0xabcd);
		
		BigInteger bigInt = Converters.toBigInteger(expected);
		
		BitMatrix actual = Converters.toBitMatrix(bigInt, bitLen);
		
		System.out.printf("expected = %s actual = %s\n", Converters.toHexString(expected.getPackedBits(false)), Converters.toHexString(actual.getPackedBits(false)));
		
		assertEquals(Converters.toHexString(expected.getPackedBits(false)), Converters.toHexString(actual.getPackedBits(false)));
	}
	
}
