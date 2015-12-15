package org.factcenter.pathORam;

import org.factcenter.inchworm.Converters;
import org.factcenter.qilin.util.BitMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PathORamDriver extends BlockStorageBase {

	/**
	 * Objects the implements the PathORam "remote" storage. It's the external
	 * memory access to which seem random for external viewer
	 */
	protected final PathORamServer server;

	/**
	 * Source of randomness
	 */
	protected Random random;

	protected PositionMap positionMap;

	protected Stash stash;
	
	protected Logger logger;

	public PathORamDriver(PathORamServer server, Random random,
			PositionMap positionMap, Stash stash) {
		super(server.getBlockSize(), server.getBlocksCount());
		this.server = server;
		this.random = random;
		this.positionMap = positionMap;
		this.stash = stash;
		this.logger = LoggerFactory.getLogger(getClass());
		
	}

	protected enum Method {
		READ, WRITE;
	}

	@Override
	public void storeBlock(Block block) {
		checkInput(block.getId(), block.getData());
		access(Method.WRITE, block);
	}

	/**
	 * Asserts that the giver input is in the bounds of the underlying storage
	 */
	public void checkInput(int blockIndex, BitMatrix blockData) {
		if (blockData.getNumCols() != getBlockSizeBits()){
			throw new IllegalArgumentException("Wrong blockSize. Expected " + getBlockSizeBits() + " got " + blockData.getNumCols());
		}
		if (blockIndex < 0 || blockIndex > getBlockCount()) {
			throw new IllegalArgumentException("Wrong index " + blockIndex);
		}
	}

	@Override
	public Block fetchBlock(int blockIndex) {
		checkInput(blockIndex, new BitMatrix(getBlockSizeBits()));
		// just passing block index to access. a bit ugly but allows to write the code as close the 
		// pseudo code in the paper as possible
		return access(Method.READ, Block.create(blockIndex, null, false));
	}

	/**
	 * The access method as described in the paper
	 * 
	 * @param operation
	 *            READ/WRITE opcode
	 * @param block
	 *            Block to write / read. (data will be null if operation is READ)
	 * @return The data read
	 * Note: One should use the exposed store/fetch methods
	 */
	public Block access(Method operation, Block block) {

		int index = block.getId();
		BitMatrix data = block.getData();
		logger.debug("PathORamDriver.access: operationd = {} index={} data={} pathORamSize={} blockSize={}", 
				operation.toString(), 
				index,
				null != data ? Converters.toHexString(data.getPackedBits(false)) : "null",
				server.getBlocksCount(),
				server.getBlockSize());
		
		// lines 1-2 from the paper. This might be a recursive call to a smaller PathORam
		int oldPosition = positionMap.updatePosition(index);

		logger.debug("PathORamDriver.access oldPosition={} index={} pathORamSize={}", oldPosition, index, server.getBlocksCount());
		
		Block readData = null;

		// lines 3-5
		List<Block> allBlocks = readPathToRoot(oldPosition);
		fillStash(allBlocks);

		// lines 6-9
		readData = stash.fetchBlock(index);
		if (Method.WRITE == operation) {
			logger.debug("PathORam.access going to store in stash our block (WRITE)");
			stash.storeBlock(block);
		}

		logger.debug("PathORamDriver.access before popping buckets " + stash.getBlockCount());
		
		// lines 10-15
		Map<Integer, Bucket> path = stash.popBucket(oldPosition, positionMap, server.getBucketSize());
		for (int level : path.keySet()){
			server.writeBucket(oldPosition, level, path.get(level));
		}

		logger.debug("PathORamDriver.access after popping buckets " + stash.getBlockCount());
		
		// line 16
		return readData;
	}

	/**
	 * Read all blocks that reside on the path from node number oldPosition to
	 * the root of the tree
	 */
	public List<Block> readPathToRoot(int oldPosition) {
		List<Block> allBlockInPath = new LinkedList<Block>();
		for (int level = server.getTreeHeight(); level >= 0; level--) {
			Bucket bucket = server.readBucket(oldPosition, level);
			for (Block block : bucket.getBlocks()) {
				//if (block.getValidBit()) {
					allBlockInPath.add(block);
				//}
			}
		}
		return allBlockInPath;
	}

//	/**  
//	 * @return if the block is dummy
//	 */
//	public boolean isBlockDummy(Block block) {
//		return Block.isDummyId(block.getId());
//	}

	/**
	 * This method fills the implemented in subclass stash with the blocks read
	 * from storage
	 */
	public void fillStash(List<Block> allBlocks) {
		for (Block block : allBlocks){
			stash.storeBlock(block);
		}
	}

}