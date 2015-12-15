package org.factcenter.inchworm.ops.concrete;

import org.factcenter.inchworm.*;
import org.factcenter.inchworm.ops.dummy.DummyOPFactory;

/**
 * Created by talm on 30/11/15.
 */
public class UnlignedRamTest extends MemoryAreaTest {

    public UnlignedRamTest(int blockSize, int blockCount) {
        super(blockSize, blockCount);
    }

    @Override
    protected MemoryFactory createAndSetupMemoryFactory(int playerId) throws Exception {
        MemoryFactory baseFactory =  new DummyOPFactory();
        UnalignedMemoryFactory unalignedMemoryFactory = new UnalignedMemoryFactory(baseFactory);
        for (MemoryArea.Type memType : MemoryArea.Type.values()) {
            unalignedMemoryFactory.allowUnaligned(memType);
        }
        return unalignedMemoryFactory;
    }

    /**
     * Allow unaligned index but ensure num is power of 2
     */
    @Override
    protected int[] getRandomIndexAndNum() {
        // We only test powers of 2 for numBlocks
        int log2count = 32 - Integer.numberOfLeadingZeros(blockCount) - 1;

        int num = log2count > 1 ? testRand.nextInt(log2count - 1) : 0;
        num = 1 << num;

        int idx = testRand.nextInt(blockCount - num);

        int[] pair = {idx, num};
        return pair;
    }
}
