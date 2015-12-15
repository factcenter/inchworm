package org.factcenter.pathORam.ops.concrete;

import org.factcenter.inchworm.MemoryFactory;
import org.factcenter.inchworm.VMOpImplementation;
import org.factcenter.inchworm.ops.concrete.ConcreteCommon;
import org.factcenter.inchworm.ops.concrete.FastMuxMemoryFactory;
import org.factcenter.pathORam.Stash;
import org.factcenter.pathORam.StashFactory;
import org.factcenter.pathORam.ops.GenericPathORAMFactory;
import org.factcenter.qilin.protocols.OTExtender;

import java.io.IOException;

/**
 * A Path-ORAM based load/store op implementation
 * 
 */
public class PathORAMFactory extends GenericPathORAMFactory {
    /**
     * Create a new PathORAMFactory using a specific memory factory
     * for the base case. The passed factory must extend {@link ConcreteCommon}.
     * @param defaultFactory
     */
	public PathORAMFactory(MemoryFactory defaultFactory) {
        super(defaultFactory);
	}

    /**
     * Create a new PathORAMFactory using the default {@link FastMuxMemoryFactory}
     * for the base memory.
     */
    public PathORAMFactory() {
        this (new FastMuxMemoryFactory());
    }

    @Override
    public StashFactory getStashFactory() {
        return new ConcreteStashFactory();
    }

    @Override
    public VMOpImplementation getOpImpl() {
        // We don't support multi-load or multi-store.
        return null;
    }

    @Override
    public void setMoreParameters(OTExtender otExtender) {
        super.setMoreParameters(otExtender);
        ((ConcreteCommon) memFactory).setMoreParameters(otExtender);
    }

    private class ConcreteStashFactory implements StashFactory {

		@Override
		public Stash createStash(int stashCapacity, int pathORamBlocksCount,
								 int blockLenBits) {
			// For now - stash size equals to pathOramSize...
			try {
				int stashBlocksCount = stashCapacity;
				logger.debug("blockLenBits = {} stashBlocksCount={} pathORamBlocksCount={}", blockLenBits,
                        stashBlocksCount, pathORamBlocksCount);
				ConcreteStash stash = new ConcreteStash(blockLenBits,
						stashBlocksCount,  // stash size (in blocks)
						pathORamBlocksCount); // corresponding pathORam size in blocks
				stash.setMoreParameters(otExtender);
				
				stash.setParameters(getPlayerId(), getState(), getRunner(), rand);
				stash.init();
				return stash;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			throw new RuntimeException("Error creating stash");
		}
	}
}
