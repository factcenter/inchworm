package org.factcenter.inchworm.ops.concrete;

import org.factcenter.inchworm.*;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

public class CircuitMuxRamTest extends MemoryAreaTest {


    // {blockSize,blockCount} parameters for testing.
    @Parameterized.Parameters
    public static Collection<Integer[]> memSizes() {
        return Arrays.asList(new Integer[][]{
                {8, 16},
        });
    }

    @Override
    protected MemoryFactory createAndSetupMemoryFactory(int playerId) throws Exception {
        CircuitMuxMemoryFactory circuitMuxMemoryFactory = new CircuitMuxMemoryFactory();
        circuitMuxMemoryFactory.setMoreParameters(otExtenders[playerId]);
        return circuitMuxMemoryFactory;
    }


    public CircuitMuxRamTest(int blockSize, int blockCount) throws Exception {
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