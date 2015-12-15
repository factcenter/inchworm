package org.factcenter.pathORam;

public class ListPositionMapFactory implements PositionMapFactory
{
	@Override
	public PositionMap createPositionMap(int entriesCount) {
		return new ListPositionMap(entriesCount);
	}
	
}