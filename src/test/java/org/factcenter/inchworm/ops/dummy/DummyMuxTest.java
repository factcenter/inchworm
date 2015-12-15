package org.factcenter.inchworm.ops.dummy;

import org.factcenter.inchworm.ops.MuxOpTest;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.inchworm.ops.OpDefaults;

/**
 * Created by talm on 9/21/14.
 */
public class DummyMuxTest extends MuxOpTest
{
    @Override
    protected OpAction getOp(VM vm) throws Exception {
        return vm.factory.getOpAction(OpDefaults.Op.OP_MUX.name);
    }
}
