package org.factcenter.inchworm.ops;

import org.factcenter.inchworm.VMState;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;

/**
 * Created by talm on 8/18/14.
 */
public interface OpAction {
    /**
     * Perform an action.
     * This is usually a secure computation between the parties. The return values will be written
     * according to the corresponding {@link OpDesc}.
     *
     * @param state
     * @param inputs
     * @return
     */
    public BitMatrix[] doOp(VMState state, BitMatrix... inputs) throws IOException;
}
