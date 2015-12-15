package org.factcenter.pathORam.ops.concrete;

import org.factcenter.fastgc.inchworm.*;
import org.factcenter.fastgc.inchworm.SingleWriteStashCircuit.Result;
import org.factcenter.inchworm.Converters;
import org.factcenter.inchworm.VMRunner;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.concrete.ConcreteCommon;
import org.factcenter.pathORam.*;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

public class ConcreteStash extends ConcreteCommon implements Stash {

	private List<Block> blocksList = new ArrayList<Block>();
	private int blockSizeInBits;
	private int blockCount;
	private int bitWidthIndex;
	private int bitWidthData;
	private int bitWidthStashEntry;
	private SingleMuxInchworm singleMux;
	private SingleWriteStashInchworm singleWriteStash;
	private PopStashEntrySharedInchworm popStashEntry;
	private TreeHelper treeHelper;
	private int indexMask;
	
	/**
	 * Label bit-length. (= OT Extender default security parameter)
	 */
	final static int labelBitLength = 80;

	public ConcreteStash(int blockSize, int stashSize, int pathORamSize) {
        super(null);
		this.blockSizeInBits = blockSize;
		blocksList = new ArrayList<Block>();
		
		this.treeHelper = new TreeHelper(pathORamSize);

		bitWidthIndex = TreeHelper.log2ceil(pathORamSize);
		bitWidthData = blockSize;
		bitWidthStashEntry = bitWidthData + bitWidthIndex;
		for (int i = 0; i < stashSize; ++i) {
			blocksList.add(i, Block.createDummy(blockSize));
		}
		
		this.blockCount = stashSize;
		indexMask = (1 << bitWidthIndex) - 1; 
	}

	@Override
	public void setParameters(int playerId, VMState state, VMRunner runner, Random rand) {

		super.setParameters(playerId, state, runner, rand);

		boolean serverOp = (0 == playerId) ? true : false;
		singleMux = new SingleMuxInchworm(serverOp,
				rand, 
				labelBitLength,
				bitWidthIndex,
				bitWidthData,
				runner.getYaoChannel(),
				otExtender);
		
		singleWriteStash = new SingleWriteStashInchworm(serverOp,
				rand,
				labelBitLength,
				bitWidthIndex,
				bitWidthData,
				runner.getYaoChannel(),
				otExtender);
		
		popStashEntry = new PopStashEntrySharedInchworm(serverOp,
				rand,
				labelBitLength,
				bitWidthStashEntry,
				bitWidthIndex, 
				blocksList.size(),
				runner.getYaoChannel(), 
				otExtender);
		
		logger.info("bitWidthData={} bitWidthIndex={}", bitWidthData, bitWidthIndex);
	}

	@Override
	public void init() throws IOException, InterruptedException {
		otExtender.init();
		singleMux.init();
		singleWriteStash.init();
		popStashEntry.init();
		//singleChoose.init();
	}

	@Override
	public int getBlockSizeBits() {
		return blockSizeInBits;
	}

	@Override
	public int getBlockCount() {
		return blockCount;
	}

