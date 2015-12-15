package org.factcenter.pathORam;


/**
 * @author mikegarts
 * 
 */
public abstract class BlockStorageBase implements BlockStorage {

	private final int blockSizeBits;
	private final int blockCount;

	/**
	 * @param blockSize
	 *            block size in bytes
	 * @param blockCount
	 *            storage's capacity in blocks
	 */
	public BlockStorageBase(int blockSize, int blockCount) {
		this.blockCount = blockCount;
		this.blockSizeBits = blockSize;
	}

	@Override
	public int getBlockSizeBits() {
		return blockSizeBits;
	}

	@Override
	public int getBlockCount() {
		return blockCount;
	}

}
