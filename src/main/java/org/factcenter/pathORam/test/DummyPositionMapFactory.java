package org.factcenter.pathORam.test;

import org.factcenter.pathORam.BlockStorage;
import org.factcenter.pathORam.BlockStoragePositionMapFactory;
import org.factcenter.pathORam.ops.BlockStoragePositionMap;

public class DummyPositionMapFactory implements
		BlockStoragePositionMapFactory {
	@Override
	public BlockStoragePositionMap createPositionMap(BlockStorage blockStorage,
			int valuesInBlock) {
		return new DummyBlockStoragePositionMap(blockStorage, valuesInBlock);
	}
}