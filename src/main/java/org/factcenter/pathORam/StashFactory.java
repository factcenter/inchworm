package org.factcenter.pathORam;

public interface StashFactory {
	/**
	 * @param stashCapacity - number of blocks stash will hold
	 * @param pathORamBlocksCount - the corresponding path-o-ram size
	 * @param blockLenBits - data block size in bytes 
	 */
	Stash createStash(int stashCapacity, int pathORamBlocksCount, int blockLenBits);
}