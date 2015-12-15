package org.factcenter.pathORam.test;

import org.factcenter.pathORam.BlockStorage;
import org.factcenter.pathORam.MyRandom;
import org.factcenter.pathORam.ops.BlockStoragePositionMap;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class BlockStoragePositionMapTest {//extends BlockStorageTest {

	BlockStoragePositionMap pm;
	Random random = MyRandom.getRandom();
	int blockCount;
	int blockSize;
	int valuesInBlock;
	
	@Before
	public void setUp(){
		blockSize = 128;
		blockCount = 1 << 14;
		valuesInBlock = blockSize / 4;
		BlockStorage blockStorage = new HashMapBlockStorage(blockSize, blockCount);
		pm = new DummyBlockStoragePositionMap(blockStorage, valuesInBlock);
		//ram = pm;
	}
	
	int getRandVal(){
		return random.nextInt(pm.getMaxSupportedValue());
	}
	
	int getRandIndex(){
		return random.nextInt(blockCount);
	}

	@Test
	public void testUpdatePosition(){

		for (int i = 0 ; i < blockCount ; i+=50){
		
			for (int j = 0 ; j < 4 ; j++){
				
				pm.updatePosition(i);
				
				int currentPosition = pm.get(i);
				int oldPosition = pm.updatePosition(i);
				assertEquals(currentPosition, oldPosition);
				
				int currentPosition2 = pm.get(i);
				int oldPosition2 = pm.updatePosition(i);
				
				System.out.println("i="+i+" j="+j);
				assertEquals(currentPosition2, oldPosition2);
			}
		}
	}
	
//	@Test
//	public void testGetAndSetOne() {
////		pm.set(15, 15);
////		pm.set(16, 16 % pm.getMaxSupportedValue());//maxSupportedIndex);
////		assertEquals(15, pm.get(15));
////		assertEquals(16 % pm.getMaxSupportedValue() , pm.get(16));
//	}

//	@Test
//	public void testGetAndSet() {
////		for (int i = 0 ; i < pm.getBlockCount() ; ++i)
////		{
////			pm.set(i, i % pm.getMaxSupportedValue());
////		}
////		for (int i = 0 ; i < pm.getBlockCount() ; ++i)
////		{
////			assertEquals(i % pm.getMaxSupportedValue(), pm.get(i));
////		}
//	}

	@Test
	public void testBlocksLessThanByte(){
		blockSize = 128;
		blockCount = 1 << 14;
		valuesInBlock = blockSize  *2;
		testUpdatePosition();
	}

}
