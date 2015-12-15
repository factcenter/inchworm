package org.factcenter.inchworm;

import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.inchworm.ops.VMProtocolParty;

/**
 * Interface defining the current set of op implementations the player VM executes.
 */
public interface VMOpImplementation extends VMProtocolParty {
    /**
     * Return the action corresponding to a particular op.
     */
    public OpAction getOpAction(String opName);

}
