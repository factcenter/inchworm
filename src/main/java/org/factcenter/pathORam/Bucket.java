package org.factcenter.pathORam;

import java.util.List;

/**
 * @author mikegarts
 *
 */
public class Bucket
{
	private final int size;
	private List<Block> blocks;
	
	/**
	 * @return true if the Bucket is full
	 */
	public boolean isFull()
	{
		return getBlocks().size() >= size;
	}
	
	/**
	 * @param size size of the bucket
	 * @param blocks container of blocks 
	 */
	public Bucket(int size, List<Block> blocks)
	{
		this.size = size;
		this.setBlocks(blocks);
	}

	/**
	 * @param dummyBlockSize fill block with dummies of dummyBlockSize each
	 */
	public void fillDummies(int dummyBlockSize) 
	{
		for (int i = getBlocks().size() ; i < size ; i++)
		{
			getBlocks().add(Block.createDummy(dummyBlockSize));
		}
	}

	/**
	 * @return blocks container
	 */
	public List<Block> getBlocks() {
		return blocks;
	}

	private void setBlocks(List<Block> blocks) {
		this.blocks = blocks;
	}
	
}