package org.factcenter.inchworm.ops.dummy;

import org.factcenter.inchworm.*;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.inchworm.ops.VMProtocolParty;
import org.factcenter.inchworm.ops.common.*;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.util.BitMatrix;
import org.factcenter.qilin.util.IntegerUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Creates the set of dummy ops for the Inchworm Virtual Machine.
 */
public class DummyOPFactory implements MemoryFactory, VMOpImplementation, VMProtocolParty {

	/*-
	 * ----------------------------------------------------------------
	 *             DummyOPFactory Ops members.
	 * ----------------------------------------------------------------
	 */
    Map<String,OpAction> ops;

    ProtocolInfo info;
   
    class DummyRam extends SimpleRAM {
        private int reconstructIndex(BitMatrix indexShare) throws IOException {
            Channel toPeer = info.getChannel();

            toPeer.writeObject(indexShare);
            toPeer.flush();

            BitMatrix realIndex = toPeer.readObject(BitMatrix.class);
            realIndex.xor(indexShare);

            return (int) realIndex.toInteger(32);
        }

        /**
         * Check if a multiblock is aligned on a multiblock boundary.
         * @param index the starting index (in blocks) for the multiblock
         * @param numBlocks number of blocks in the multiblock. Must be a power of 2
         * @return true iff access is aligned
         */
        public boolean isAligned(int index, int numBlocks) {
            if (Integer.bitCount(numBlocks) != 1)
                logger.warn("Attempting access for block size {} -- not a power of 2", numBlocks);
            return Integer.numberOfTrailingZeros(index) >= Integer.numberOfTrailingZeros(numBlocks);
        }

        @Override
        public void storeOblivious(BitMatrix indexShare, BitMatrix blockShare) throws IOException {
            Channel toPeer = info.getChannel();

            int index = reconstructIndex(indexShare);

            BitMatrix peerRamShare;
            BitMatrix peerBlockShare;

            // Left (main) player.
            if (info.getPlayerId() == 0) {
                // We'll need to rerandomize entire memory to make sure it's oblivious
                peerRamShare = info.nextRandom(ram.getNumCols());
                peerBlockShare = info.nextRandom(blockShare.getNumCols());


//                toPeer.writeObject(peerRamShare);
//                toPeer.writeObject(peerBlockShare);
//                toPeer.flush();
            } else {
//                peerRamShare = toPeer.readObject(BitMatrix.class);
//                peerBlockShare = toPeer.readObject(BitMatrix.class);
            }
            BitMatrix reSharedBlock = blockShare.clone();
//            reSharedBlock.xor(peerBlockShare);

//            ram.xor(peerRamShare);
            int numBlocks = blockShare.getNumCols()/getBlockSize();

            if (!isAligned(index, numBlocks)) {
                logger.error("Unaligned dummy store (index: {}, numBlocks: {}", index, numBlocks);
            }

            store(index, reSharedBlock);
        }

        @Override
        public BitMatrix loadOblivious(BitMatrix indexShare, int numBlocks) throws IOException {
            int index = reconstructIndex(indexShare);

            if (!isAligned(index, numBlocks)) {
                logger.error("Unaligned dummy load (index: {}, numBlocks: {}", index, numBlocks);
            }

            return load(index, numBlocks);
        }
    }

	/*-
	 * ----------------------------------------------------------------
	 *             DummyOPFactory CTor.
	 * ----------------------------------------------------------------
	 */
	public DummyOPFactory() {
        ops = new HashMap<>();


        info = new ProtocolInfo();

        ops.put("out", new Out(info));
        ops.put("in", new In(info));

        ops.put("halt", new Halt(info));

        OpAction mov = new Mov(info);
        ops.put("load", mov);
        ops.put("store", mov);
        ops.put("loadreg", mov);
        ops.put("storereg", mov);
        ops.put("zero", mov);

        OpAction xor = new Xor(info);
        ops.put("xori", xor);
        ops.put("xor", xor);

        OpAction add = new DummyAdd(info);
        ops.put("add", add);
        ops.put("next", add);
        
        ops.put("sub", new DummySub(info));
        ops.put("and", new DummyAnd(info));
        ops.put("or", new DummyOr(info));
        ops.put("rol", new DummyRol(info));
        ops.put("mux", new DummyMux(info));
        ops.put("mul", new DummyMul(info));
        ops.put("div", new DummyDiv(info));

        ops.put("call", new DummyCall(info));
        ops.put("return", new DummyRet(info));
    }

    @Override
    public MemoryArea createNewMemoryArea(MemoryArea.Type memArea) {
    	if (memArea == MemoryArea.Type.TYPE_RAM) {
    		return new DummyRam();
    	}
        return new DummyRam();
    }


	/*-
	 * ----------------------------------------------------------------
	 *                VMProtocolParty Overrides.
	 * ----------------------------------------------------------------
	 */
	@Override
	public void setParameters(int playerNum, VMState state, VMRunner runner, Random rand) {
        info.setParameters(playerNum, state, runner, rand);
	}

	@Override
	public void init() throws IOException, InterruptedException {
		info.init();
	}

	@Override
	public int getPlayerId() {
		return info.getPlayerId();
	}



    @Override
    public OpAction getOpAction(String opName) {
        return ops.get(opName);
    }
}
