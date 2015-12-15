package org.factcenter.pathORam.test;

import org.factcenter.inchworm.Converters;
import org.factcenter.pathORam.Block;
import org.factcenter.pathORam.BlockStorage;
import org.factcenter.pathORam.MyRandom;
import org.factcenter.qilin.util.BitMatrix;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import test.categories.Slow;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author mikegarts
 *
 */
public class BlockStorageTest {
	
	public BlockStorage ram;
	private Random random = MyRandom.getRandom();
	
	@Before
	public void setUp() {
		int blockCount = (int) Math.pow(2, 16);
		int blockSize = 128;
		ram = new HashMapBlockStorage(blockSize, blockCount);
	}

	/**
	 * Call the storeBlock method
	 */
	@Test
	public void testStoreBlock() {
		ram.storeBlock(generateRandomBlock(true));
	}

	
	/**
	 * Tests reading block that was never written (expects null)
	 */
	@Test
	public void testFetchBlock() {
		ram.fetchBlock(0);
	}

	/**
	 * Stores, fetches and asserts one block
	 */
	@Test
	public void testStoreAndFetch()
	{
		BitMatrix data = generateDataBlock((byte)'b', ram.getBlockSizeBits());
		Block block = Block.create(0, data, true);
		ram.storeBlock(block);
		Block actual = ram.fetchBlock(0);
		assertEquals(0, actual.getId());
		assertEquals(Converters.toHexString(data), Converters.toHexString(actual.getData()));
	}
	
	/**
	 * Fills the storage, then reads all written and asserts
	 */
	@Test
	public void testFillAndRead()
	{
		Map<Integer,Block> stored = new HashMap<Integer,Block>();
		for (int i = 0 ; i < ram.getBlockCount() ; i += Math.max(ram.getBlockCount()/1000,1))
		{
			Block block = Block.create(i, getRandomBitMatrix(ram.getBlockSizeBits()), true);

			stored.put(block.getId(), block);
			System.out.printf("writing i = %d\n",i);
			ram.storeBlock(block);
			Block actual = ram.fetchBlock(block.getId());
			assertTrue(areEqual(block, actual));
		}
		for (int i = 0 ; i < ram.getBlockCount() ; i += Math.max(ram.getBlockCount()/1000,1))
		{
			System.out.printf("reading i = %d\n",i);
			assertTrue(areEqual(stored.get(i), ram.fetchBlock(i)));
		}
	}
	
	@Test
    @Category({Slow.class})
    public void testStressRandom(){
		Map<Integer,Block> stored = new HashMap<Integer,Block>();
		for (int i = 0 ; i < ram.getBlockCount() ; i += Math.max(ram.getBlockCount()/10000,1))
		{
			Block block = generateRandomBlock(true);
			
			stored.put(block.getId(),block);
			System.out.printf("testing i = %d\n",i);
			ram.storeBlock(block);
			assertTrue(areEqual(block, ram.fetchBlock(block.getId())));
		}
	}
	
	/**
	 * Runs the testFillAndRead twice
	 */
	@Test
	public void testFillAndReadTwice()
	{
		testFillAndRead();
		testFillAndRead();
	}
	
	public BitMatrix generateDataBlock(long b, int size)
	{
		BitMatrix newBm = new BitMatrix(size);
		newBm.setBits(0, size, b);
		return newBm;
	}
	
	public BitMatrix getRandomBitMatrix(int size){
		BitMatrix bm = new BitMatrix(size);
		bm.fillRandom(random);
		return bm;
	}
	
	public Block generateRandomBlock(boolean validBit){
		return Block.create(random.nextInt(ram.getBlockCount()), getRandomBitMatrix(ram.getBlockSizeBits()), validBit);
	}
	
	public boolean areEqual(Block b1, Block b2){
		if (b1.getId() == b2.getId()){
			return Converters.toHexString(b1.getData()).equals(Converters.toHexString(b2.getData()));
		}
		return false;
	}
	
}
