package org.factcenter.inchworm.ops.dummy;

import org.factcenter.inchworm.ops.CallOpTest;
import org.factcenter.inchworm.ops.OpAction;

/**
 * Created by talm on 9/5/14.
 */
public class DummyCallOpTest extends CallOpTest {

    @Override
    protected OpAction getOp(VM vm) {
        return vm.factory.getOpAction("call");
    }
}
