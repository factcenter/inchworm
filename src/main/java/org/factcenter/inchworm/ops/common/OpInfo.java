package org.factcenter.inchworm.ops.common;

import org.factcenter.inchworm.VMRunner;
import org.factcenter.qilin.comm.Channel;

/**
 * Information required by most OpAction.
 */
public interface OpInfo {
    /**
     * Channel to the peer.
     */
    public Channel getChannel();

    /**
     * The Id of the current player.
     */
    public int getPlayerId();


    /**
     * The player's IO Handler.
     * @return
     */
    public VMRunner getRunner();
}
