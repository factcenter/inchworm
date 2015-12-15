package org.factcenter.inchworm.ops;

/**
 * Created by talm on 9/5/14.
 */
public class DummyRetOpTest extends RetOpTest {
    @Override
    protected OpAction getOp(VM vm) throws Exception {
        return vm.factory.getOpAction("return");
    }
}
