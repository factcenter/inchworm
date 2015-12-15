package org.factcenter.inchworm.ops.concrete;


import org.factcenter.inchworm.Converters;
import org.factcenter.inchworm.VMRunner;
import org.factcenter.inchworm.VMState;
import org.factcenter.pathORam.*;
import org.factcenter.pathORam.ops.concrete.ConcreteStash;
import org.factcenter.pathORam.test.BlockStorageTest;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.comm.LocalChannelFactory;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.protocols.generic.DummyOTExtender;
import org.factcenter.qilin.util.BitMatrix;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.categories.Slow;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConcreteStashTest extends BlockStorageTest {

	//final static int REPEAT_COUNT = 10;
	
	/*-
	 * ----------------------------------------------------------------
	 *                  Test data members and setup.
	 * ----------------------------------------------------------------
	 */

	final Logger logger = LoggerFactory.getLogger(getClass());
	
	// Mocked.
	VMRunner runnerStub0, runnerStub1;
	
	VMState stateStub0, stateStub1;

	// Dummy
	OTExtender otExt0, otExt1;
	
	// Real.
	Channel[] channels;
	
	// Use insecure random with a fixed seed for repeatability
	Random rand = MyRandom.getRandom();//new Random(1);
	
	// Left op
	//SSMUXLoad loadOp0;
	ConcreteStash stash0;
	
	// Right op
	//SSMUXLoad loadOp1;
	ConcreteStash stash1;

	private Channel[] yaoChannel;

	private int pathORamSize;

	private int blockSizeBits;
	
	Stash stash;

	private int stashCapacity;

	
	@Before
	public void setUp() {

		// Use mock objects for runner, state and Logger classes.

		runnerStub0 = mock(VMRunner.class);
		runnerStub1 = mock(VMRunner.class);
		stateStub0 = mock(VMState.class);
		stateStub1 = mock(VMState.class);

		LocalChannelFactory channelFactory = new LocalChannelFactory();
		channels = channelFactory.getChannelPair();
		yaoChannel = channelFactory.getChannelPair();

		otExt0 = new DummyOTExtender();
		otExt0.setParameters(channels[0], rand);
		
		otExt1 = new DummyOTExtender();
		otExt1.setParameters(channels[1], rand);

		
		// arrange stubs.
		when(runnerStub0.getChannel()).thenReturn(channels[0]);
		when(runnerStub1.getChannel()).thenReturn(channels[1]);

		when(runnerStub0.getYaoChannel()).thenReturn(yaoChannel[0]);
		when(runnerStub1.getYaoChannel()).thenReturn(yaoChannel[1]);
		
		// Now we can setup the load-op parameters.
		//loadOp0 = new SSMUXLoad();
		//loadOp1 = new SSMUXLoad();
		
		blockSizeBits = 32;
		pathORamSize = 64;
		stashCapacity = TreeHelper.calcStashSize(pathORamSize);
		
		stash0 = new ConcreteStash(blockSizeBits, stashCapacity, pathORamSize);
		stash1 = new ConcreteStash(blockSizeBits, stashCapacity, pathORamSize);
		/*-
		 * NOTE: Currently the ConcreteOPFactory is created by both players in the base class (Player),
		 *       and opFactory.setMoreParameters(otExtender) is called immediately after the creation 
		 *       before the VMRunner is created.
		 *       
		 *       This is the reason we have to call SSMUXLoad.setMoreParameters() first to set the 
		 *       extenderOTStub as the otExtender for the fastMuxSingle.
		 */
		stash0.setMoreParameters(otExt0);
		stash1.setMoreParameters(otExt1);
		
		stash0.setParameters(0, stateStub0, runnerStub0, rand);
		stash1.setParameters(1, stateStub1, runnerStub1, rand);
		
		try {
			stash0.init();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		stash = new Stash() {
			
			@Override
			public void storeBlock(Block block) {
				writeToStash(block);	
			}
			
			@Override
			public int getBlockSizeBits() {
				return blockSizeBits;
			}
			
			@Override
			public int getBlockCount() {
				return stashCapacity;
			}
			
			@Override
			public Block fetchBlock(int blockIndex) {
				return fetchFromStash(blockIndex);
			}

			@Override
			public Map<Integer, Bucket> popBucket(int oldPosition,
												  PositionMap positionMap, int bucketSize) {
				return popBucketImpl(oldPosition, positionMap, bucketSize);
			}

		};
		ram = stash;
	}
	
	@Test
	public void writeTest() throws IOException, InterruptedException{
		byte[] testData0 = { (byte) 0x1, (byte) 0x1, (byte) 0x1, (byte) 0x1};
		int idx0 = 1;
		
		Block block = generateRandomBlock(true);		
		writeToStash(block);
		
		Block result0 = fetchFromStash(block.getId());
		
		System.out.println(result0);
		
	}

	@Ignore
	@Test
	public void testFillAndReadTwice(){}
	
	@Test
    @Category({Slow.class})
	public void testPopBucketManyTimes() {
		for (int i = 0 ; i < 10; i++){
			testPopBucket();
		}	
	}
	
	@Test
	public void testPopBucket() {
		
		ListPositionMap positionMap = new ListPositionMap(pathORamSize);

		Stash referenceStash = new ArrayStashFactory().createStash(stashCapacity, pathORamSize, blockSizeBits);
		
		for (int i = 0 ; i < ram.getBlockCount() ; i++)
		{
			Block block = Block.create(i, getRandomBitMatrix(ram.getBlockSizeBits()), true);
			stash.storeBlock(block);
			referenceStash.storeBlock(block);
		}
		
		int oldPosition = (int) (Math.abs(rand.nextLong()) % pathORamSize);
		int bucketSize = PathORamServer.DEFAULT_PATH_ORAM_BUCKET_SIZE;
				
		for (int i = 0 ; i < 5; i++){
			
			Map<Integer, Bucket> expected = referenceStash.popBucket(oldPosition, positionMap, bucketSize);
			Map<Integer, Bucket> actual = popBucketImpl(oldPosition, positionMap, bucketSize);

			assertEquals(expected.keySet().size(), actual.keySet().size());
			
			for (Entry<Integer, Bucket> entry : expected.entrySet()){
				assertBucketEqual(entry.getValue(), actual.get(entry.getKey()));
			}
		}
	}
	
	public void assertBucketEqual(Bucket expected, Bucket actual) {
		assertEquals(expected.getBlocks().size(), actual.getBlocks().size());
		for (int i = 0 ; i < expected.getBlocks().size() ; i++){
			Block expectedBlock = expected.getBlocks().get(i);
			Block actualBlock = actual.getBlocks().get(i);
						
			System.out.println("expected = " + expectedBlock + " actual = " + actualBlock);
			
			if (expectedBlock.getValidBit()){
				assertEquals(expectedBlock.getId(), actualBlock.getId());
				Assert.assertEquals(Converters.toHexString(expectedBlock.getData()), Converters.toHexString(actualBlock.getData()));
				assertEquals(expectedBlock.getValidBit(), actualBlock.getValidBit());
			}else{
				assertTrue(expectedBlock.getValidBit() == actualBlock.getValidBit());
			}
		}
	};
	
	public Map<Integer, Bucket> popBucketImpl(int oldPosition, PositionMap positionMap, int bucketSize){
		Map<Integer, Bucket> result0 = new HashMap<Integer, Bucket>();
		
		Map<Integer, Bucket> retValMap = new HashMap<Integer, Bucket>();
		
		PositionMap zeroPositionMap = new PositionMap() {
			
			@Override
			public int updatePosition(int oldPositionShare) {
				throw new RuntimeException();
			}
			
			@Override
			public int get(int positionIndexShare) {
				return 0;
			}
		};
		
		for (int i = 0 ; i < stashCapacity ; i++){
			
			BitMatrix data0 = stash0.getBlocks().get(i).getData();
			BitMatrix data1 = stash1.getBlocks().get(i).getData();
			
			boolean valid0 = stash0.getBlocks().get(i).getValidBit();
			boolean valid1 = stash1.getBlocks().get(i).getValidBit();
						
			stash0.getBlocks().set(i, Block.create(i, data0, valid0));
			stash1.getBlocks().set(i, Block.create(0, data1, valid1));
			//stash0.getBlocks().set(i, new StashElement(new Block(i, data0), valid0));
			//stash1.getBlocks().set(i, new StashElement(new Block(0, data1), valid1));
			
		}
		
		try {
			Thread fetchThread = runPopBucket(oldPosition, zeroPositionMap,  bucketSize, result0);
			Map<Integer, Bucket> result1 = stash0.popBucket(oldPosition, positionMap,  bucketSize);
			fetchThread.join();
			
			for (Entry<Integer, Bucket> blockInLevel : result0.entrySet()){
				int level = blockInLevel.getKey();
				List<Block> blocks0 = result0.get(level).getBlocks();
				List<Block> blocks1 = result1.get(level).getBlocks();
				List<Block> blockListInLevel = new LinkedList<Block>();
				for (int i=0 ; i < bucketSize ; ++i){
					Block block0 = blocks0.get(i);
					Block block1 = blocks1.get(i);
					int newId = block0.getId() ^ block1.getId();
					block0.getData().xor(block1.getData());
					BitMatrix data = block0.getData();
					blockListInLevel.add(Block.create(newId, data, block0.getValidBit() ^ block1.getValidBit()));
				}
				retValMap.put(level, new Bucket(bucketSize, blockListInLevel));
			}
			
			return retValMap;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}
	
	Thread runPopBucket(final int oldPosition, final PositionMap positionMap, final int bucketSize, final Map<Integer, Bucket> result) 
			throws IOException, InterruptedException {
			Thread opRunner = new Thread("runPopBucket1") {
				public void run() {
						try {
							stash1.init();
							Map<Integer, Bucket> temp = stash1.popBucket(oldPosition, positionMap, bucketSize);
							result.putAll(temp);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							throw new RuntimeException();
						}
						
				}
			};
			opRunner.start();
			return opRunner;
		}
	
	public Block fetchFromStash(int idx0){
		Block result0 = Block.createDummy(blockSizeBits);
		
		try {
			Thread fetchThread = runFetch1(0, result0);
			Block result1 = stash0.fetchBlock(idx0);
			fetchThread.join();
			result0.getData().xor(result1.getData());
			int id = result0.getId() ^ result1.getId();
			boolean valid = result0.getValidBit() ^ result1.getValidBit();
			return Block.create(id, result0.getData(), valid);
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}

	Thread runFetch1(final int blockIndex, final Block result) 
			throws IOException, InterruptedException {
			Thread opRunner = new Thread("runFetch1") {
				public void run() {
						try {
							stash1.init();
							Block temp = stash1.fetchBlock(blockIndex);
							result.setFields(temp);
						} catch (Exception e) {
							e.printStackTrace();
							throw new RuntimeException();
						}
				}
			};
			opRunner.start();
			return opRunner;
		}

	Thread runStore1(final Block block) 
		throws IOException, InterruptedException {
		Thread opRunner = new Thread("runStore1") {
			public void run() {
				try {
					stash1.init();
					stash1.storeBlock(block);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();}
			}
		};
		opRunner.start();
		return opRunner;
	}


	

	public void writeToStash(Block block) {
		try {
			Block block0 = block;
			Block block1 = Block.create(0, new BitMatrix(blockSizeBits), false);
			Thread t1 = runStore1(block1);
			stash0.storeBlock(block0);
			t1.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
