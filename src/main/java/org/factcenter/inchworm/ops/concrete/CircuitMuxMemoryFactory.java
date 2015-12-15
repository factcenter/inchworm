package org.factcenter.inchworm.ops.concrete;

import org.factcenter.fastgc.inchworm.MuxKNOpInchworm;
import org.factcenter.fastgc.inchworm.UnMuxKNOpInchworm;
import org.factcenter.inchworm.MemoryArea;
import org.factcenter.inchworm.MemoryFactory;
import org.factcenter.inchworm.SimpleRAM;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;

/**
 * Created by talm on 9/8/14.
 */
public class CircuitMuxMemoryFactory extends ConcreteCommon implements MemoryFactory {
    public CircuitMuxMemoryFactory() {
        super(null);
    }

    @Override
    public MemoryArea createNewMemoryArea(MemoryArea.Type memArea) {
        return new CircuitMuxMemory();
    }

    class CircuitMuxMemory extends SimpleRAM implements MemoryArea {
        MuxKNOpInchworm[] muxLoaders;
        UnMuxKNOpInchworm[] muxStorers;
        boolean needsInit = true;
        int ctrlBits;

        public void initMuxes() throws IOException {
            // We allow block sizes in powers of two up to half the memory size.
            for (int i = 0; i < ctrlBits; ++i) {
                int width = 1 << i;
                int virtualBlockSize = getBlockSize() * width;
                int virtualBlockCount = getBlockCount() / width;
                muxLoaders[i] = new MuxKNOpInchworm(getPlayerId() == 0, rand, labelBitLength, virtualBlockSize, virtualBlockCount,
                        ctrlBits - i, getChannel(), otExtender);
                muxLoaders[i].init();

                muxStorers[i] = new UnMuxKNOpInchworm(getPlayerId() == 0, rand, labelBitLength, virtualBlockSize, virtualBlockCount,
                        ctrlBits - i, getChannel(), otExtender);
                muxStorers[i].init();


            }
        }

        @Override
        public void init(int blockSize, int blockCount) {
            if (getBlockSize() == blockSize && getBlockCount() == blockCount)
                return;
            super.init(blockSize, blockCount);
            ctrlBits = 32 - Integer.numberOfLeadingZeros(blockCount) - 1;

            muxLoaders = new MuxKNOpInchworm[ctrlBits];
            muxStorers = new UnMuxKNOpInchworm[ctrlBits];
            // We can't initialize here, because the parameters may not have been set yet for
            // CircuitMuxMemoryFactory, so we delay to the first load.
            needsInit = true;

        }

        @Override
        public BitMatrix loadOblivious(BitMatrix indexShare, int numBlocks) throws IOException {
            if (needsInit) {
                initMuxes();
                needsInit = false;
            }

            assert(Integer.bitCount(numBlocks) == 1); // we only support powers of two
            if (numBlocks >= getBlockCount())
                return ram;

            BitMatrix randShare = new BitMatrix(getBlockSize() * numBlocks);
            randShare.fillRandom(rand);

            int logNumBlocks = 32 - Integer.numberOfLeadingZeros(numBlocks) - 1;

            int virtualCtrlbits = ctrlBits - logNumBlocks;
            muxLoaders[logNumBlocks].setData(ram, indexShare.getSubMatrixCols(logNumBlocks, virtualCtrlbits), randShare);
            muxLoaders[logNumBlocks].run();

            BitMatrix result;
            if (getPlayerId() == 0) {
                result = BitMatrix.valueOf(muxLoaders[logNumBlocks].opOutput, getBlockSize() * numBlocks);
                result.xor(randShare);
            } else {
                result = randShare;
            }

            return result;
        }

        @Override
        public void storeOblivious(BitMatrix indexShare, BitMatrix blockShare) throws IOException {
            if (needsInit) {
                initMuxes();
                needsInit = false;
            }

            int numBlocks = blockShare.getNumCols() / getBlockSize();
            assert(Integer.bitCount(numBlocks) == 1); // we only support powers of two
            if (numBlocks >= getBlockCount()) {
                store(0, blockShare);
            }

            BitMatrix randShare = new BitMatrix(ram.getNumCols());
            //randShare.fillRandom(rand);

            int logNumBlocks = 32 - Integer.numberOfLeadingZeros(numBlocks) - 1;

            int virtualCtrlbits = ctrlBits - logNumBlocks;
            muxStorers[logNumBlocks].setData(ram, blockShare, indexShare.getSubMatrixCols(logNumBlocks, virtualCtrlbits), randShare);
            muxStorers[logNumBlocks].run();

            BitMatrix result;
            if (getPlayerId() == 0) {
                result = BitMatrix.valueOf(muxStorers[logNumBlocks].opOutput, ram.getNumCols());
                result.xor(randShare);
            } else {
                result = randShare;
            }

            store(0, result);
        }
    }
}
