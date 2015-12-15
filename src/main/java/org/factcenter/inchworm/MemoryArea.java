package org.factcenter.inchworm;

import org.factcenter.qilin.comm.Sendable;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;


/**
 * Implements an interface to memory.
 * This interface should allow initializing the memory and reading out blocks.
 * In the naive storage implementation, the memory is simply shared between the parties
 * (see {@link SimpleRAM}); in this case initializing and reading are fast (local) operations.
 * 
 * Other implementations may be more complex (e.g., using ORAM).
 * Operations may throw an {@link java.lang.UnsupportedOperationException} if the operation
 * is not supported (e.g., writing to read-only memory).
 */
public interface MemoryArea extends Sendable {
	/**
	 * @return Storage's block size (in bits)
	 */
	public int getBlockSize();

	/**
	 * @return Number of blocks in the storage
	 */
	public int getBlockCount();


    /**
     * Reads a consecutive sequence of block shares from a known position (these are shares of the RAM).
     * @param index index of the first block to read.
     * @param num number of blocks to read.
     * @return a bit vector containing shares of the blocks.
     */
    public BitMatrix load(int index, int num) throws IOException;

    /**
     * Obliviously read memory.
     * Hides the index of the block(s) read and the block contents.
     *
     * @param indexShare a share of the index (the index denotes which block of memory should be read).
     * @param numBlocks Number of blocks to read.
     * @return a share of a memory block
     * @throws IOException
     */
    public BitMatrix loadOblivious(BitMatrix indexShare, int numBlocks) throws IOException;

    /**
     * Stores block shares at a known (public) position. The blocks are stored consecutively, beginning at index pos.
     *
     * @param blockShares Shares of the blocks to store (should be a bit vector, where each
     * 	consecutive {@link #getBlockSize()} bits are a block.
     * @param index the index at which to begin storing the blocks.
     */
    public void store(int index, BitMatrix blockShares)  throws IOException;


    /**
     * Obliviously store a block of memory.
     * Hides the contents of the block and the location to which it is stored.
     * @param indexShare a share of the index (the index denotes which block of memory should be read)
     * @param blockShare the share of the block to be written. This must be a power-of-2 times the blocksize.
     */
    public void storeOblivious(BitMatrix indexShare, BitMatrix blockShare) throws IOException;

    /**
     * Initialize memory (no operations should be performed on memory before it is initialized).
     * If memory has already been initialized (in the same configuration), this method should do nothing.
     */
    public void init(int blockSize, int blockCount);

	/**
	 * Reset memory to initial state (or initialize memory if it has not been initialized).
	 */
	public void reset();

    enum Type {
        TYPE_REG,
        TYPE_RAM,
        TYPE_ROM,
        TYPE_STACK,
    }
}
