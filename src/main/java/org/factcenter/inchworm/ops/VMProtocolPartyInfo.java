package org.factcenter.inchworm.ops;

import org.factcenter.inchworm.VMRunner;
import org.factcenter.inchworm.VMState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Random;

/**
 * Created by talm on 8/31/14.
 */
public class VMProtocolPartyInfo implements VMProtocolParty {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The randomness source for the ops.
     */
    public Random rand;

    /**
     * VMState of the current player.
     */
    protected VMState state;

    public VMState getState() { return state; }

    /**
     * VMRunner of the current player.
     */
    protected VMRunner runner;

    public VMRunner getRunner() { return runner; }

    /**
     * Player ID.
     */
    int playerId;

    @Override
    public int getPlayerId() {
        return playerId;
    }

    @Override
    public void setParameters(int playerId, VMState state, VMRunner runner, Random rand) {
        this.playerId = playerId;
        this.state = state;
        this.runner = runner;
        this.rand = rand;

    }

    @Override
    public void init() throws IOException, InterruptedException {

    }
}
