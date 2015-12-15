package org.factcenter.inchworm.ops.concrete;

import org.factcenter.inchworm.ops.OpAction;

/**
 * Created by talm on 9/5/14.
 */
public class CallOpTest extends org.factcenter.inchworm.ops.CallOpTest {
    @Override
    protected OpAction getOp(VM vm) throws Exception {
        CallOp callOp = new CallOp();
        callOp.setMoreParameters(otExtenders[vm.playerId]);
        callOp.setParameters(vm.playerId, vm.state, vm.runner, vm.rand);
        callOp.init();
        return callOp;
    }
}
