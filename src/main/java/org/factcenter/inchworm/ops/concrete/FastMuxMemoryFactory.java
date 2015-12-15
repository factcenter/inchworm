package org.factcenter.inchworm.ops.concrete;

import org.factcenter.inchworm.*;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;

/**
 * A MemoryFactory that creates FastMux-based Memory Areas
 */
public class FastMuxMemoryFactory extends ConcreteCommon implements MemoryFactory {
    public FastMuxMemoryFactory() {
        super(null);
    }

    @Override
    public MemoryArea createNewMemoryArea(MemoryArea.Type memArea) {
        return new FastMuxRAM();
    }


    public class FastMuxRAM extends SimpleRAM {
        /**
         * Store a set of consecutive blocks. The number of blocks must be a power of two, and indices are implicitly
         * assumed to be aligned on the "multiblock" boundary (e.g., if storing pairs of blocks, indices must be even).
         * @param indexShare Share of the index at which to store.
         * @param blockShare the shares of the blocks to store.
         * @throws IOException
         */
        @Override
        public void storeOblivious(BitMatrix indexShare, BitMatrix blockShare) throws IOException {
            // Note: we only support store aligned on power-of-2 boundaries
            // (e.g., if blockShare is of size half the memory, index must be 0 or memSize/2)
            // In particular, this means the size of blockShare (in blocks) must be a power-of-2

            if (blockShare.getNumCols() < getBlockSize()) {
                // We're storing something that's smaller than a single block, We will zero-pad at the end
                BitMatrix newBlockShare = new BitMatrix(getBlockSize());
                newBlockShare.copyBits(blockShare);
                blockShare = newBlockShare;
            } else if (blockShare.getNumCols() % getBlockSize() != 0) {
                // We're storing something that isn't evenly divisible into blocks.
                // We'll cut off the last bits...
                int extraBits = blockShare.getNumCols() % getBlockSize();
                BitMatrix newBlockShare = blockShare.getSubMatrixCols(0, blockShare.getNumCols() - extraBits);
                blockShare = newBlockShare;
            }

            int actualBlocksNum = blockShare.getNumCols() / getBlockSize();


            if ( Integer.bitCount(actualBlocksNum) != 1) {
                throw new UnsupportedArgException(String.format("We currently support oblivious storage only if " +
                        "the number of blocks is a power of 2 (%d is not)", actualBlocksNum));
            }


            // Shift-right by log_2(actualBlocksNum) [since block size is multiplied by actualBlocksNum]
            int log2 = Integer.numberOfTrailingZeros(actualBlocksNum);

            long actualIndexShare = indexShare.toInteger(64);
            actualIndexShare >>>= log2;

            ram = FastUnMux.fastUnMuxMultiple(blockShare, blockShare.getNumCols(), actualIndexShare, ram, otExtender,
                    rand, runner, getPlayerId(), null, null);
        }

        /**
         * Load a set of consecutive blocks. The number of blocks must be a power of two, and indices are implicitly
         * assumed to be aligned on the "multiblock" boundary (e.g., if storing pairs of blocks, indices must be even).
         * @param indexShare share of the starting index from which to load
         * @param numBlocks number of blocks to load
         * @return a share of the loaded blocks
         * @throws IOException
         */
        @Override
        public BitMatrix loadOblivious(BitMatrix indexShare, int numBlocks) throws IOException {
            if ( Integer.bitCount(numBlocks) != 1) {
                logger.error("We currently support oblivious load only if " +
                        "the number of blocks is a power of 2 ({} is not))", numBlocks);

                throw new UnsupportedArgException(String.format("We currently support oblivious load only if " +
                        "the number of blocks is a power of 2 (%d is not)", numBlocks));
            }
            int log2 = Integer.numberOfTrailingZeros(numBlocks);

            long actualIndexShare = indexShare.toInteger(64);
            actualIndexShare >>>= log2;
            return FastMux.fastMuxMultiple(getBlockSize() * numBlocks, actualIndexShare,
                    ram, otExtender, rand, runner, getPlayerId(), null, null);
        }
    }


}
