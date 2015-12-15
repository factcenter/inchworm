package org.factcenter.pathORam;


/**
 * @author mikegarts
 * 
 *         Block storage high level interface
 */
public interface BlockStorage {

	/**
	 * @return Storage's block size
	 */
	public int getBlockSizeBits();

	/**
	 * @return Number of blocks in the storage
	 */
	public int getBlockCount();

	/**
	 * Stores Block block in the storage. Previous data stored is discarded
	 * 
	 * @param block The block to store
	 */
	public void storeBlock(Block block);

	/**
	 * @param blockIndex
	 *            index
	 * @return Block stored under index blockIndex
	 */
	public Block fetchBlock(int blockIndex);

}