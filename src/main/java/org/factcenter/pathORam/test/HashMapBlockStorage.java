package org.factcenter.pathORam.test;

import org.factcenter.pathORam.Block;
import org.factcenter.pathORam.BlockStorageBase;
import org.factcenter.pathORam.MyRandom;
import org.factcenter.qilin.util.BitMatrix;

import java.util.HashMap;

public class HashMapBlockStorage extends BlockStorageBase {

	public HashMapBlockStorage(int blockSize, int blockCount) {
		super(blockSize, blockCount);
		for (int i = 0 ; i < blockCount ; ++i)
		{
			BitMatrix bits = new BitMatrix(blockSize);
			bits.fillRandom(MyRandom.getRandom());
			Block dummy = Block.create(i, bits, false);
 			storeBlock(dummy);
		}
	}

	HashMap<Integer,Block> map = new HashMap<Integer,Block>();

	@Override
	public void storeBlock(Block block) {
		map.put(block.getId(), block);
	}

	@Override
	public Block fetchBlock(int blockIndex) {
		return map.get(blockIndex);
	}
}