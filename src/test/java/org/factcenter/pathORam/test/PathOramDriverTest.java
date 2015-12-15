package org.factcenter.pathORam.test;

import org.factcenter.pathORam.*;
import org.junit.Before;

public class PathOramDriverTest extends BlockStorageTest {

	@Before
	@Override
	public void setUp() {
		
		int blocksCount = (int) Math.pow(2, 16);
		int blockSize = 32;
		int bucketSize = PathORamServer.DEFAULT_PATH_ORAM_BUCKET_SIZE;
		PathORamServer server = new LocalServer(blocksCount, blockSize, bucketSize);
		blocksCount = server.getBlocksCount();
		PositionMap positionMap = new ListPositionMap(blocksCount);
		TreeHelper treeHelper = new TreeHelper(blocksCount);
		Stash stash = new ArrayStash(blockSize, blocksCount, treeHelper);
		ram = new PathORamDriver(server,MyRandom.getRandom(), positionMap, stash);
		
	}


}
