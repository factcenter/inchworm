package org.factcenter.pathORam;

public class LocalServerFactory implements ServerFactory
{

	private int bucketSize;
	
	public LocalServerFactory(int bucketSize)
	{
		this.bucketSize = bucketSize;
	}

	@Override
	public PathORamServer createPathORamServer(int blocksCount, int blockSize) {
		return new LocalServer(blocksCount, blockSize, bucketSize);
	}
	
}