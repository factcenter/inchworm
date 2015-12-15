package org.factcenter.inchworm.ops.dummy;

import org.factcenter.inchworm.*;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.comm.LocalChannelFactory;
import org.factcenter.qilin.util.BitMatrix;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class DummyRamTest extends MemoryAreaTest {

    final static int NUM_PLAYERS = 2;

    @Override
    protected MemoryFactory createAndSetupMemoryFactory(int playerId) throws Exception {
        return new DummyOPFactory();
    }

    public DummyRamTest(int blockSize, int blockCount) throws Exception {
        super(blockSize, blockCount);
    }



}