package org.factcenter.inchworm.ops.dummy;

import org.factcenter.inchworm.ops.GenericOpTest;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.inchworm.ops.OpDefaults;
import org.factcenter.inchworm.ops.RolOpTest;

/**
 * Created by talm on 09/12/15.
 */
public class DummyRolOpTest extends RolOpTest {
    @Override
    protected OpAction getOp(VM vm) throws Exception {
        return vm.factory.getOpAction(OpDefaults.Op.OP_ROL.name);
    }
}
