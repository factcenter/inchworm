package org.factcenter.pathORam;

/**
 * @author mikegarts
 * 
 *         PathORam server interface
 */
public interface PathORamServer {

	final static int DEFAULT_PATH_ORAM_BUCKET_SIZE = 2;
	
	/**
	 * Read bucket that corresponds to leaf leafIndex at level level
	 * 
	 * @param leafIndex
	 *            the index of the leaf.
	 * @param level
	 *            The level of the node to read. 0 is the root level.
	 * @return The Bucket
	 */
	public Bucket readBucket(int leafIndex, int level);

	/**
	 * Write Bucket bucket to the node that correspond to leaf leafIndex at
	 * level level
	 * 
	 * @param leafIndex
	 *            leafIndex
	 * @param level
	 *            level
	 * @param bucket
	 *            bucket to write
	 */
	public void writeBucket(int leafIndex, int level, Bucket bucket);

	/**
	 * @return number of blocks in storage
	 */
	public int getBlocksCount();

	/**
	 * @return Bucket tree's height
	 */
	public int getTreeHeight();

	/**
	 * @return Number of buckets in three
	 */
	public int getBucketSize();

	/**
	 * @return The of the block in bytes
	 */
	public int getBlockSize();

}