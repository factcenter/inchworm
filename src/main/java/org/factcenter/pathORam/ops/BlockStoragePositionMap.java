package org.factcenter.pathORam.ops;

import org.factcenter.pathORam.Block;
import org.factcenter.pathORam.BlockStorage;
import org.factcenter.pathORam.MyRandom;
import org.factcenter.pathORam.PositionMap;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;
import java.util.Random;

/**
 * @author mikegarts This class implements the PositionMap interface using
 *         provided BlockStorage. 
 */
public abstract class BlockStoragePositionMap implements PositionMap {
	private final BlockStorage blockStorage;
	private final Random random;
	private final int valuesInBlock;
	private final int valueLenBits;
	private final int maxIndex;
		
	public BlockStoragePositionMap(BlockStorage blockStorage, int valuesInBlock) {
		this.blockStorage = blockStorage;
		this.valuesInBlock = valuesInBlock;
		this.random = MyRandom.getRandom();
		this.valueLenBits = blockStorage.getBlockSizeBits() / valuesInBlock;
		maxIndex = valuesInBlock * blockStorage.getBlockCount();
	}

	@Override
	public int get(int positionIndex) {
		testIndex(positionIndex);
		
		int blockIndex = toBlockIndex(positionIndex);
		int indexInBlock = toIndexInBlock(positionIndex);
		Block block = blockStorage.fetchBlock(blockIndex);
		BitMatrix valueBits = loadFromBlock(valueLenBits, indexInBlock, block.getData());
		
		int unsignedValue = valueBitsToPositionInt(valueBits) ;
		return unsignedValue;
	
	}

	@Override
	public int updatePosition(int positionIndex) {
		testIndex(positionIndex);
		
		// generate new position
		int newPosition = getRandomPosition();
		testValue(newPosition);
		
		// calc block index and the position in block
		int blockIndex = toBlockIndex(positionIndex);
		int indexInBlock = toIndexInBlock(positionIndex);
		
		// get block bits (will recalculate block index inside)
		Block block = blockStorage.fetchBlock(blockIndex);
		BitMatrix blockBits = block.getData();
		
		// read value bits from block
		BitMatrix valueBits = loadFromBlock(valueLenBits, indexInBlock, blockBits);
		
		// convert bits to old position integer
		long oldPositionShare = valueBitsToPositionInt(valueBits);
		
		// unshares old position (does nothing when runs not in inchworm)
		int oldPosition = unshareInt((int)oldPositionShare) & getMaxSupportedValue();

		// prepare new position bitmatrix 
		BitMatrix newValueBm = new BitMatrix(valueLenBits);
		newValueBm.setBits(0, valueLenBits, newPosition);
		
		// store new position bitmatrix in the block 
		BitMatrix newBlockBits = storeInBlock(newValueBm, valueLenBits, indexInBlock, blockBits);
		
		// finally, store the updated block in the blockStorage 
		Block blockToWrite = Block.create(blockIndex, newBlockBits, getValidBit());
		blockStorage.storeBlock(blockToWrite);
		
		// return oldPosition value
		return oldPosition;

	}

	/**
	 * Return Returns different values when running in inchworm. When running without inchworm just returns true;
	 * @return Different values when running under inchworm, true when running without inchworm
	 */
	public abstract boolean getValidBit();

	/**
	* Unshares value when run under inchworm. Just returns the input when running without inchworm
	 */
	public abstract int unshareInt(int oldPositionShare);
	
	/**
	 * @return New randomized position.
	 */
	private int getRandomPosition() {
		return Math.abs(random.nextInt(getMaxSupportedValue()))% (getMaxSupportedValue() );
	}
	
	/**
	 * Reads the numeric value stored in the BitMatrix
	 */
	private int valueBitsToPositionInt(BitMatrix valueBits) {
		return (int) valueBits.getBits(0, valueLenBits) & getMaxSupportedValue();
	}


//	public void set(int positionIndex, int newValue) {
////		testValue(newValue);
////
////		BitMatrix newValueBm = new BitMatrix(valueLenBits);
////		newValueBm.setWord(0, valueLenBits, newValue);
////
////		storeNewValue(positionIndex, newValueBm);
//		bla[positionIndex] = newValue;
//	}

	private Block getBlock(int positionIndex) {
		int blockIndex = toBlockIndex(positionIndex);
		Block block = blockStorage.fetchBlock(blockIndex);

		if (null == block || null == block.getData()) {
			block = Block.createDummy(blockStorage.getBlockSizeBits());
		}
		return block;
	}

	protected int toBlockIndex(int positionIndex) {
		return positionIndex / valuesInBlock;
	}

	protected int toIndexInBlock(int positionIndex) {
		return (positionIndex % valuesInBlock);
	}

	public int getMaxSupportedValue() {
		return (1 << valueLenBits) - 1;
	}

	private void testIndex(int index) {
		if (index > maxIndex || index < 0) {
			throw new RuntimeException("index overflow index,value " + index
					+ " " + maxIndex);
		}
	}

	private void testValue(int newValue) {
		if (newValue > getMaxSupportedValue()) {
			throw new RuntimeException("value overflow newValue, maxValue "
					+ newValue + " " + getMaxSupportedValue());
		}
	}

	private BitMatrix storeInBlock(BitMatrix valueShare, int blockLen,
			long idxShare, BitMatrix memShare) {
		try {
//			System.out.printf("BlockStoragePositionMap.storeInBlock valueShare=%s blockLen=%d idxShare=%d memShare=%s (before store)\n",
//					Converters.toHexString(valueShare), blockLen, idxShare, Converters.toHexString(memShare));
			return store(valueShare, blockLen, idxShare, memShare);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("store failed");
		}
	}

	private BitMatrix loadFromBlock(int blockBitLength, long idxShare,
			BitMatrix memShare) {
		try {
			return load(blockBitLength, idxShare, memShare);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("load failed");
		}
	}

	protected abstract BitMatrix store(BitMatrix valueShare, int blockLen,
			long idxShare, BitMatrix memShare) throws IOException;

	protected abstract BitMatrix load(int blockBitLength, long idxShare,
			BitMatrix memShare) throws IOException;

}
