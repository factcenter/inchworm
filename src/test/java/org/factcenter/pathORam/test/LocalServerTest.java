package org.factcenter.pathORam.test;

import org.factcenter.pathORam.Block;
import org.factcenter.pathORam.Bucket;
import org.factcenter.pathORam.LocalServer;
import org.factcenter.pathORam.PathORamServer;
import org.factcenter.qilin.util.BitMatrix;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author mikegarts
 *
 */
public class LocalServerTest {

	
	private int blocksCount = 128;
	private int bucketSize = 4;
	private int blockSize = 128;
		
	private PathORamServer server;
	
	/**
	 * Initialize the server variable before each test
	 */
	@Before
	public void init() {
		server = new LocalServer(blocksCount, blockSize, bucketSize);
		blocksCount = server.getBlocksCount();
	}
	
	/**
	 * Test the LocalPathORam ctor
	 */
	@Test
	public void testLocalPathORamServer() {
		// test the ctor in @Before
	}

	/**
	 * Test the read bucket function
	 */
	@Test
	public void testReadBucket() {
		Bucket b = server.readBucket(50, 2);
		assertEquals(bucketSize, b.getBlocks().size()); 
	}
	
	/**
	 * Test the tree construction by checking the height
	 */
	@Test
	public void testgetTreeHeight()
	{
		assertEquals(6, server.getTreeHeight());
	}

	/**
	 * Test calling the write method
	 */
	@Test
	public void testCallToWriteBucket() {
		
		server.writeBucket(50, 2, makeBucket(123));
	}

	/**
	 * Naively tests the write method and asserts the result
	 */
	@Test
	public void testSimpleWriteRead()
	{
		Bucket b = makeBucket(666);
		server.writeBucket(17, 2, b);
		Bucket newBucket = server.readBucket(17, 2);
		assertEquals(b, newBucket);
	}
	
	/**
	 * Test writing to index zero level zero
	 */
	@Test
	public void testSimpleWriteReadZeroZero()
	{
		Bucket b = makeBucket(666);
		server.writeBucket(0, 0, b);
		Bucket newBucket = server.readBucket(0, 0);
		assertEquals(b, newBucket);
	}
	
	/**
	 * Writes and reads from different levels
	 */
	@Test
	public void testWriteReadLevels()
	{
		for (int i = 0 ; i < server.getTreeHeight() ; i++)
		{
			server.writeBucket(i, i, makeBucket(i));
		}
		for (int i = 0; i < server.getTreeHeight() ; i++)
		{
			Bucket bucket = server.readBucket(i, i);
			for (Block block : bucket.getBlocks())
			{
				assertEquals(i, block.getId());
			}
		}
	}
	
	/**
	 * Fail test wring bucket size
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testAssertBadBucketSizeFail()
	{
		Bucket b = new Bucket(bucketSize-1, new ArrayList<Block>());
		b.fillDummies(blockSize);
		server.writeBucket(0, 0, b);
	}
	
	/**
	 * Fail test bad block size
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testAssertBadBlockSizeFail()
	{
		Bucket b = new Bucket(bucketSize, new ArrayList<Block>());
		for (int i=0; i < b.getBlocks().size(); ++i)
		{
			b.getBlocks().set(i, Block.create(0,new BitMatrix(blockSize), true));
		}
		server.writeBucket(0, 0, b);
	}


	
	private Bucket makeBucket(int globalIndex) {
		List<Block> blocks = new ArrayList<Block>();
		for (int blockInBucket = 0 ; blockInBucket < server.getBucketSize(); blockInBucket++)
		{
			blocks.add(Block.create(globalIndex, new BitMatrix(blockSize),true));
			
		}
		Bucket bucket = new Bucket(server.getBucketSize(), blocks);
		return bucket;
	}
}
