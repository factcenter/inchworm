package org.factcenter.pathORam;

public interface ServerFactory 
{
	PathORamServer createPathORamServer(int blocksCount, int blockSize);
}