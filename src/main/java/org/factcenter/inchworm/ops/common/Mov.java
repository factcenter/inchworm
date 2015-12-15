package org.factcenter.inchworm.ops.common;

import org.factcenter.inchworm.MemoryArea;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;

public class Mov extends Common implements OpAction {
    public Mov(OpInfo info) {
        super(info);
    }

    /**
     * Copies inputs to outputs. The "smart" parts are all in the {@link MemoryArea} implementation.
     */
    @Override
    public BitMatrix[] doOp(VMState state, BitMatrix... inputs) throws IOException {
        BitMatrix[] outputs = { inputs[0] };
        return outputs;
    }
}
