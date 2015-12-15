package org.factcenter.inchworm.ops.concrete;

import org.factcenter.inchworm.ops.OpAction;

/**
 * Created by talm on 9/5/14.
 */
public class RetOpTest extends org.factcenter.inchworm.ops.RetOpTest {
    @Override
    protected OpAction getOp(VM vm) throws Exception {
        RetOp retOp = new RetOp();
        retOp.setMoreParameters(otExtenders[vm.playerId]);
        retOp.setParameters(vm.playerId, vm.state, vm.runner, vm.rand);
        retOp.init();
        return retOp;
    }

}
