package org.factcenter.pathORam;

public class ArrayStashFactory implements StashFactory
{
	@Override
	public Stash createStash(int stashCapacity, int pathORamBlocksCount,
			int blockLenBits) {
		TreeHelper treeHelper = new TreeHelper(pathORamBlocksCount);
		return new ArrayStash(blockLenBits, stashCapacity ,treeHelper);	
	}
}