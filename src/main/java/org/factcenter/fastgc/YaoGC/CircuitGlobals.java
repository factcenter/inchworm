package org.factcenter.fastgc.YaoGC;

import org.factcenter.fastgc.Cipher.Cipher;

import java.math.BigInteger;
import java.util.Random;

public class CircuitGlobals {
	Random rand;

	/**
	 * Global secret used to compute label negations ("free XOR" trick). If the
	 * 0 value has label <i>x</i>, the 1 value has label {@code (x XOR ((R << 1) | 1))}.
	 */
	public final BigInteger R;

	public final int labelBitLength;

	public int totalWires;
	
	public int totalCircuits;
	
	public Cipher cipher;

	public CircuitGlobals(Random rand, int labelBitLength) {
		this.rand = rand;
		this.labelBitLength = labelBitLength;
		this.cipher = new Cipher();
		R = new BigInteger(labelBitLength - 1, rand);
		totalWires = 0;
		totalCircuits = 0;
	}
}
