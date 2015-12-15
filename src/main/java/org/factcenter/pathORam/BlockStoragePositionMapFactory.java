package org.factcenter.pathORam;

import org.factcenter.pathORam.ops.BlockStoragePositionMap;

public interface BlockStoragePositionMapFactory {
	BlockStoragePositionMap createPositionMap(BlockStorage blockStorage, int valuesInBlock);
}
