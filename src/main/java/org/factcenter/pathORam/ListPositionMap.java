package org.factcenter.pathORam;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ListPositionMap implements PositionMap
{
	private List<Integer> positionMap ;
	private Random random;
	private int blockCount;
	
	public ListPositionMap(int blockCount)
	{
		System.out.println("ListpositionMap.ctor creating with size = " + blockCount);
		this.blockCount = blockCount;
		positionMap = new ArrayList<Integer>(blockCount);
		for (int i = 0; i < blockCount; i++) {
			positionMap.add(i);
		}
		java.util.Collections.shuffle(positionMap);
		
		random = MyRandom.getRandom();
	}

	@Override
	public int get(int position) {
		return positionMap.get(position);
	}

	//@Override
	public void set(int position, int newValue) {
		positionMap.set(position, newValue);
	}

	@Override
	public int updatePosition(int index) {
		int oldPosition = this.get(index);
		int newPosition = random.nextInt(blockCount) % blockCount;
		positionMap.set(index, newPosition);
		return oldPosition;
	}
}