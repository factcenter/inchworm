package org.factcenter.pathORam.ops.dummy;

import org.factcenter.inchworm.ops.VMProtocolPartyInfo;
import org.factcenter.pathORam.Block;
import org.factcenter.pathORam.Bucket;
import org.factcenter.pathORam.PositionMap;
import org.factcenter.pathORam.TreeHelper;
import org.factcenter.pathORam.ops.InchwormStash;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DummyStash extends VMProtocolPartyInfo implements InchwormStash {

	private int stashBlockSize;
	private int pathORamBlocksCount;
	private int stashSize;
	private TreeHelper treeHelper;
	private Block[] stash;

	public DummyStash(int blockSize, int stashSize, int pathORamSize)
	{
		this.stashBlockSize = blockSize;
		this.pathORamBlocksCount = pathORamSize;
		this.stashSize = stashSize;
		this.treeHelper = new TreeHelper(pathORamBlocksCount);
		stash = new Block[stashSize];
	}
		
	private int toRealValueExchange(int valueShare)  {
		try {
			getRunner().getChannel().writeInt(valueShare);
            getRunner().getChannel().flush();
			int realIndex = getRunner().getChannel().readInt() ^ valueShare;
			return realIndex;
		} catch (IOException e) {
			throw new RuntimeException();
		}

	}

	public Bucket popBucket(int oldPosition, int level,
							PositionMap positionMap, int bucketSize) {
		

		List<Block> legalToStoreBlocks = new LinkedList<Block>();
		for (int i=0 ; i < stash.length;  i++) {
			if (stash[i] == null){
				continue;
			}
			Block currentBlock = stash[i]; 
			int entryPositionShare = positionMap.get(currentBlock.getId());
			int entryPosition = toRealValueExchange(entryPositionShare);
			logger.debug("in popBucket on index {}. oldPosition={} entryPosition={} level={}", i, oldPosition, entryPosition, level);
			if (treeHelper.isSameBucket(oldPosition, entryPosition, level)) {
				legalToStoreBlocks.add(currentBlock);
				stash[i] = null;
				if (legalToStoreBlocks.size() >= bucketSize) {
					break;
				}
			}
		}

		Bucket bucket = new Bucket(bucketSize, legalToStoreBlocks);
		bucket.fillDummies(stashBlockSize);
		return 	bucket;
	}

	@Override
	public int getBlockSizeBits() {
		return stashBlockSize;
	}

	@Override
	public int getBlockCount() {
		return stashSize;
	}

	@Override
	public void storeBlock(Block block) {
		int blockIndexShare = block.getId();
		int index = toRealValueExchange(blockIndexShare);
		if (0 == toRealValueExchange(block.getValidBit() == true ? 1 : 0)){
			return;
		}
		
		for (int i = 0; i < stash.length; i++) {
			if (stash[i]==null || toRealValueExchange(stash[i].getId()) == index){
				stash[i] = block;
				logger.debug("added on index {}", i);
				return;
			}
		}
		throw new RuntimeException("stash size overflow size,capacity=" + stash.length + " ," + stash.length );
	}

	@Override
	public Block fetchBlock(int blockIndexShare) {
		int index = toRealValueExchange(blockIndexShare);
		for (int i = 0; i < stash.length; i++) {
			if (stash[i] != null && toRealValueExchange(stash[i].getId()) == index){
				return stash[i];
			}
		}
		return null;
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
