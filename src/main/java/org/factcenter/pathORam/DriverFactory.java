package org.factcenter.pathORam;

import java.util.Random;

public class DriverFactory implements PathORamDriverFactory
{

	@Override
	public PathORamDriver createDriver(PathORamServer server,
			PositionMap positionMap, Stash stash, Random random) {
		return new PathORamDriver(server, random, positionMap, stash);
	}
	
}