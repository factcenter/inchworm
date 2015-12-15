package org.factcenter.pathORam.ops;

import org.factcenter.inchworm.*;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.inchworm.ops.OpDefaults;
import org.factcenter.inchworm.ops.VMProtocolPartyInfo;
import org.factcenter.pathORam.*;
import org.factcenter.qilin.comm.SendableInput;
import org.factcenter.qilin.comm.SendableOutput;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;
import java.util.Random;

/**
 * This class has two main purposes. 1. Deal with construction
 * of the pathORam-ram. 2. Avoid duplicating same code in dummy and
 * concrete opcodes
 *         
 * @author mikegarts 
 */
public abstract class GenericPathORAMFactory extends VMProtocolPartyInfo implements MemoryFactory {

    /**
     * A memory factory for generating the "base" memory areas (that are not based on path-ORAM).
     */
    protected MemoryFactory memFactory;

    SimpleRAM baseMemory;

    protected OTExtender otExtender;

    public GenericPathORAMFactory(MemoryFactory memFactory) {
        this.memFactory = memFactory;

        MemoryArea baseMemory = memFactory.createNewMemoryArea(MemoryArea.Type.TYPE_RAM);
        if (!(baseMemory instanceof SimpleRAM)) {
            throw new UnsupportedArgException("Memory Factory for base memory must create SimpleRAMs!");
        }

        this.baseMemory = (SimpleRAM) baseMemory;
    }


    /**
     * Sets the OT extension for the concrete op.
     * @param otExtender
     */
    public void setMoreParameters(OTExtender otExtender) {
        this.otExtender = otExtender;
    }

    @Override
    public void setParameters(int playerId, VMState state, VMRunner runner, Random rand) {
        super.setParameters(playerId, state, runner, rand);
        memFactory.setParameters(playerId, state, runner, rand);
    }

    @Override
    public void init() throws IOException, InterruptedException {
        super.init();
        memFactory.init();
    }

    /**
	 * @author mikegarts
	 * Delegates the load/store operations to the load/store opcodes
	 */
	public class ConcretePositionMapFactory implements
			BlockStoragePositionMapFactory {
		@Override
		public BlockStoragePositionMap createPositionMap(
                BlockStorage blockStorage, int valuesInBlock) {
			
			return new BlockStoragePositionMap(blockStorage, valuesInBlock) {
				
				@Override
				public BitMatrix store(BitMatrix valueShare, int blockLen,
						long idxShare, BitMatrix memShare) throws IOException {

                    baseMemory.init(memShare, blockLen);
                    baseMemory.storeOblivious(BitMatrix.valueOf(idxShare, 32), valueShare);
                    return baseMemory.getMemoryData();
				}

				@Override
				public BitMatrix load(int blockBitLength, long idxShare,
						BitMatrix memShare) throws IOException {
                    baseMemory.init(memShare, blockBitLength);
					return baseMemory.loadOblivious(BitMatrix.valueOf(idxShare, 32), 1);
				}

				@Override
				public int unshareInt(int oldPositionShare) {
					int oldPosition;
					try {
						runner.getChannel().writeInt(oldPositionShare);
						runner.getChannel().flush();
						oldPosition = runner.getChannel().readInt() ^ oldPositionShare;
						return oldPosition;
					} catch (IOException e) {
						throw new RuntimeException();
					}

				}

				@Override
				public boolean getValidBit() {
					return getPlayerId() == 0;
				}				
			};
		}
	}


	/**
	 * Implement this method with the stash you would like to use.
	 * Dummy,Circuits-based or a debug stash
	 */
	public abstract StashFactory getStashFactory();


    /**
     * Used only for inefficient block-by-block storage fallback.
     * @return
     */
    public abstract  VMOpImplementation getOpImpl();

    public class PathORAM implements MemoryArea {

        protected int ramSize;
        protected int ramWordSizeBits;

        protected BlockStorage ram;

        @Override
        public BitMatrix loadOblivious(BitMatrix indexShare, int numBlocks) throws IOException {
            if (numBlocks != 1) {
                logger.warn("PathORAM doesn't support loading more than one block at a time; falling back to" +
                        "extremely inefficient block-by-block method");

                BitMatrix result = new BitMatrix(getBlockSize() * numBlocks);
                OpAction addOp = getOpImpl().getOpAction(OpDefaults.Op.OP_ADD.name);
                BitMatrix curIdxShare = indexShare;
                for (int i = 0; i < numBlocks; ++i) {
                    BitMatrix currentBlock = loadOblivious(curIdxShare, 1);

                    result.setBits(i * getBlockSize(), currentBlock);


                    // xor of both playerIds is 1, so adding playerId increments the index.
                    BitMatrix[] addResults = addOp.doOp(getState(), curIdxShare,
                            BitMatrix.valueOf(getPlayerId(), getState().getWordSize()));

                    curIdxShare = addResults[0];
                }
                return result;
            }

            Block loaded = ram.fetchBlock((int) indexShare.toInteger());
            logger.debug("LoadStoreProtocolImp.loadMem index={} data={}", indexShare, Converters.toHexString(loaded.getData()));
            return loaded.getData();
        }

