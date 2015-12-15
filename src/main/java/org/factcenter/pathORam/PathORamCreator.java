package org.factcenter.pathORam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class PathORamCreator {

	/**
	 * Logger.
	 */
	final public Logger logger = LoggerFactory.getLogger(getClass());
	
	private Random random;
	private PositionMapFactory positionMapFactory;
	private StashFactory stashFactory;
	private PathORamDriverFactory pathORamFactory;
	private ServerFactory serverFactory;
	private BlockStoragePositionMapFactory blockStoragePositionMapFactory;

//	public static int BITS_IN_BYTE = 8;

	public PathORamCreator(PositionMapFactory positoinMapFactory,
			StashFactory stashFactory, Random random,
			ServerFactory serverFactory, PathORamDriverFactory pathORamFactory,
			BlockStoragePositionMapFactory blockStoragePositionMapFactory) {
		this.positionMapFactory = positoinMapFactory;
		this.stashFactory = stashFactory;
		this.random = random;
		this.pathORamFactory = pathORamFactory;
		this.serverFactory = serverFactory;
		this.blockStoragePositionMapFactory = blockStoragePositionMapFactory;
	}

//	public int calcPositionMapRatio(int blocksCount, int blockLenghBits) {
//		int addressLengthBits = calcAdressLen(blocksCount);
//		int ratio = blockLenghBits / addressLengthBits;
//		return ratio;
//	}

	public BlockStorage createPathORam(int blocksCount, int blockSizeBits) {
		if (0 == blocksCount || 0 == blockSizeBits) {
			// No ram in this case...
			return null;
		}
		blocksCount = TreeHelper.toPowerOfTwo(blocksCount);
		
		logger.debug("Starting to construct PathORam");

		// The algorithm for construction:
		// 1. Start from the most inner layer and construct a simple PathORam
		// (HashSetPositionMap). 
		// Recursively build layer N+1 on top os layer N (starts with N=0).
		// Each time provide layer N+1 as position map of layer N.
		// note: the innermost layer is actually gets built first (recursion stop)

		BlockStorage bs =  createLayers(0, blocksCount, blockSizeBits);
		
		logger.debug("Finished constructing PathORam");
		return bs;
	}

	public int calcAdressLen(int blocksCount) {
		return TreeHelper.log2ceil(blocksCount);
	}

	private BlockStorage createLayers(int layer, int blocksCount,
			int blockLenghBits) {
				
		//int blockLenBytes = (int) Math.ceil(1.0 * blockLenghBits / BITS_IN_BYTE);
		blocksCount = TreeHelper.toPowerOfTwo(blocksCount);
		int valuesInBlock = TreeHelper.toPowerOfTwo((int) Math.ceil(Math.sqrt(blocksCount)));//calcPositionMapRatio(blocksCount, blockLenghBits);//
		int stashCapacity = TreeHelper.calcStashSize(blocksCount);
		
		logger.trace("PathORamCreator.createLayers: Layer {} blocksCount={} blockLength={} ratio = {} stashCapacity={}\n", layer,
				blocksCount, 
				blockLenghBits, 
				valuesInBlock,
				stashCapacity);
		
		Stash stash = stashFactory.createStash(stashCapacity, blocksCount,
				blockLenghBits);
		PathORamServer server = serverFactory.createPathORamServer(blocksCount,
				blockLenghBits);
				
		PositionMap positionMap = null;

		int innerPathORamBlocksCount = TreeHelper.toPowerOfTwo((int) Math.ceil(1.0 * blocksCount / valuesInBlock)); // N/X
		int innerPathORamBlockLengthBits = valuesInBlock * calcAdressLen(blocksCount); //B = X*logN
		
		if (innerPathORamBlocksCount <= 256 || valuesInBlock == 1) {
		//if (true) {
			// Here create trivial pathORam with simple position map
			logger.trace("Building actual position map: layer = {} blockCount={} blockLenghBits={}\n", layer, blocksCount, blockLenghBits);
			positionMap = positionMapFactory.createPositionMap(blocksCount);
		} else {
			BlockStorage positionMapStorage = createLayers(layer + 1,
					innerPathORamBlocksCount, innerPathORamBlockLengthBits);
			
			positionMap = blockStoragePositionMapFactory.createPositionMap(positionMapStorage, valuesInBlock);			

		}

		BlockStorage pathORam = pathORamFactory.createDriver(server,
				positionMap, stash, random);
		return pathORam;

	}
}
