package org.factcenter.pathORam.ops;

import org.factcenter.inchworm.MemoryArea;
import org.factcenter.inchworm.ops.VMProtocolPartyInfo;
import org.factcenter.pathORam.PositionMap;
import org.factcenter.pathORam.TreeHelper;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;
//import antlr.Utils;

public class InchwormPositionMap extends VMProtocolPartyInfo implements PositionMap {

	private MemoryArea ramPositionMapBits;
	private long entryMask;

//	private BlockStorage blockStorage;
//	private PositionMap positionMap;
	
	public InchwormPositionMap(int _entriesCount, MemoryArea ramPositionMapBits) {
        this.ramPositionMapBits = ramPositionMapBits;
		int entriesCount = TreeHelper.toPowerOfTwo(_entriesCount);
		int entryLengthBits = TreeHelper.log2ceil(entriesCount);
		//ramPositionMapBits = new BitMatrix(entriesCount * entryLengthBits);
        ramPositionMapBits.init(entryLengthBits, entriesCount);
		entryMask = (1 << entryLengthBits) - 1;

	}
			
	@Override
	public int get(int positionIndexShare) {
		try {
			BitMatrix bmOldPos;
			long oldPositionShare = 0;

            bmOldPos = ramPositionMapBits.loadOblivious(BitMatrix.valueOf(positionIndexShare, 32), 1);

			oldPositionShare = bmOldPos.toInteger();
			
			int valueToReturn = unsignEntry(oldPositionShare);
			//logger.debug("InchwormPositionMap.get in={} out={}, entriesCoun={}", positionIndexShare, valueToReturn, entriesCount);			
			return valueToReturn;
		} catch (IOException e) {
			System.out.println("EXCEPTION in get position");
			e.printStackTrace();
		}
		return -1;
	}

	public void set(int positionIndexShare, int newPosition) {
		try {
            ramPositionMapBits.storeOblivious(BitMatrix.valueOf(positionIndexShare, 32),
                    BitMatrix.valueOf(newPosition, ramPositionMapBits.getBlockSize()));

//			BitMatrix bmNewPos = new BitMatrix(entryLengthBits);
//			bmNewPos.setBits(0, entryLengthBits, (long) newPosition);
//			BitMatrix bmNewPosMap = runner
//					.getOps()
//					.getStore()
//					.store(bmNewPos, entryLengthBits, positionIndexShare,
//							ramPositionMapBits);
//			ramPositionMapBits = bmNewPosMap;
		} catch (IOException e) {
			System.out.println("EXCEPTION in set position");
			e.printStackTrace();
		}
	}

	@Override
	public int updatePosition(int indexShare) {
		int oldPosition = 0;
		try {
			long oldPositionShare = this.get(indexShare);
			int newPositionNumShare = Math.abs(rand.nextInt()) % ramPositionMapBits.getBlockCount();
			this.set(indexShare, newPositionNumShare);
			
			oldPosition = unsignEntry(unshareLong(oldPositionShare));
			
			logger.debug("updatePosition: oldPositionShare={} oldPosition={}, " +
                    "newPositionNumShare={}, indexShare={} entriesCount={}",oldPositionShare, oldPosition,
                    newPositionNumShare, indexShare, ramPositionMapBits.getBlockCount());
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("EXCEPTION in update position");
		}

		return oldPosition;
	}

	private long unshareLong(long oldPositionShare) throws IOException {
		long oldPosition;
		getRunner().getChannel().writeLong(oldPositionShare);
        getRunner().getChannel().flush();
		oldPosition = getRunner().getChannel().readLong() ^ oldPositionShare;
		return oldPosition;
	}

	public int unsignEntry(long number) {
		return (int)((number & entryMask) % ramPositionMapBits.getBlockCount());
	}
	
	@Override
	public void init() throws IOException, InterruptedException {
        BitMatrix randData = new BitMatrix(ramPositionMapBits.getBlockSize() * ramPositionMapBits.getBlockCount());
        randData.fillRandom(rand);
		ramPositionMapBits.store(0, randData);
	}
}
