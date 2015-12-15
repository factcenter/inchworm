package org.factcenter.pathORam;

import java.util.Map;

/**
 * @author mikegarts
 * Interface of stash data structure of the pathORam algorithm.
 *
 * Note: in the methods of BlockStorage - every input and return value is a shared value!
 */
public interface Stash extends BlockStorage
{
	/**
	 * Removes from stash the path to be written back to the pathOram's tree.
	 * Each block contains up to bucketSize blocks packed in a bucket according to the PathORam constraint.
	 * The PathORam constraint means that the P(oldPosition,level) = P(positionMap[index in stash],l)
	 * Where P(x,l) means the bucket at level l along the path to node x in the tree
	 * 
	 * @param oldPosition - oldPosition, not a share
	 * @param positionMap - a position map to check the positions of the elements in stash
	 * @param bucketSize - bucket size as declared in the PathORam algorithm (not a share)
	 * Note:  The blocks in the returned bucket are removed from stash
	 */
	public Map<Integer,Bucket> popBucket(int oldPosition, PositionMap positionMap, int bucketSize );
	
	public static double STASH_SIZE_FACTOR = 2;
}