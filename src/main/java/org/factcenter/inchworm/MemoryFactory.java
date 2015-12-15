package org.factcenter.inchworm;

import org.factcenter.inchworm.ops.VMProtocolParty;

/**
 * Created by talm on 8/30/14.
 */
public interface MemoryFactory extends VMProtocolParty {
    /**
     * Create a new memory area of the specified type.
     * @param memArea
     * @return
     */
    public MemoryArea createNewMemoryArea(MemoryArea.Type memArea);
}
