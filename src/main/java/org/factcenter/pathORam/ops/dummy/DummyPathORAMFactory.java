package org.factcenter.pathORam.ops.dummy;

import org.factcenter.inchworm.VMOpImplementation;
import org.factcenter.inchworm.VMRunner;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.dummy.DummyOPFactory;
import org.factcenter.pathORam.StashFactory;
import org.factcenter.pathORam.ops.GenericPathORAMFactory;

import java.io.IOException;
import java.util.Random;

public class DummyPathORAMFactory extends GenericPathORAMFactory {
    DummyOPFactory opImpl;

	public DummyPathORAMFactory() {
        super(new DummyOPFactory());
        opImpl = new DummyOPFactory();
	}

    @Override
    public StashFactory getStashFactory() {
        return new DummyStashFactory(getPlayerId(), getState(), getRunner(), rand);
    }

    @Override
    public VMOpImplementation getOpImpl() {
        return opImpl;
    }

    @Override
    public void setParameters(int playerId, VMState state, VMRunner runner, Random rand) {
        super.setParameters(playerId, state, runner, rand);
        opImpl.setParameters(playerId, state, runner, rand);
    }

    @Override
    public void init() throws IOException, InterruptedException {
        super.init();
        opImpl.init();
    }
}
