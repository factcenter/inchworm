package org.factcenter.pathORam;

/**
 * @author mikegarts
 * Position map interface.
 * PathORamDriver uses it to store the mapping between block indices and the leaf indices (in the tree that represents the pathORam) 
 */
public interface PositionMap
{
	/**
	 * Get the position stored under the given index
	 * @return the position stored under positionIndexShare (a shared value according to the algorithm)
	 */
	int get(int positionIndexShare);
	
	/**
	 * Associates the position positionIndex with value newValue
	 */
//	void set(int positionIndexShare, int newValueShare);
	
	/**
	 * Calculates a new random position for index. Updates the position map with a new value. 
	 * 
	 * @param oldPositionShare
	 *            the position (share) to update in positionMap
	 * @return Returns the old value from position map (not share)
	 * Note: Position map is updated as a side effect of this method
	 */
	int updatePosition(int oldPositionShare);
	
}