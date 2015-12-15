// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.Utils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigInteger;

public class Utils {

	private Utils() {
	}

	/**
	 * 2^64
	 */
	final static BigInteger x64 = BigInteger.ONE.shiftLeft(64);

	/**
	 * Ensure that the passed long parameter will be interpreted as a positive value.
	 * @param x
	 */
	public static final BigInteger getUnsignedBigInteger(long x) {
		BigInteger z = BigInteger.valueOf(x);
		if (z.signum() < 0)
			return z.add(x64);
		else
			return z;
	}
	
	/**
	 * Ensure that the passed long parameter will be interpreted as a positive value.
	 * 
	 * @param x - value.
	 * @param bitWidth - VM word size.
	 * @return a positive value.
	 */	
	public static final BigInteger getUnsignedBigInteger(long x, int bitWidth) {
	   return BigInteger.valueOf(x & (((long) 1 << bitWidth) - 1));
	}

	/*
	 * Write the least significant n bytes of the BigInteger m to os. (Serialization)
	 */
	public static void writeBigInteger(BigInteger m, int n, DataOutput os) throws IOException {
		byte[] temp = new byte[n];
		BigInteger mask = BigInteger.valueOf(0xFF);

		for (int j = 0; j < n; j++) {
			temp[j] = (byte) m.and(mask).intValue();
			m = m.shiftRight(8);
		}

		os.write(temp);
	}

	/*
	 * Read a BigInteger of n bytes from "is", which is written with writeBigInteger.
	 * (Deserialization)
	 */
	public static BigInteger readBigInteger(int n, DataInput dis) throws IOException {
		BigInteger ret = BigInteger.ZERO;

		byte[] temp = new byte[n];
		dis.readFully(temp, 0, n);

		for (int j = n - 1; j >= 0; j--) {
			ret = ret.or(BigInteger.valueOf(0xFF & temp[j]));
			ret = ret.shiftLeft(8);
		}
		ret = ret.shiftRight(8);

		return ret;
	}

	public static void writeBigInteger(BigInteger m, DataOutput oos) throws IOException {
		byte[] bytes = m.toByteArray();
		oos.writeInt(bytes.length);
		oos.write(bytes);
	}

	/*
	 * Read a BigInteger of n bytes from "is", which is written with writeBigInteger.
	 * (Deserialization)
	 */
	public static BigInteger readBigInteger(DataInput ois) throws IOException {
		int length = ois.readInt();
		byte[] bytes = new byte[length];
		ois.readFully(bytes, 0, length);

		return new BigInteger(bytes);
	}

	public static void writeBigIntegerArray(BigInteger[] arr, int n, DataOutput oos)
			throws IOException {
		int length = arr.length;
		oos.writeInt(length);

		for (int i = 0; i < length; i++)
			Utils.writeBigInteger(arr[i], n, oos);
	}

	public static BigInteger[] readBigIntegerArray(int n, DataInput ois) throws IOException {
		int length = ois.readInt();
		BigInteger[] ret = new BigInteger[length];

		for (int i = 0; i < length; i++)
			ret[i] = Utils.readBigInteger(n, ois);

		return ret;
	}
}