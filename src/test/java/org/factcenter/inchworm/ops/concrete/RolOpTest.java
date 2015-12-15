package org.factcenter.inchworm.ops.concrete;

import org.factcenter.inchworm.ops.OpAction;

/**
 * Created by talm on 09/12/15.
 */
public class RolOpTest extends org.factcenter.inchworm.ops.RolOpTest {
    @Override
    protected OpAction getOp(VM vm) throws Exception {
        RolOp rolOp = new RolOp();
        rolOp.setMoreParameters(otExtenders[vm.playerId]);
        rolOp.setParameters(vm.playerId, vm.state, vm.runner, vm.rand);
        rolOp.init();
        return rolOp;
    }
}
