package org.factcenter.pathORam.ops.dummy;

import org.factcenter.inchworm.VMRunner;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.VMProtocolPartyInfo;
import org.factcenter.pathORam.Stash;
import org.factcenter.pathORam.StashFactory;

import java.io.IOException;
import java.util.Random;

public class DummyStashFactory extends VMProtocolPartyInfo implements StashFactory {
	
	public DummyStashFactory(int playerId, VMState state, VMRunner runner, Random rand) {
        setParameters(playerId, state, runner, rand);
	}
	
	@Override
	public Stash createStash(int stashCapacity, int pathORamBlocksCount,
			int blockLenBytes) {
		try {
			int stashBlocksCount = stashCapacity;
			DummyStash stash = new DummyStash(blockLenBytes,
					stashBlocksCount, pathORamBlocksCount);
			stash.setParameters(getPlayerId(), getState(), getRunner(), rand);
			stash.init();
			return stash;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		throw new RuntimeException("Error creating stash");
	}
}