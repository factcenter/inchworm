package org.factcenter.inchworm.ops.concrete;

import org.factcenter.inchworm.*;

public class FastMuxRamTest extends MemoryAreaTest {

    public final static int NUM_PLAYERS = 2;


    @Override
    protected MemoryFactory createAndSetupMemoryFactory(int playerId) throws Exception {
        FastMuxMemoryFactory factory = new FastMuxMemoryFactory();

        factory.setMoreParameters(otExtenders[playerId]);
        return factory;
    }

    public FastMuxRamTest(int blockSize, int blockCount) throws Exception {
        super(blockSize, blockCount);
    }

    @Override
    protected int[] getRandomIndexAndNum() {
        // We only test powers of 2
        int log2count = 32 - Integer.numberOfLeadingZeros(blockCount) - 1;

        int num = log2count > 1 ? testRand.nextInt(log2count - 1) : 0;
        num = 1 << num;

        int idx = testRand.nextInt(blockCount);
        idx &= ~(num - 1);

        int[] pair = {idx, num};
        return pair;
    }

}