package org.factcenter.pathORam.test;

import org.factcenter.pathORam.BlockStorage;
import org.factcenter.pathORam.ops.BlockStoragePositionMap;
import org.factcenter.qilin.util.BitMatrix;

public class DummyBlockStoragePositionMap extends
		BlockStoragePositionMap {
	public DummyBlockStoragePositionMap(BlockStorage blockStorage,
			int valuesInBlock) {
		super(blockStorage, valuesInBlock);
	}

	@Override
	public BitMatrix load(int blockBitLength, long idxShare, BitMatrix memShare) throws java.io.IOException {
		idxShare = idxShare * blockBitLength;
		return memShare.getSubMatrixCols((int)idxShare, blockBitLength).clone();
	}

	@Override
	public BitMatrix store(BitMatrix valueShare, int blockLen, long idxShare, BitMatrix memShare) throws java.io.IOException {
		long offsetInMemShare = idxShare * blockLen;
		for (int i = 0 ; i < blockLen ; ++i){
			int bitToSet = valueShare.getBit(i);
			memShare.setBit((int) (offsetInMemShare + i), bitToSet);			
		}
		
		return memShare;
	}

	@Override
	public int unshareInt(int oldPositionShare) {
		return oldPositionShare;
	}

	@Override
	public boolean getValidBit() {
		return true;
	}
}