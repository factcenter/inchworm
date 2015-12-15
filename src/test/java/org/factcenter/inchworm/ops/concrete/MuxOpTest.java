package org.factcenter.inchworm.ops.concrete;

import org.factcenter.inchworm.ops.OpAction;

public class MuxOpTest extends org.factcenter.inchworm.ops.MuxOpTest {

    @Override
    protected OpAction getOp(VM vm) throws Exception {
        MuxOp muxOp = new MuxOp();
        muxOp.setMoreParameters(otExtenders[vm.playerId]);
        muxOp.setParameters(vm.playerId, vm.state, vm.runner, vm.rand);
        muxOp.init();
        return muxOp;
    }
}

