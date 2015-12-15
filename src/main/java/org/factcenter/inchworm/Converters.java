package org.factcenter.inchworm;

import org.factcenter.pathORam.Block;
import org.factcenter.qilin.util.BitMatrix;

import java.math.BigInteger;

public class Converters {
//	/**
//	 * Helper method. Converts from BigInteger bits sequence to byte[] in a sign safe way
//	 */
//	static public byte[] toByteArray(BigInteger bigInteger, int arrayLenInBits)
//	{
//		int resLen = arrayLenInBits/8;
//		BigInteger byteMask = BigInteger.valueOf(0xff);
//		BigInteger copyNum = bigInteger;
//		if (arrayLenInBits % 8 != 0)
//		{
//			throw new IllegalArgumentException("Only byte alligned arrays supported");
//		}
//		byte[] result = new byte[resLen];
//		for (int i = 0 ; i < resLen ; ++i){
//			result[resLen- i - 1] = (byte)copyNum.and(byteMask).longValue();
//			copyNum = copyNum.shiftRight(8);
//		}
//		return result;
////		int dataLenBytes = arrayLenInBits/8;
////		byte[] data = bigInteger.toByteArray();
////		if (data.length > dataLenBytes)
////		{
////			byte[] newData = new byte[dataLenBytes];
////			for (int i = 0 ; i < dataLenBytes ; i++)
////			{
////				newData[i] = data[i+1];
////			}
////			return newData;
////		}
////		return data;
//	}
	
	/**
	 * Converts from BigInteger bit sequence that represents block to a Block type.
	 * Inverse of {@link #blockToBigInteger(Block, int,int)}
	 */
	static public Block toBlock(BigInteger entryBits, int indexLenBits, int dataLenBits, boolean validBit )
	{
		BigInteger INDEX_MASK = BigInteger.ONE.shiftLeft(indexLenBits)
				.subtract(BigInteger.ONE);
		BigInteger DATA_MASK = BigInteger.ONE.shiftLeft(dataLenBits)
				.subtract(BigInteger.ONE);

		BigInteger dataInt = entryBits.and(DATA_MASK);
		BigInteger indexInt = entryBits.shiftRight(dataLenBits).and(INDEX_MASK);
		int id = (int)indexInt.longValue();
		BitMatrix data = toBitMatrix(dataInt, dataLenBits);
		
		if (data.getNumCols() != dataLenBits){
			throw new RuntimeException("Convert to block failed");
		}
		
		return Block.create(id, data, validBit);
	}
	
	static public BitMatrix toBitMatrix(BigInteger bigInt, int bitMatrixLen){
		BitMatrix result = new BitMatrix(bitMatrixLen);
		BigInteger currNum = bigInt;
		for (int i = 0 ; i < bitMatrixLen ; ++i){
			result.setBit(bitMatrixLen - i - 1, (int)currNum.and(BigInteger.ONE).longValue());
			currNum = currNum.shiftRight(1);
		}
		return result;
	}
	
	public static BigInteger toBigInteger(BitMatrix data){
		BigInteger result = BigInteger.ZERO;
		for (int i = 0 ; i < data.getNumCols() ; ++i){
			result = result.shiftLeft(1);
			BigInteger newBit = 0 == data.getBit(i) ? BigInteger.ZERO : BigInteger.ONE;
			result = result.or(newBit);
		}

		return result;
	}
	
	/**
	 * Converts Block type to Bits sequence (BigInteger). Inverse of {@link #toBlock(BigInteger, int, int, boolean)}
	 */
	public static BigInteger blockToBigInteger(Block currentBlock, int dataLenBits, int indexLenBits) {
		BigInteger dataMask = BigInteger.ONE.shiftLeft(dataLenBits).subtract(BigInteger.ONE);
		BigInteger indexMask = BigInteger.ONE.shiftLeft(indexLenBits).subtract(BigInteger.ONE);
		BigInteger entryId = BigInteger.valueOf(currentBlock.getId()).and(indexMask);
		BigInteger entryData = toBigInteger(currentBlock.getData()).and(dataMask);
		BigInteger entry = entryId.shiftLeft(dataLenBits).or(entryData);
		return entry;
	}
	
	/**
	 * Helper method to avoid java's annoying signed byte. Converts byte to BigInteger in sign safe way
	 */
//	public static BigInteger toBigInt(byte[] bytes){
//		// try using new BigInteger(1 (magnitude), bytes )
//		BigInteger res = BigInteger.ZERO;
//		for (byte b : bytes){
//			BigInteger newByte = BigInteger.valueOf(b & 0xFF);
//			res = res.shiftLeft(8).or(newByte);
//		}
//		return res;
//	}
	

	
//	public static BitMatrix toBitMatrix(byte[] bytes, int bitMatrixLength){
//		BigInteger tmpInt = new BigInteger(1,bytes);
//		BitMatrix res = new BitMatrix(bitMatrixLength);
//		for (int i = 0 ; i < bitMatrixLength; ++i){
//			long currBit = tmpInt.and(BigInteger.ONE).longValue();
//			tmpInt = tmpInt.shiftRight(1);
//			res.setBit(i, (int)currBit);
//		}
//		return res;
//	}
	
	public static byte[] toBytes(BitMatrix bm){
		return bm.getPackedBits(true);
	}
	
	public static String toHexString(byte[] bytes){
		return new BigInteger(1, bytes).toString(16);
	}
	
	public static String toHexString(BitMatrix bytes){
		return new BigInteger(1, bytes.getPackedBits(false)).toString(16);
	}
}
