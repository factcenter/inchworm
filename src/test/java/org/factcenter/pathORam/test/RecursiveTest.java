package org.factcenter.pathORam.test;

import org.factcenter.pathORam.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertNotNull;

/**
 * @author mikegarts
 *
 */
public class RecursiveTest extends BlockStorageTest {

	/**
	 * Initialize the local server
	 */
	@Before
	@Override
	public void setUp() {
		int ramWordLen = 16;
		int blocksCount = (int) Math.pow(2, ramWordLen);//2 << 12;
		int bucketSize = PathORamServer.DEFAULT_PATH_ORAM_BUCKET_SIZE;
		int blockSize = ramWordLen; 
		ram = RecursiveTest.makeCreator(bucketSize).createPathORam(blocksCount, blockSize);	
	}	
	
	public static PathORamCreator makeCreator(int BUCKET_SIZE) {
		PositionMapFactory positoinMapFactory = new ListPositionMapFactory ();
		StashFactory stashFactory = new ArrayStashFactory();
		Random random = MyRandom.getRandom();
		ServerFactory serverFactory = new LocalServerFactory(BUCKET_SIZE);
		PathORamDriverFactory pathORamFactory = new DriverFactory();
		return new PathORamCreator(positoinMapFactory, stashFactory, random, serverFactory, pathORamFactory,
				new DummyPositionMapFactory());
	}
	
	@Test
	public void testCreatePathORam() {
		PathORamCreator creator;
		int BUCKET_SIZE = PathORamServer.DEFAULT_PATH_ORAM_BUCKET_SIZE;
		creator = makeCreator(BUCKET_SIZE);
		int blockLenghBits = 16 * 8; 
		int blocksCount = (int) Math.pow(2, 8);
		BlockStorage bs = creator.createPathORam(blocksCount, blockLenghBits);
		assertNotNull(bs);
	}
}
