package org.factcenter.inchworm.ops.concrete;

import org.factcenter.inchworm.ops.OpAction;

public class DivOpTest extends org.factcenter.inchworm.ops.DivOpTest {
    @Override
    protected OpAction getOp(VM vm) throws Exception {
        DivOp divOp = new DivOp();
        divOp.setMoreParameters(otExtenders[vm.playerId]);
        divOp.setParameters(vm.playerId, vm.state, vm.runner, vm.rand);
        divOp.init();
        return divOp;
    }
}
