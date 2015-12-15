package org.factcenter.pathORam;


public class TreeHelper {

	
	private int treeHeigh;
	private int blocksCount;
	
	public int getTreeHeight(){
		return treeHeigh;
	}
	public TreeHelper(int blocksCount) {
		// according to pathORam paper tree size of ceil(log(N))-1 is enough
		this.treeHeigh = TreeHelper.log2ceil(blocksCount) - 1;
		this.blocksCount = blocksCount;
	}
	
	private int getParentIndex(int currIndex) {
		return (int) (Math.floor((currIndex - 1) / 2));
	}
	
	private void checkPosition(int leafIndex, int level) throws AssertionError {
		if (level > treeHeigh || leafIndex > blocksCount) {
			String message = String.format("level=%d treeHeigh=%d leafIndex=%d blockCount=%d", level, treeHeigh, leafIndex, blocksCount);
			throw new AssertionError("level or blocks count overflow " + message);
		}
	}
	
	/**
	 * Converts Leaf number and level in tree to index of the corresponding
	 * entry in the array representing the tree.
	 * 
	 * Note: Level 0 - is root. For example the function returns 0 for every
	 *       index at level 0
	 * 
	 * @param leafIndex
	 *            leaf number (leftmost is 0)
	 * @param level
	 *            the level (root is level 0, treeHeight is the level of the
	 *            leaves)
	 * @return index in the tree array of the desired node
	 */
	public int toArrayIndex(int leafIndex, int level) {
		checkPosition(leafIndex, level);

		int currIndex = leafIndex + getBucketsCount();
		for (int currLevel = treeHeigh + 1; currLevel > level; currLevel--) {
			currIndex = getParentIndex(currIndex);
		}
		return currIndex;
	}
	
	/**
	 * Return true of both leafIndexOne and leafIndexTwo point to the same
	 * bucket at the given level
	 * 
	 * @param leafIndexOne first leaf index to compare
	 * @param leafIndexTwo second leaf index to compare
	 * @param level the level at which we compare
	 * @return true if leafIndexOne and leafIndex two point to the same bucket
	 *         at level level
	 * Note: For example return true for any leafIndexOne,leafIndexTwo at level 0 (Root's bucket)
	 */
	public boolean isSameBucket(int leafIndexOne, int leafIndexTwo, int level) {
		
		int prefix1 = calcPrefix(leafIndexOne, level);
		int prefix2 = calcPrefix(leafIndexTwo, level);
		
		if (level == 0 && (prefix1!=0 || prefix2!=0)){
			throw new RuntimeException("prefixes not zero");
		}
		
		return (prefix1 == prefix2);
	}
	
	/**
	 * 
	 * @param leafIndex the index of the leaf 
	 * @param level the level on which we want to calculate
	 * @return returns the prefix that represents the "Bucket id" on that level.
	 */
	public int calcPrefix(int leafIndex, int level){
		int bitsToShift = treeHeigh - level + 1;
		return leafIndex >> bitsToShift;
	}
	
	/**
	 * Math helper method
	 * @param number
	 * @return returns log2(ceil(number))
	 */
	public static int log2ceil(double number) {
		int numInt = (int) Math.ceil(number);
		int power =  31 - Integer.numberOfLeadingZeros(numInt);
		if (Math.pow(2, power) == number)
		{
			return power;
		}
		return power+1;
	}

	/**
	 * Calculates the desired stash size. 
	 * @param pathORamBlocksCount - number of blocks in pathORam block storage this stash will serve
	 * @return the desired stash size, including the block that were read 
	 */
	public static int calcStashSize(int pathORamBlocksCount){
		return (int) (TreeHelper.log2ceil(pathORamBlocksCount) * PathORamServer.DEFAULT_PATH_ORAM_BUCKET_SIZE * Stash.STASH_SIZE_FACTOR);
	}

	/**
	 * Calculates the closest power of two that is bigger or equal to the given number
	 */
	public static int toPowerOfTwo(int blocksCount) {
		return (int) Math.pow(2, log2ceil(blocksCount));
	}
	
	public int getBucketsCount() {
		return blocksCount - 1;
	}
	
}
