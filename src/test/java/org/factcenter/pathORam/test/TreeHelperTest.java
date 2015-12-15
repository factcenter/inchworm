package org.factcenter.pathORam.test;

import org.factcenter.pathORam.TreeHelper;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TreeHelperTest {

	/**
	 * Tests the to array index
	 */
	@Test
	public void testToArrayIndex()
	{
		int blocksCount = 128;
		TreeHelper treeHelper = new TreeHelper(blocksCount);
		for (int i = 0 ; i < blocksCount; i++)
		{
			int LEVEL = 0; 
			// index of everybody is 0
			assertEquals("error level 0 index: " + i, 0, treeHelper.toArrayIndex(i, LEVEL));
		}
		
		// level 1
		{
			int LEVEL = 1;
			for (int i = 0 ; i < (blocksCount -1) / 2; i++)
			{
				// left side is 1,
				int INDEX_ONE = 1 ;
				assertEquals("error level 1 index: " + i, INDEX_ONE, treeHelper.toArrayIndex(i, LEVEL));
			}
			for (int i = (blocksCount - 1) / 2 + 1 ; i < blocksCount ; i++)
			{
				// right side is 2
				int INDEX_TWO = 2;
				assertEquals("error level 1 index: " + i, INDEX_TWO, treeHelper.toArrayIndex(i, LEVEL));
			}
		}
	}

	@Test
	public void testLog2ceil() {
		assertEquals(2, TreeHelper.log2ceil(4));
		assertEquals(3, TreeHelper.log2ceil(5));
		assertEquals(4, TreeHelper.log2ceil(16));
		assertEquals(5, TreeHelper.log2ceil(17));
	}


	@Test
	@Ignore
	public void testTreeHeigh(){
		TreeHelper th = new TreeHelper(8);
		assertEquals(3,th.getTreeHeight());
	}
	
	@Test
	public void testToArrayFull4(){
		TreeHelper th = new TreeHelper(4); // heigh 1
		assertEquals(2, th.toArrayIndex(3, th.getTreeHeight()));
		assertEquals(2, th.toArrayIndex(2, th.getTreeHeight()));
		assertEquals(1, th.toArrayIndex(1, th.getTreeHeight()));
		assertEquals(1, th.toArrayIndex(0, th.getTreeHeight()));
		
		assertEquals(0, th.toArrayIndex(3, th.getTreeHeight() - 1));
		assertEquals(0, th.toArrayIndex(2, th.getTreeHeight() - 1));
		assertEquals(0, th.toArrayIndex(1, th.getTreeHeight() - 1));
		assertEquals(0, th.toArrayIndex(0, th.getTreeHeight() - 1));
	}
	
	@Test
	public void testToArrayFull8(){
		TreeHelper th = new TreeHelper(8); // heigh 1
		assertEquals(6, th.toArrayIndex(7, th.getTreeHeight()));
		assertEquals(6, th.toArrayIndex(6, th.getTreeHeight()));
		assertEquals(5, th.toArrayIndex(5, th.getTreeHeight()));
		assertEquals(5, th.toArrayIndex(4, th.getTreeHeight()));
		assertEquals(4, th.toArrayIndex(3, th.getTreeHeight()));
		assertEquals(4, th.toArrayIndex(2, th.getTreeHeight()));
		assertEquals(3, th.toArrayIndex(1, th.getTreeHeight()));
		assertEquals(3, th.toArrayIndex(0, th.getTreeHeight()));
		
		assertEquals(2, th.toArrayIndex(7, th.getTreeHeight() - 1));
		assertEquals(2, th.toArrayIndex(6, th.getTreeHeight() - 1));
		assertEquals(2, th.toArrayIndex(5, th.getTreeHeight() - 1));
		assertEquals(2, th.toArrayIndex(4, th.getTreeHeight() - 1));
		assertEquals(1, th.toArrayIndex(3, th.getTreeHeight() - 1));
		assertEquals(1, th.toArrayIndex(2, th.getTreeHeight() - 1));
		assertEquals(1, th.toArrayIndex(1, th.getTreeHeight() - 1));
		assertEquals(1, th.toArrayIndex(0, th.getTreeHeight() - 1));
		
		assertEquals(0, th.toArrayIndex(7, th.getTreeHeight() - 2));
		assertEquals(0, th.toArrayIndex(6, th.getTreeHeight() - 2));
		assertEquals(0, th.toArrayIndex(5, th.getTreeHeight() - 2));
		assertEquals(0, th.toArrayIndex(4, th.getTreeHeight() - 2));
		assertEquals(0, th.toArrayIndex(3, th.getTreeHeight() - 2));
		assertEquals(0, th.toArrayIndex(2, th.getTreeHeight() - 2));
		assertEquals(0, th.toArrayIndex(1, th.getTreeHeight() - 2));
		assertEquals(0, th.toArrayIndex(0, th.getTreeHeight() - 2));
		
	}
	
	@Test
	public void testConcreteIsSameBucketCalculation(){
		// Exhausting, all inputs test of the "isSameBucket" calculation performed in concrete stash 

		int blockCount = 1 << 8;
		TreeHelper th = new TreeHelper(blockCount);
		
		for (int level = th.getTreeHeight() ; level >= 0; level--){
			for (int leafIndexOne = blockCount-1 ; leafIndexOne >=0 ; leafIndexOne--){
				for (int leafIndexTwo = 0 ; leafIndexTwo <= blockCount/2 ; leafIndexTwo++){
		
					int levelShift = th.getTreeHeight() - level + 1;
					
					boolean referenceVal = th.isSameBucket(leafIndexOne, leafIndexTwo, level);
					boolean concreteStashVal = (leafIndexOne >> levelShift) == (leafIndexTwo >> levelShift);
					
					System.out.println("leafIndexOne = " + leafIndexOne + " leafIndexTwo = " + leafIndexTwo + " level = " + level);
					
					assertEquals(referenceVal, concreteStashVal);
				}
			}
		}
	}
}
