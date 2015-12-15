package org.factcenter.inchworm;

import org.factcenter.qilin.comm.SendableInput;
import org.factcenter.qilin.comm.SendableOutput;
import org.factcenter.qilin.util.BitMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

abstract public class SimpleRAM implements MemoryArea {
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	
    int blockSize;
	
	protected BitMatrix ram;


    public SimpleRAM() {
    }

	@Override
	public int getBlockSize() {
		return blockSize;
	}

	@Override
	public int getBlockCount() {
        if (ram == null)
            return 0;
		return ram.getNumCols() / blockSize;
	}

    @Override
    public void store(int index, BitMatrix blockShares) {
        ram.setBits(index * blockSize, blockShares);

    }

    /**
     * Initialize with pre-existing data
     * @param data
     * @param blockSize
     */
    public void init(BitMatrix data, int blockSize) {
        this.blockSize = blockSize;
        this.ram = data;

    }

    public BitMatrix getMemoryData() {
        return ram;
    }

    // The oblivious load/store are abstract since they differ between dummy/concrete instances.
    @Override
    abstract public void storeOblivious(BitMatrix indexShare, BitMatrix blockShare) throws IOException;

    @Override
    abstract public BitMatrix loadOblivious(BitMatrix indexShare, int numBlocks) throws IOException;

    @Override
    public void init(int blockSize, int blockCount) {
        this.blockSize = blockSize;
        if (ram == null || ram.getNumCols() != blockSize * blockCount)
            ram = new BitMatrix(blockSize * blockCount);
    }

	@Override
	public BitMatrix load(int pos, int num) {
		return ram.getSubMatrixCols(pos * blockSize, num * blockSize);
	}


	@Override
	public void reset() {
		ram.reset();
	}

    @Override
    public void writeTo(SendableOutput out) throws IOException {
        out.writeInt(blockSize);
        out.writeObject(ram);
    }

    @Override
    public void readFrom(SendableInput in) throws IOException {
        blockSize = in.readInt();
        ram = in.readObject(BitMatrix.class);
    }
}
