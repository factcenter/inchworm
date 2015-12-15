package org.factcenter.inchworm.ops.dummy;

import org.factcenter.inchworm.ops.DivOpTest;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.inchworm.ops.OpDefaults;

/**
 * Created by talm on 9/5/14.
 */
public class DummyDivTest extends DivOpTest {
    @Override
    protected OpAction getOp(VM vm) throws Exception {
        return vm.factory.getOpAction(OpDefaults.Op.OP_DIV.name);
    }
}