        @Override
        public void storeOblivious(BitMatrix indexShare, BitMatrix valueShare) throws IOException {
            if (valueShare.getNumCols() != getBlockSize()) {
                logger.warn("PathORAM doesn't support storing more than one block; using" +
                        " extremely inefficient block-by-block method");

                int numBlocks = valueShare.getNumCols() / getBlockSize();
                BitMatrix curIdxShare = indexShare;
                OpAction addOp = getOpImpl().getOpAction(OpDefaults.Op.OP_ADD.name);

                for (int i = 0; i < numBlocks; ++i) {
                    BitMatrix currentBlock = valueShare.getSubMatrixCols(i * getBlockSize(), getBlockSize());

                    storeOblivious(curIdxShare, currentBlock);
                    // xor of both playerIds is 1, so adding playerId increments the index.
                    BitMatrix[] addResults = addOp.doOp(getState(), curIdxShare,
                            BitMatrix.valueOf(getPlayerId(), getState().getWordSize()));

                    curIdxShare = addResults[0];

                }
                return;
            }

            logger.debug("LoadStoreProtocolImp.store index={} data={}", indexShare , Converters.toHexString(valueShare));
            ram.storeBlock(Block.create((int) indexShare.toInteger(), valueShare, getPlayerId() ==0));
            return;
        }


        @Override
        public void init(int blockSize, int blockCount) {
            ramSize = blockCount;
            ramWordSizeBits = blockSize;

            if (ramSize < 4) {
                return;
            }

            ServerFactory serverFactory = new LocalServerFactory(
                    PathORamServer.DEFAULT_PATH_ORAM_BUCKET_SIZE);
            DriverFactory pathORamFactory = new DriverFactory();

            StashFactory stashFactory = getStashFactory();
            PositionMapFactory positionMapFactory = new InchwormPositionMapFactory(memFactory, getPlayerId(), getState(),
                    getRunner(), rand);


            BlockStoragePositionMapFactory blockStoragePositionMapFactory = new ConcretePositionMapFactory();
            PathORamCreator creator = new PathORamCreator(positionMapFactory,
                    stashFactory, rand, serverFactory, pathORamFactory,
                    blockStoragePositionMapFactory);

            BlockStorage pathORam = creator.createPathORam(ramSize, ramWordSizeBits);
            ram = pathORam;
        }

        @Override
        public void store(int idx, BitMatrix blockShares) throws IOException {
            logger.warn("PathORAM doesn't currently support direct access to local RAM shares; using oblivious method");
            int blockPtrSize = 32 - Integer.numberOfLeadingZeros(getBlockCount());
            if (getPlayerId() == 0)
                storeOblivious(BitMatrix.valueOf(idx, blockPtrSize), blockShares);
            else
                storeOblivious(BitMatrix.valueOf(0, blockPtrSize), blockShares);
        }

        @Override
        public int getBlockSize() {
            return ramWordSizeBits;
        }

        @Override
        public int getBlockCount() {
            return ramSize;
        }

        @Override
        public BitMatrix load(int pos, int num) throws IOException {
            logger.warn("PathORAM doesn't currently support direct access to local RAM shares; using oblivious method");
            return loadOblivious(BitMatrix.valueOf(pos, 1), num);
        }


        @Override
        public void reset() {
            logger.error("PathORAM doesn't currently support reset");
            throw new UnsupportedOperationException("PathORAM doesn't currently support reset");
        }

        @Override
        public void writeTo(SendableOutput out) throws IOException {
            // TODO: Implement
        }

        @Override
        public void readFrom(SendableInput in) throws IOException {
            // TODO: Implement
        }
    }

    @Override
    public MemoryArea createNewMemoryArea(MemoryArea.Type memArea) {
        switch (memArea) {
            case TYPE_RAM:
                return new PathORAM();
            default:
                assert(memFactory != null);
                return memFactory.createNewMemoryArea(memArea);
        }
    }
}