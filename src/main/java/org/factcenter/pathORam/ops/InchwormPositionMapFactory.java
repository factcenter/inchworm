package org.factcenter.pathORam.ops;

import org.factcenter.inchworm.MemoryArea;
import org.factcenter.inchworm.MemoryFactory;
import org.factcenter.inchworm.VMRunner;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.VMProtocolPartyInfo;
import org.factcenter.pathORam.PositionMap;
import org.factcenter.pathORam.PositionMapFactory;

import java.io.IOException;
import java.util.Random;

public class InchwormPositionMapFactory extends VMProtocolPartyInfo implements PositionMapFactory
{
    MemoryFactory memFactory;

	public InchwormPositionMapFactory(MemoryFactory memFactory, int playerId, VMState state, VMRunner runner, Random rand)
	{
        setParameters(playerId, state, runner, rand);
        this.memFactory = memFactory;
	}

	@Override
	public PositionMap createPositionMap(int entriesCount) {
		try {
            MemoryArea mapRam = memFactory.createNewMemoryArea(MemoryArea.Type.TYPE_RAM);
			InchwormPositionMap positionMap = new InchwormPositionMap(entriesCount, mapRam);
			positionMap.setParameters(getPlayerId(), getState(), getRunner(), rand);
			positionMap.init();
			return positionMap;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		throw new RuntimeException("Error creating positionMap");
	}
}