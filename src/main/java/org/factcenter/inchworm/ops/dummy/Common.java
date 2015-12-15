package org.factcenter.inchworm.ops.dummy;

import org.factcenter.inchworm.VMRunner;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.VMProtocolParty;
import org.factcenter.inchworm.ops.common.OpInfo;
import org.factcenter.qilin.comm.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Random;

/**
 * Created by talm on 8/20/14.
 */
public class Common implements VMProtocolParty, OpInfo {
    /**
     *  Logger.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected ProtocolInfo info;

    public Common(ProtocolInfo info) {
        this.info = info;
    }

    @Override
    public Channel getChannel() {
        return info.getChannel();
    }

    @Override
    public int getPlayerId() {
        return info.getPlayerId();
    }

    @Override
    public VMRunner getRunner() { return info.getRunner(); }

    @Override
    public void setParameters(int playerId, VMState state, VMRunner runner, Random rand) {
        // Do nothing (should be set on info)
    }

    @Override
    public void init() throws IOException, InterruptedException {
        // Do nothing (should be called on info).
    }
}
