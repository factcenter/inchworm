package org.factcenter.pathORam.ops.dummy;

import org.factcenter.inchworm.*;
import org.factcenter.inchworm.ops.dummy.DummyOPFactory;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.comm.LocalChannelFactory;
import org.factcenter.qilin.protocols.generic.DummyOTExtender;
import org.factcenter.qilin.util.BitMatrix;
import org.junit.Ignore;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Created by talm on 9/6/14.
 */
@Ignore
public class DummyPathORAMTest extends MemoryAreaTest {

    // {blockSize,blockCount} parameters for testing.
    @Parameterized.Parameters
    public static Collection<Integer[]> memSizes() {
        return Arrays.asList(new Integer[][]{
                {32, 4}
        });
    }

    @Override
    protected MemoryFactory createAndSetupMemoryFactory(int playerId) throws Exception {
        DummyPathORAMFactory factory = new DummyPathORAMFactory();
        factory.setMoreParameters(otExtenders[playerId]);
        return factory;
    }

    public DummyPathORAMTest(final int blockSize, final int blockCount) throws Exception {
        super(blockSize, blockCount);
    }

}
