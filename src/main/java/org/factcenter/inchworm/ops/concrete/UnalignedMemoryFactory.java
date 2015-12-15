package org.factcenter.inchworm.ops.concrete;

import org.factcenter.inchworm.*;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.inchworm.ops.OpDefaults;
import org.factcenter.qilin.comm.SendableInput;
import org.factcenter.qilin.comm.SendableOutput;
import org.factcenter.qilin.util.BitMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * A MemoryFactory that wraps an arbitrary memory factory and allows access that is not aligned on a "multiblock"
 * This
 */
public class UnalignedMemoryFactory implements MemoryFactory {
    final Logger logger = LoggerFactory.getLogger(getClass());

    MemoryFactory alignedMemoryFactory;

    VMState state;

    VMRunner runner;

    /**
     * Add op used to increment offset.
     * TODO: For better efficiency, we could use a different-width addop for every size;
     * for now we use word-size add.
     */
    OpAction addOp;

    /**
     * VM Wordsize (needed for addOp).
     */
    int addOpWordSize;

    /**
     * Set of memory types that will be wrapped to allow unaligned access.
     */
    Set<MemoryArea.Type> unalignedAreaTypes;

    /**
     * Default constructor. This constructor does not specify an underlying aligned
     * {@link MemoryFactory}, so {@link #createNewMemoryArea(MemoryArea.Type)} may not be called
     * on the instance. However, {@link UnalignedRAM} objects can still be manually created using its constructor.
     * Note that {@link #init()} and {@link #setParameters(int, VMState, VMRunner, Random)} must still be called.
     */
    public UnalignedMemoryFactory() {
        unalignedAreaTypes = EnumSet.noneOf(MemoryArea.Type.class);
        this.alignedMemoryFactory = null;
    }


    /**
     * Construct an UnalignedMemoryFactory based on an underlying aligned {@link MemoryFactory}/
     * @param alignedMemoryFactory the underlying aligned factory.
     */
    public UnalignedMemoryFactory(MemoryFactory alignedMemoryFactory) {
        this();
        this.alignedMemoryFactory = alignedMemoryFactory;
    }

    /**
     * Register a memory type as allowing unaligned access.
     * @param memType
     */
    public void allowUnaligned(MemoryArea.Type memType) {
        unalignedAreaTypes.add(memType);
    }

    @Override
    public MemoryArea createNewMemoryArea(MemoryArea.Type memAreaType) {
        MemoryArea baseMem = alignedMemoryFactory.createNewMemoryArea(memAreaType);
        if (unalignedAreaTypes.contains(memAreaType)) {
            return new UnalignedRAM(baseMem);
        }
        return baseMem;
    }

    @Override
    public int getPlayerId() {
        return runner.getPlayerId();
    }

    @Override
    public void setParameters(int playerId, VMState state, VMRunner runner, Random rand) {
        this.state = state;
        this.runner = runner;
        this.addOpWordSize = state.getWordSize();
        addOp = runner.getOpImpl().getOpAction(OpDefaults.Op.OP_ADD.name);
        if (alignedMemoryFactory != null)
            alignedMemoryFactory.setParameters(playerId, state, runner, rand);
    }

    @Override
    public void init() throws IOException, InterruptedException {
        if (alignedMemoryFactory != null)
            alignedMemoryFactory.init();
    }


    /**
     * Wrapped memory that allows unaligned accesses to underlying memory
     *
     */
    public class UnalignedRAM implements MemoryArea {
        /**
         * Wrapped MemoryArea that only allows aligned access
         */
        protected MemoryArea alignedMemory;

        /**
         * Staging memory areas for performing actual unaligned access
         */
        protected Map<Integer,MemoryArea> stagingAreas;

        public UnalignedRAM(MemoryArea alignedMemory) {
            this.alignedMemory = alignedMemory;
            stagingAreas = new HashMap<>();
        }

        /**
         * Setup a read/write size (in blocks) for which to allow unaligned access.
         * This method can be used to manually set up an UnalignedRAM without giving the factory
         * access to an underlying aligned {@link MemoryFactory}.
         * Reads/writes for sizes that were not registered will be implicitly aligned (by truncating low-order bits).
         * @param numBlocks reads/writes of exactly this many blocks can be unaligned to the multiblock boundary.
         * @param newStagingArea new Staging area
         */
        public void setupUnalignedForSize(int numBlocks, MemoryArea newStagingArea) {
            if ( Integer.bitCount(numBlocks) != 1) {
                logger.error("We currently support oblivious load only if " +
                        "the number of blocks is a power of 2 ({} is not))", numBlocks);
                throw new UnsupportedArgException(String.format("We currently support oblivious load only if " +
                        "the number of blocks is a power of 2 (%d is not)", numBlocks));
            }

            newStagingArea.init(getBlockSize(), numBlocks * 2);
            stagingAreas.put(numBlocks, newStagingArea);
        }

        /**
         * Setup a read/write size (in blocks) for which to allow unaligned access.
         * Reads/writes for sizes that were not registered will be implicitly aligned (by truncating low-order bits).
         * @param numBlocks reads/writes of exactly this many blocks can be unaligned to the multiblock boundary.
         */
        public MemoryArea setupUnalignedForSize(int numBlocks) {
            MemoryArea staging = stagingAreas.get(numBlocks);
            if (staging != null)
                return staging;

            staging = alignedMemoryFactory.createNewMemoryArea(Type.TYPE_RAM);
            setupUnalignedForSize(numBlocks, staging);
            return staging;
        }


