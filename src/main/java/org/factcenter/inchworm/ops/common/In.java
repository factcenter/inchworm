package org.factcenter.inchworm.ops.common;

import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;

/**
 * Created by talm on 8/25/14.
 */
public class In extends Common implements OpAction {
    public In(OpInfo info) {
        super(info);
    }

    @Override
    public BitMatrix[] doOp(VMState state, BitMatrix... inputs) throws IOException {
        BitMatrix[] outputs = { info.getRunner().getInputFromPlayer() };

        return outputs;
    }
}
