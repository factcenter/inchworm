package org.factcenter.inchworm.ops.concrete;

import org.factcenter.inchworm.MemoryArea;
import org.factcenter.inchworm.MemoryFactory;

/**
 * Created by talm on 30/11/15.
 */
public class UnlignedRamOverFastMuxTest extends FastMuxRamTest {

    protected UnalignedMemoryFactory[] unalignedMemoryFactories = new UnalignedMemoryFactory[NUM_PLAYERS];

    @Override
    protected MemoryFactory createAndSetupMemoryFactory(int playerId) throws Exception {
        MemoryFactory baseFactory =  super.createAndSetupMemoryFactory(playerId);
        UnalignedMemoryFactory unalignedMemoryFactory = new UnalignedMemoryFactory(baseFactory);
        for (MemoryArea.Type memType : MemoryArea.Type.values()) {
            unalignedMemoryFactory.allowUnaligned(memType);
        }
        return unalignedMemoryFactory;
    }

    public UnlignedRamOverFastMuxTest(int blockSize, int blockCount) throws Exception {
        super(blockSize, blockCount);
        cryptoRand = zeroRandom; // Easier debugging; remove for better testing.
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