        @Override
        public int getBlockSize() { return alignedMemory.getBlockSize();  }

        @Override
        public int getBlockCount() { return alignedMemory.getBlockCount();  }


        /**
         * Return a share of the number 1.
         * @return a share of 1. The actual value of the share depends on the player ID.
         */
        final int getShareOfOne() { return getPlayerId() == 0 ? 1 : 0; }

        /**
         * Load a multiblock into a staging area for further processing
         * @param staging the staging area to load into (should hold 2*numBlocks blocks)
         * @param indexShare the share of the index from which to read (may be unaligned)
         * @param numBlocks the number of blocks in the multiblock.
         * @return An array containing (0) The offset within the staging area from which reads should start, (1) the
         *      "masked index share" (aligned to a power-of-two), (2) incremented maskedIndexShare (location of second multiblock)).
         * @throws IOException
         */
        final BitMatrix[] loadIntoStagingArea(MemoryArea staging, BitMatrix indexShare, int numBlocks) throws IOException {

            int log2 = Integer.numberOfTrailingZeros(numBlocks);

            long maskedIndexSharePrimitive = indexShare.toInteger(64) & ~((1 << log2) - 1);
            BitMatrix maskedIndexShare = BitMatrix.valueOf(maskedIndexSharePrimitive, addOpWordSize);

            BitMatrix offsetShare = BitMatrix.valueOf(indexShare.getSubMatrixCols(0, log2).toInteger(), addOpWordSize);

            // ==== Load first multiblock (at (index & mask)) ====
            BitMatrix stagingBlocksShare = alignedMemory.loadOblivious(maskedIndexShare, numBlocks);
            staging.store(0, stagingBlocksShare);

            // ==== Load second multiblock (at ((index >> log2) + 1) << log2) ====
            // Share of value used to increment the maskedIndexShare's multiblock number.
            BitMatrix multiBlockIncShare = BitMatrix.valueOf(getShareOfOne() << log2, addOpWordSize);
            BitMatrix nextMaskedIndexShare = addOp.doOp(state, maskedIndexShare, multiBlockIncShare)[0];

            stagingBlocksShare = alignedMemory.loadOblivious(nextMaskedIndexShare, numBlocks);
            staging.store(numBlocks, stagingBlocksShare);

            BitMatrix[] retval = {offsetShare, maskedIndexShare, nextMaskedIndexShare};
            return retval;
        }

        @Override
        public BitMatrix load(int index, int num) throws IOException {  return alignedMemory.load(index, num);  }

        @Override
        public BitMatrix loadOblivious(BitMatrix indexShare, int numBlocks) throws IOException {
            if (numBlocks == 1)
                return alignedMemory.loadOblivious(indexShare, numBlocks);

            MemoryArea staging = setupUnalignedForSize(numBlocks);

            BitMatrix offsetShare = loadIntoStagingArea(staging, indexShare, numBlocks)[0];

            // ==== Load requested (unaligned) block from staging area) ====
            BitMatrix oneShare = BitMatrix.valueOf(getShareOfOne(), addOpWordSize);

            BitMatrix retShare = new BitMatrix(getBlockSize() * numBlocks);

            for (int i = 0; i < numBlocks; ++i) {
                if (i > 0)
                    // Increment offsetShare
                    offsetShare = addOp.doOp(state, offsetShare, oneShare)[0];
                BitMatrix blockShare = staging.loadOblivious(offsetShare, 1);
                retShare.setBits(i * getBlockSize(), blockShare);

            }

            return retShare;
        }

        @Override
        public void store(int index, BitMatrix blockShares) throws IOException {
            alignedMemory.store(index, blockShares);
        }

        @Override
        public void storeOblivious(BitMatrix indexShare, BitMatrix blockShare) throws IOException {
            final int blockSize = getBlockSize();
            final int numBlocks = blockShare.getNumCols() / blockSize;

            if (numBlocks == 1) {
                alignedMemory.storeOblivious(indexShare, blockShare);
                return;
            }

            MemoryArea staging = setupUnalignedForSize(numBlocks);

            BitMatrix[] vals = loadIntoStagingArea(staging, indexShare, numBlocks);
            BitMatrix offsetShare = vals[0];
            BitMatrix maskedIndexShare = vals[1];
            BitMatrix nextMaskedIndexShare = vals[2];

            BitMatrix oneShare = BitMatrix.valueOf(getShareOfOne(), addOpWordSize);

            // We overwrite the staging area for the relevant blocks
            for (int i = 0; i < numBlocks; ++i) {
                if (i > 0)
                    // Increment offsetShare
                    offsetShare = addOp.doOp(state, offsetShare, oneShare)[0];
                BitMatrix curBlock = blockShare.getSubMatrixCols(i * blockSize, blockSize);
                staging.storeOblivious(offsetShare, curBlock);
            }

            // Store each multiblock separately, as it might not be aligned.
            alignedMemory.storeOblivious(maskedIndexShare, staging.load(0, numBlocks));
            alignedMemory.storeOblivious(nextMaskedIndexShare, staging.load(numBlocks, numBlocks));
        }

        @Override
        public void init(int blockSize, int blockCount) {
            alignedMemory.init(blockSize, blockCount);
        }

        @Override
        public void reset() {
            alignedMemory.reset();
        }

        @Override
        public void writeTo(SendableOutput out) throws IOException {
            alignedMemory.writeTo(out);
        }

        @Override
        public void readFrom(SendableInput in) throws IOException {
            alignedMemory.readFrom(in);
        }
    }
}
