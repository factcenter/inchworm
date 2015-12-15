package org.factcenter.pathORam;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mikegarts
 * 
 *         The tree stored in array of buckets. Much like described in
 *         http://en.wikipedia.org/wiki/Binary_tree#Arrays
 */
public class LocalServer implements PathORamServer {
	private Bucket[] bucketsTree;
	private final int bucketsCount;
	private final int blocksCount;
	private final int treeHeigh;
	private final int bucketSize;
	private final int blockSize;
	
	private TreeHelper treeHelper;
	
	/**
	 * @param blocksCount
	 *            Number of blocks in the PathORam storage
	 * @param blockSize
	 *            Block size
	 * @param bucketSize
	 *            Bucket size
	 */
	public LocalServer(int blocksCount, int blockSize, int bucketSize) {
		this.blocksCount = TreeHelper.toPowerOfTwo(blocksCount);
		this.bucketSize = bucketSize;
		this.blockSize = blockSize;
		System.out.println("LocalServer.ctor blocksCount= " + blocksCount);
		treeHelper = new TreeHelper(this.blocksCount);
		treeHeigh = treeHelper.getTreeHeight();
		bucketsCount = treeHelper.getBucketsCount(); //nodesAmountFromBlocks();
		bucketsTree = new Bucket[bucketsCount];
		for (int i = 0; i < bucketsCount; i++) {
			bucketsTree[i] = makeDummyBucket();
		}
	}

	@Override
	public Bucket readBucket(int leafIndex, int level) {
		checkPosition(leafIndex, level);
		return bucketsTree[treeHelper.toArrayIndex(leafIndex, level)];
	}

	@Override
	public void writeBucket(int leafIndex, int level, Bucket bucket) {
		checkBucket(bucket);
		checkPosition(leafIndex, level);
		bucketsTree[treeHelper.toArrayIndex(leafIndex, level)] = bucket;
	}

	@Override
	public int getBlocksCount() {
		return blocksCount;
	}

	@Override
	public int getTreeHeight() {
		return treeHeigh;
	}

	@Override
	public int getBucketSize() {
		return bucketSize;
	}

	private Bucket makeDummyBucket() {
		List<Block> blocks = new ArrayList<Block>(bucketSize);
		Bucket bucket = new Bucket(bucketSize, blocks);
		bucket.fillDummies(blockSize);
		return bucket;
	}

	@Override
	public int getBlockSize() {
		return this.blockSize;
	}

//	private int nodesAmountFromBlocks() {
//		if (1==this.blocksCount)
//		{
//			return 1;
//		}
//		return 2 * (this.blocksCount / 2) - 1;
//	}

	private void checkBucket(Bucket bucket) throws AssertionError {
		if (bucket.getBlocks().size() != bucketSize) {
			throw new IllegalArgumentException("Bucket size error");
		}
		for (Block b : bucket.getBlocks()) {
			if (b.getData().getNumCols() != blockSize) {
				throw new IllegalArgumentException("Block size error");
			}
		}
	}

	private void checkPosition(int leafIndex, int level) throws AssertionError {
		if (level > treeHeigh || leafIndex > blocksCount) {
			throw new AssertionError("level or blocks count overflow " + level + " " + leafIndex);
		}
	}

}