	@Override
	public void storeBlock(Block block) {
		int blockIndex = block.getId();
		BitMatrix blockData = block.getData();
		boolean inBlockValidBit = block.getValidBit();
				
		testIndex(blockIndex);
		SingleWriteStashInchworm myWriteStash = singleWriteStash;
		try {

			boolean carry = false;
			for (int i = 0; i < blocksList.size(); ++i) {

				BigInteger randBits = new BigInteger(bitWidthData + bitWidthIndex + 2, rand);

				Block currentStashBlock = blocksList.get(i);
				
				myWriteStash.setData(currentStashBlock.getId(), blockIndex ,
						currentStashBlock.getData(), blockData, carry,
						currentStashBlock.getValidBit(), inBlockValidBit, randBits);
				

				BigInteger resultBits = BigInteger.ZERO;
				
				myWriteStash.run();
				if (getPlayerId() == 0) {
					resultBits = myWriteStash.opOutput.xor(randBits);
				} else {
					resultBits = randBits;
				}

				Result myParsedResult = myWriteStash.parseResult(resultBits);

				carry = myParsedResult.carry;

				Block newBlock = Block.create(myParsedResult.index,
						Converters.toBitMatrix(myParsedResult.data,
								blockSizeInBits)
								, myParsedResult.valid);
				blocksList.set(i, newBlock);

			}

			// assert that write to stash succeeded. otherwise fault the program
			runner.getChannel().writeBoolean(carry ^ inBlockValidBit);
			runner.getChannel().flush();
			boolean unsharedCarryXorValid = runner.getChannel().readBoolean() ^ carry ^ inBlockValidBit;
			if (false != unsharedCarryXorValid) {
				// In either case (valid=true carry=false, valid=false, carry=true) this is an error.
				// The later case is probably error in a circuit (shouldn't write if block is an invalid block)
				// The former means that there was no empty slot in stash and write operation failed. So halting the program
				logger.error("ConcreteStash.storeBlock failed to store block in stash! exiting");
				throw new RuntimeException("ConcreteStash.storeBlock failed to store block in stash! exiting");
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	@Override
	public Block fetchBlock(int blockIndex) {
		testIndex(blockIndex);
		BigInteger resultBits = BigInteger.ZERO;
		boolean validOut = false;
		BigInteger dataOut = BigInteger.ZERO;
	
		SingleMuxInchworm myMux = singleMux;
		try {
			for (Block currentStashBlock : blocksList) {
				
				BigInteger randBigInt = new BigInteger(myMux.getRandBitsLength(), rand);
				
				myMux.setData(blockIndex, 
						currentStashBlock.getId(),
						currentStashBlock.getData(), 
						currentStashBlock.getValidBit(),
						randBigInt);
			
				myMux.run();
				if (0 == getPlayerId()) {
					resultBits = resultBits.xor(myMux.opOutput);
				}
				resultBits = resultBits.xor(randBigInt);
				
				SingleMuxCircuit.Result parsedResult = myMux.parseResult(resultBits);
				dataOut = dataOut.xor(parsedResult.data);
				validOut = validOut ^ parsedResult.validBit;
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		

		BitMatrix data = Converters.toBitMatrix(resultBits, blockSizeInBits);
		return Block.create(blockIndex, data, validOut);
	}

	@Override
	public Map<Integer, Bucket> popBucket(int oldPosition,
										  PositionMap positionMap, int bucketSize) {
		
		HashMap<Integer, Integer> tempPositionMap = new HashMap<Integer, Integer>();
	
		int elementindex = 0;
		for (Block currentStashBlock : blocksList){
			int val =  positionMap.get(currentStashBlock.getId()) & indexMask;
			tempPositionMap.put(elementindex, val);
			++elementindex;
		}

		Map<Integer,Bucket> path = new HashMap<Integer, Bucket>();
		for (int level = treeHelper.getTreeHeight(); level >= 0; level--) {
			Bucket bucket = popBucket(oldPosition, level, tempPositionMap, bucketSize);
			path.put(level, bucket);
		}
		return path;
		
	}
	
	
	public Bucket popBucket(int oldPosition, int level,
			HashMap<Integer, Integer> positionMap, int bucketSize) {
				
		List<Block> removedBlocks = new LinkedList<Block>();
		try {
			BigInteger pathPrefix 		= BigInteger.valueOf(treeHelper.calcPrefix(oldPosition 	& indexMask, level));
			
			for (int currentBlockInBucket = 0; currentBlockInBucket < bucketSize; ++currentBlockInBucket) {	
				BigInteger randValidBits 	= new BigInteger(blockCount, rand);
				BigInteger randEntryBits 	= new BigInteger(bitWidthStashEntry, rand);
				BigInteger[] stashEntries 	= new BigInteger[blockCount];
				BigInteger[] entryPrefixes 	= new BigInteger[blockCount];
				boolean[] validBits 		= new boolean[blockCount];
				BigInteger randEntryValid	= new BigInteger(1,rand);
				
				// prepare inputs. huge input size!
				for (int i = 0 ; i < blocksList.size() ; i++){
					Block currentStashBlock = blocksList.get(i);
					
					stashEntries[i] = Converters.blockToBigInteger(currentStashBlock, bitWidthData, bitWidthIndex);
					
					int entryNode = positionMap.get(i);
					entryPrefixes[i] = BigInteger.valueOf(treeHelper.calcPrefix(entryNode 	& indexMask, level));
					
					validBits[i] = currentStashBlock.getValidBit();
					
				}

				popStashEntry.setData(pathPrefix, stashEntries, entryPrefixes, validBits, randValidBits, randEntryBits, randEntryValid);
				popStashEntry.run();
				
				BigInteger resultBits = BigInteger.ZERO;
				
				BigInteger randBits = randEntryValid.shiftLeft(blockCount).or(randValidBits).shiftLeft(bitWidthStashEntry).or(randEntryBits);
				
				if (getPlayerId() == 0) {
					resultBits = popStashEntry.opOutput.xor(randBits);
				} else {
					resultBits = randBits;
				}
				
				PopStashEntryCircuit.Result result = popStashEntry.parseResult(resultBits);
				
				// write valid bits back
				int i=0;
				for (Block block : blocksList){
					block.setValid(result.validBits[i++]);
				}
				
				Block toBucket = Converters.toBlock(result.entryToBucket,bitWidthIndex, bitWidthData, result.entryValidBit);
				removedBlocks.add(toBucket);
			}
			
		} catch (Exception e) {
			System.out.println("EXCEPTION in popLegalToStoreBlocks");
			e.printStackTrace();
			System.exit(-1);
		}
		return new Bucket(bucketSize, removedBlocks);
	}
	
	
	public int getMaxIndexVal() {
		return (1 << bitWidthIndex) - 1;
	}

	private void testIndex(int index) {
		if (index < 0 || index > getMaxIndexVal()) {
			throw new RuntimeException(String.format(
					"index overflow. index=%d , max = %d", index,
					getMaxIndexVal()));
		}
	}

	public List<Block> getBlocks(){
		return blocksList;
	}
	
}
