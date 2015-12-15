package org.factcenter.pathORam;

import java.util.Random;

public interface PathORamDriverFactory {
	PathORamDriver createDriver(PathORamServer server, PositionMap positionMap, Stash stash,
			Random random);
}