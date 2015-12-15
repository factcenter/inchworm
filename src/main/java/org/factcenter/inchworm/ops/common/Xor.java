package org.factcenter.inchworm.ops.common;

import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;

public class Xor extends Common implements OpAction {
    public Xor(OpInfo info) {
        super(info);
    }
	
	/*-
	 *             regs[destRegNdx] = srcReg1Val ^ srcReg2Val
	 * 
	 *   L: sends nothing to R                R: sends s1R,s2R to L     
	 *   L: has now:  s1 = s1R^s1L ,s2 = s2R^s2L
	 *   L: does the xor                      R: does nothing
	 *   L: sends r (0 or random) to R
	 *   L: stores r^result at destRegNdxL    R: stores r at destRegNdxR
	 *   
	 */
    @Override
    public BitMatrix[] doOp(VMState state, BitMatrix... inputs) throws IOException {
        Channel toPeer = info.getChannel();

        BitMatrix aShare = inputs[0];
        BitMatrix bShare = inputs[1];

        BitMatrix result = aShare.clone();
        result.xor(bShare);


        BitMatrix[] outputs = { result };
        return outputs;
    }
}
