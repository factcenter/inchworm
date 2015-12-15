package org.factcenter.pathORam;

import org.factcenter.inchworm.Converters;
import org.factcenter.qilin.util.BitMatrix;

/**
 * @author mikegarts
 * 
 *         Represents PathORam's data block
 */
public class Block {
	/**
	 * @param id block's id
	 * @param data block's data
	 * @param isValid Block's valid bit.
	 * @note The values are shares (including the valid bit) when working with inchworm 
	 */
	private Block(int id, BitMatrix data, boolean isValid) {
		this.setData(data);
		this.id = id;
		this.setValid(isValid);
	}

	public static Block create(int id, BitMatrix data, boolean isValid){
		return new Block(id, data, isValid);
	}

	/**
	 * @return block's id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return block's data
	 */
	public BitMatrix getData() {
		return data;
	}

	private void setData(BitMatrix data) {
		this.data = data;
	}

	private BitMatrix data;
	private int id;
	private boolean validBit;

	/**
	 * Creates a dummy block with a given block size
	 * 
	 * @param dummyBlockSizeBits
	 *            size in bits of the dummy block
	 * @return New dummy block
	 */
	public static Block createDummy(int dummyBlockSizeBits) {
		BitMatrix bm = new BitMatrix(dummyBlockSizeBits);
		bm.fillRandom(MyRandom.getRandom());
		return new Block(0, bm, false);
	}

	@Override
	public String toString() {
		return String.format("ID=%d value=%s valid=%b", id, Converters.toHexString(data), validBit);
	}


	public boolean getValidBit() {
		return validBit;
	}


	public void setValid(boolean isValid) {
		this.validBit = isValid;
	}

	public void setFields(Block block2) {
		this.id = block2.id;
		this.validBit = block2.validBit;
		this.data = block2.data;
	}
}
