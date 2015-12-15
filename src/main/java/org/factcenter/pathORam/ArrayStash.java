package org.factcenter.pathORam;

import org.factcenter.qilin.util.BitMatrix;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ArrayStash implements Stash
{
	//private Map<Integer, BitMatrix> stash = new HashMap<Integer, BitMatrix>();
	private Block[] stash;
	private int blockSize;
	private TreeHelper treeHelper;
	private int stashMaxCapacity;
	
	public ArrayStash(int blockSize, int stashSize, TreeHelper treeHelper) {
		System.out.println("Creating HashMapStash blockSize = " + blockSize + " stashSize = " + stashSize);
		this.stashMaxCapacity = stashSize;
		this.blockSize = blockSize;
		this.treeHelper = treeHelper;
		stash = new Block[stashMaxCapacity];
	}

	@Override
	public Block fetchBlock(int index) {
		for (int i = 0; i < stash.length; i++) {
			if (stash[i] != null && stash[i].getId()==index){
				return stash[i];
			}
		}
		return null;
	}

	@Override
	public void storeBlock(Block block) {
		if (!block.getValidBit()){
			return;
		}
		int index = block.getId();
		BitMatrix data = block.getData();
		boolean validBit = block.getValidBit();
		for (int i = 0; i < stash.length; i++) {
			if (stash[i]==null || stash[i].getId()==index){
				stash[i] = Block.create(index, data, validBit);
				return;
			}
		}
		throw new RuntimeException("stash size overflow size,capacity=" + stash.length + " ," + stashMaxCapacity );
	}

	@Override
	public int getBlockSizeBits() {
		return blockSize;
	}
		

	@Override
	public int getBlockCount() {
		return stashMaxCapacity;
	}

	
	
	
	public Bucket popBucket(int oldPosition, int level,
			PositionMap positionMap, int bucketSize) {
		
		List<Block> legalToStoreBlocks = new LinkedList<Block>();
		for (int i=0 ; i < stash.length;  i++) {
			if (stash[i] == null){
				continue;
			}
			Block currentBlock = stash[i]; 
			int entryPosition = positionMap.get(currentBlock.getId());
			if (treeHelper.isSameBucket(oldPosition, entryPosition, level)) {
				legalToStoreBlocks.add(currentBlock);
				stash[i] = null;
				if (legalToStoreBlocks.size() >= bucketSize) {
					break;
				}
			}
		}

		Bucket bucket = new Bucket(bucketSize, legalToStoreBlocks);
		bucket.fillDummies(blockSize);
		return 	bucket;
	}

	@Override
	public Map<Integer, Bucket> popBucket(int oldPosition,
			PositionMap positionMap, int bucketSize) {
		Map<Integer,Bucket> path = new HashMap<Integer, Bucket>();
		for (int level = treeHelper.getTreeHeight(); level >= 0; level--) {
			Bucket bucket = popBucket(oldPosition, level, positionMap, bucketSize);
			path.put(level, bucket);
		}
		return path;
	}
}