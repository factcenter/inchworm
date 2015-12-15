// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.Cipher;

import java.math.BigInteger;
import java.security.MessageDigest;

public final class Cipher {
	private static final int unitLength = 160; // SHA-1 has 160-bit output.

	private static final BigInteger mask = BigInteger.ONE.shiftLeft(80)
			.subtract(BigInteger.ONE);

	/**
	 * Instance of sha1 message digest. Note that this can't be 
	 * a static variable because it isn't thread-safe.
	 */
	private MessageDigest sha1;

	public Cipher() {
		try {
			sha1 = MessageDigest.getInstance("SHA-1");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Compute an Yao Garbling, assuming H is a random oracle.   
	 * @param keyL 
	 * @param keyR
	 * @param wireSerialNo should be unique for every wire.
	 * @param plaintext
	 * @return H(keyL, keyR, wireSerialNo) XOR plaintext
	 */
	public BigInteger encrypt(BigInteger keyL, BigInteger keyR, int wireSerialNo,
			BigInteger plaintext) {
		BigInteger ret = getPadding(keyL, keyR, wireSerialNo);
		ret = ret.xor(plaintext);

		return ret;
	}

	public BigInteger decrypt(BigInteger keyL, BigInteger keyR, int wireSerialNo,
			BigInteger ciphertext) {
		BigInteger ret = getPadding(keyL, keyR, wireSerialNo);
		ret = ret.xor(ciphertext);

		return ret;
	}

	// this padding generation function is dedicated for encrypting garbled
	// tables.
	private BigInteger getPadding(BigInteger keyL, BigInteger keyR, int wireSerialNo) {
		sha1.update(keyL.toByteArray());
		sha1.update(keyR.toByteArray());
		sha1.update(BigInteger.valueOf(wireSerialNo).toByteArray());
		return (new BigInteger(sha1.digest())).and(mask);
	}

	public BigInteger encrypt(BigInteger key, BigInteger msg,
			int msgLength) {
		return msg.xor(getPaddingOfLength(key, msgLength));
	}

	public BigInteger decrypt(BigInteger key, BigInteger cph,
			int cphLength) {
		return cph.xor(getPaddingOfLength(key, cphLength));
	}

	private BigInteger getPaddingOfLength(BigInteger key, int padLength) {
		sha1.update(key.toByteArray());
		BigInteger pad = BigInteger.ZERO;
		byte[] tmp = new byte[unitLength / 8];
		for (int i = 0; i < padLength / unitLength; i++) {
			System.arraycopy(sha1.digest(), 0, tmp, 0, unitLength / 8);
			pad = pad.shiftLeft(unitLength).xor(new BigInteger(1, tmp));
			sha1.update(tmp);
		}
		System.arraycopy(sha1.digest(), 0, tmp, 0, unitLength / 8);
		pad = pad.shiftLeft(padLength % unitLength).xor(
				(new BigInteger(1, tmp)).shiftRight(unitLength
						- (padLength % unitLength)));
		return pad;
	}

	public BigInteger encrypt(int j, BigInteger key, BigInteger msg,
			int msgLength) {
		return msg.xor(getPaddingOfLength(j, key, msgLength));
	}

	public BigInteger decrypt(int j, BigInteger key, BigInteger cph,
			int cphLength) {
		return cph.xor(getPaddingOfLength(j, key, cphLength));
	}

	private BigInteger getPaddingOfLength(int j, BigInteger key,
			int padLength) {
		sha1.update(BigInteger.valueOf(j).toByteArray());
		sha1.update(key.toByteArray());
		BigInteger pad = BigInteger.ZERO;
		byte[] tmp = new byte[unitLength / 8];
		for (int i = 0; i < padLength / unitLength; i++) {
			System.arraycopy(sha1.digest(), 0, tmp, 0, unitLength / 8);
			pad = pad.shiftLeft(unitLength).xor(new BigInteger(1, tmp));
			sha1.update(tmp);
		}
		System.arraycopy(sha1.digest(), 0, tmp, 0, unitLength / 8);
		pad = pad.shiftLeft(padLength % unitLength).xor(
				(new BigInteger(1, tmp)).shiftRight(unitLength
						- (padLength % unitLength)));
		return pad;
	}
}