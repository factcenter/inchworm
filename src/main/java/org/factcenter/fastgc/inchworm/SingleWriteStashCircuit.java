package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.*;

import java.math.BigInteger;

/**
 * 
 * @author mikegarts
 * 
 *         INPUT: [
 *         IndexOld0, IndexNew0, DataOld0, DataNew0, carry0, validBit0 , ValidInBlock0, randomBits0,
 *         IndexOld1, IndexNew1, DataOld1, DataNew1, carry1, validBit1 , ValidInBlock1, randomBits0,
 *         ] 
 *		   OUTPUT: [IndexOut, DataOut ,validOut ,carryOut]
 *			(writeStash iif ((valid==0 and carry==0) OR (carry==0 and indexOld==indexNew)AND(ValidInBlock==1)) 
 *			carryOut = carryOld or writeStash 
 *			validOut = writeStash OR validOld
 *			IndexOut = IndexNew if writeStash, else IndexOld
 *			DataOut  = DataNew if writeStash, esle DataOld
 *         
 */
public class SingleWriteStashCircuit extends CompositeCircuit {

	private final int indexLenBits;
	private final int dataLenBits;

	// OFFSETS in input bits
	private final int IDX_OLD0;
	private final int IDX_NEW0;
	private final int DATA_OLD0;
	private final int DATA_NEW0;
	private final int CARRY0;
	private final int VALID0;
	private final int RANDOM_BITS0;
	private final int IDX_OLD1;
	private final int IDX_NEW1;
	private final int DATA_OLD1;
	private final int DATA_NEW1;
	private final int CARRY1;
	private final int VALID1;
	private final int RANDOM_BITS1;
	
	private UNSHARE unshareIndices;
	private UNSHARE unshareData;
	
	private MUX_2L_L muxIndices; 
	private MUX_2L_L muxData;
	
	private SHARE reShareData;
	private SHARE reShareIndices;
	private SHARE reshareCarry;
	private XOR_2_1 unshareValid;
	private SHARE reshareValid;
	private XOR_2_1 unshareCarry;
	private OR_2_1 orValidCarry;
	private INVERTER negateOrValidCarry;
	private OR_2_1 orCalcCarryOut;
	private OR_2_1 orCalcValidOut;
	private CompareWithCarryCircuit compareIndices;
	private OR_2_1 orValidCarryIndicesSame;
	private XOR_2_1 unshareValidInBlock;
	private AND_2_1 andShouldWriteValidInBlock;
	private int VALID_IN_BLOCK0;
	private int VALID_IN_BLOCK1; 

	static int NUM_SUBCIRCUITS = 18;

	public static int calcInDegree(int indexLenBits, int dataLenBits){
		return 6 * dataLenBits +     //(dataOld, dataNew, dataRandomBits) X (left, right)  
		   6 * indexLenBits +    //(indexOld, indexNew, indexRandomBits) X (left, right)
		   8 + // (carry, carryRand, valid, validRand) X (left, right);
		   2 ; // validInBlock X (L,R)
	}
	
	public SingleWriteStashCircuit(CircuitGlobals globals, int indexLenBits,
								   int dataLenBits) {
		super(globals, calcInDegree(indexLenBits, dataLenBits), 
				dataLenBits + indexLenBits + 1 + 1, // data+index+carry+valid
				NUM_SUBCIRCUITS, // sub circuits because somebody was too lazy
									// to calculate with reflection
				"Single_write_stash_" + indexLenBits + "_" + dataLenBits);
		this.indexLenBits = indexLenBits;
		this.dataLenBits = dataLenBits;

		// calculate offsets
		IDX_OLD0 = 0;
		IDX_NEW0 = IDX_OLD0 + indexLenBits;
		DATA_OLD0 = IDX_NEW0 + indexLenBits;
		DATA_NEW0 = DATA_OLD0 + dataLenBits;
		CARRY0 = DATA_NEW0 + dataLenBits;
		VALID0 = CARRY0 + 1;
		VALID_IN_BLOCK0 = VALID0 + 1;
		RANDOM_BITS0 = VALID_IN_BLOCK0 + 1;
		IDX_OLD1 = RANDOM_BITS0 + indexLenBits + dataLenBits + 1 + 1;
		IDX_NEW1 = IDX_OLD1 + indexLenBits;
		DATA_OLD1 = IDX_NEW1 + indexLenBits;
		DATA_NEW1 = DATA_OLD1 + dataLenBits;
		CARRY1 = DATA_NEW1 + dataLenBits;
		VALID1 = CARRY1 + 1;
		VALID_IN_BLOCK1 = VALID1 + 1;
		RANDOM_BITS1 = VALID_IN_BLOCK1 + 1;
		
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		unshareIndices = new UNSHARE(globals, indexLenBits);
		unshareData = new UNSHARE(globals, dataLenBits);
		
		muxData = new MUX_2L_L(globals, dataLenBits);
		muxIndices = new MUX_2L_L(globals, indexLenBits);

		reShareData = new SHARE(globals, dataLenBits);
		reShareIndices = new SHARE(globals, indexLenBits);
		reshareCarry = new SHARE(globals, 1);
		reshareValid = new SHARE(globals, 1);
		
		unshareValid = new XOR_2_1(globals);
		unshareCarry = new XOR_2_1(globals);
		orValidCarry = OR_2_1.newInstance(globals, isForGarbling);
		negateOrValidCarry = new INVERTER(globals);
		orCalcCarryOut = OR_2_1.newInstance(globals, isForGarbling);
		orCalcValidOut = OR_2_1.newInstance(globals, isForGarbling);
		
		compareIndices = new CompareWithCarryCircuit(globals, indexLenBits);
		orValidCarryIndicesSame = OR_2_1.newInstance(globals, isForGarbling);
		
		unshareValidInBlock = new XOR_2_1(globals);
		andShouldWriteValidInBlock = AND_2_1.newInstance(globals, isForGarbling);

		int i = 0;
		subCircuits[i++] = unshareIndices;
		subCircuits[i++] = unshareData;
		subCircuits[i++] = muxData;
		subCircuits[i++] = muxIndices;
		subCircuits[i++] = reShareData;
		subCircuits[i++] = reShareIndices;
		subCircuits[i++] = reshareCarry;
		subCircuits[i++] = reshareValid;
		
		subCircuits[i++] = unshareValid;
		subCircuits[i++] = unshareCarry;
		subCircuits[i++] = orValidCarry;
		subCircuits[i++] = negateOrValidCarry;
		subCircuits[i++] = orCalcCarryOut;
		subCircuits[i++] = orCalcValidOut;
		
		subCircuits[i++] = compareIndices;
		subCircuits[i++] = orValidCarryIndicesSame;
		
		subCircuits[i++] = unshareValidInBlock;
		subCircuits[i++] = andShouldWriteValidInBlock; 
	}

	@Override
	protected void connectWires() {

		unshareIndices.connectWiresToXY(inputWires, IDX_OLD0, inputWires, IDX_OLD1);
		unshareData.connectWiresToXY(inputWires, DATA_OLD0, inputWires, DATA_OLD1);
				
		// unshare CARRY 
		inputWires[CARRY0].connectTo(unshareCarry.inputWires, 0);
		inputWires[CARRY1].connectTo(unshareCarry.inputWires, 1);
		
		// unshare valid
		inputWires[VALID0].connectTo(unshareValid.inputWires, 0);
		inputWires[VALID1].connectTo(unshareValid.inputWires, 1);
		
		// unshareValidIn bit
		inputWires[VALID_IN_BLOCK0].connectTo(unshareValidInBlock.inputWires, 0);
		inputWires[VALID_IN_BLOCK1].connectTo(unshareValidInBlock.inputWires, 1);
		
		// connect carry to unshare with carry
		inputWires[CARRY0].connectTo(compareIndices.inputWires, compareIndices.carryIndexX());
		inputWires[CARRY1].connectTo(compareIndices.inputWires, compareIndices.carryIndexY());
		
		
		// mux data
		for (int i = 0; i < dataLenBits; ++i) {
			unshareData.outputWires[i].connectTo(muxData.inputWires, i + dataLenBits);
			unshareData.outputWires[i + dataLenBits].connectTo(muxData.inputWires, i);
		}
		
		// calc writeStash control bit (negate output)
		unshareCarry.outputWires[0].connectTo(orValidCarry.inputWires, 0);
		unshareValid.outputWires[0].connectTo(orValidCarry.inputWires, 1);
		orValidCarry.outputWires[0].connectTo(negateOrValidCarry.inputWires, 0);
		
		// mux indices
		for (int i = 0; i < indexLenBits; ++i){
			unshareIndices.outputWires[i].connectTo(muxIndices.inputWires, i + indexLenBits);
			unshareIndices.outputWires[i + indexLenBits].connectTo(muxIndices.inputWires, i);
			
			// connect to compareWithCarry
			unshareIndices.outputWires[i].connectTo(compareIndices.inputWires, i);
			unshareIndices.outputWires[i + indexLenBits].connectTo(compareIndices.inputWires, i + indexLenBits + 1);
		}
		
		// calculate should write bit
		negateOrValidCarry.outputWires[0].connectTo(orValidCarryIndicesSame.inputWires, 0);
		compareIndices.outputWires[0].connectTo(orValidCarryIndicesSame.inputWires, 1);
		
		// the should write bit is the output of andShouldWriteValidInBlock gate
		orValidCarryIndicesSame.outputWires[0].connectTo(andShouldWriteValidInBlock.inputWires, 0);
		unshareValidInBlock.outputWires[0].connectTo(andShouldWriteValidInBlock.inputWires, 1);
		
		Wire shouldWriteWire = andShouldWriteValidInBlock.outputWires[0];
		
		// connect to data mux's control bit
		shouldWriteWire.connectTo(muxData.inputWires , 2 * dataLenBits);
		
		// connect to index mux's control bit
		shouldWriteWire.connectTo(muxIndices.inputWires, 2 * indexLenBits);

		// calc carryOut
		shouldWriteWire.connectTo(orCalcCarryOut.inputWires,0);
		unshareCarry.outputWires[0].connectTo(orCalcCarryOut.inputWires,1);
		
		// calc validOut
		shouldWriteWire.connectTo(orCalcValidOut.inputWires,0);
		unshareValid.outputWires[0].connectTo(orCalcValidOut.inputWires, 1);
		
		// RESHARE
		reShareIndices.connectWiresToInputs(inputWires, RANDOM_BITS0,
											inputWires, RANDOM_BITS1, 
											muxIndices.outputWires, 0);
		reShareData.connectWiresToInputs(inputWires, RANDOM_BITS0 + indexLenBits, 
										 inputWires, RANDOM_BITS1 + indexLenBits, 
										 muxData.outputWires, 0);
		reshareCarry.connectWiresToInputs(inputWires, RANDOM_BITS0 + indexLenBits + dataLenBits,
										  inputWires, RANDOM_BITS1 + indexLenBits + dataLenBits,
										  orCalcCarryOut.outputWires,0); // carry_out_bit
		
		reshareValid.connectWiresToInputs(inputWires, RANDOM_BITS0 + indexLenBits + dataLenBits + 1,
				  						  inputWires, RANDOM_BITS1 + indexLenBits + dataLenBits + 1,
				  						  orCalcValidOut.outputWires,0); // valid_out_bit
	}

	@Override
	protected void defineOutputWires() {
		for (int i = 0; i < indexLenBits; ++i) {
			 reShareIndices.outputWires[i].connectTo(outputWires, i);
		}
		for (int i = 0; i < dataLenBits; ++i) {
			 reShareData.outputWires[i].connectTo(outputWires, i + indexLenBits);
		}
		// carry
		 reshareCarry.outputWires[0].connectTo(outputWires, indexLenBits + dataLenBits);
		
		// valid
		 reshareValid.outputWires[0].connectTo(outputWires, indexLenBits + dataLenBits + 1);
	}

	/**
	 * Combine data from one side into inputs for circuit. 
	 * Helper method
	 *
	 *     INPUT: [
	 *     IndexOld0, IndexNew0, DataOld0, DataNew0, carry0, valid0, validInBlockBit0, randomBits0,
	 *     IndexOld1, IndexNew1, DataOld1, DataNew1, carry1, valid1, validInBlockBit1, randomBits1,
	 *     ] 
	 */
	public BigInteger packInputs(BigInteger indexOld, BigInteger indexNew, BigInteger dataOld,
			BigInteger dataNew, BigInteger carry, BigInteger valid, BigInteger validInBlockBit, BigInteger r) {
		return r
				.shiftLeft(1).or(validInBlockBit)
				.shiftLeft(1).or(valid)
				.shiftLeft(1).or(carry)
				.shiftLeft(dataLenBits).or(dataNew)
				.shiftLeft(dataLenBits).or(dataOld)
				.shiftLeft(indexLenBits).or(indexNew)
				.shiftLeft(indexLenBits).or(indexOld);
	}
	
	/**
	 * 
	 * Helper class to work with the computation result
	 */
	public class Result
	{
		public Result(int index, BigInteger data, boolean carry, boolean valid)
		{
			this.index = index;
			this.data = data;
			this.carry = carry;
			this.valid = valid;
		}
		public int index;
		public BigInteger data;
		public boolean carry;
		public boolean valid;
		
		@Override
		public String toString() {
			return "Index = " + index + " data = " + data.toString(16) + " carry = " + carry + " valid = " + valid;
		}
	}
		
	/**
	 * Helper method to convert computation result (BigInteger) to Result class
	 */
	public Result parseResult(BigInteger resultBits)
	{
		BigInteger INDEX_MASK = BigInteger.ONE.shiftLeft(indexLenBits)
				.subtract(BigInteger.ONE);
		BigInteger DATA_MASK = BigInteger.ONE.shiftLeft(dataLenBits)
				.subtract(BigInteger.ONE);
		
		BigInteger index = resultBits
				.and(INDEX_MASK);
		BigInteger data = resultBits.shiftRight(indexLenBits)
				.and(DATA_MASK);
		BigInteger carry = resultBits.shiftRight(indexLenBits).shiftRight(dataLenBits)
				.and(BigInteger.ONE);
		BigInteger valid = resultBits.shiftRight(indexLenBits).shiftRight(dataLenBits).shiftRight(1)
				.and(BigInteger.ONE);
				
		return new Result((int)index.longValue(), 
				data,
				carry.equals(BigInteger.ONE) ? true : false,
				valid.equals(BigInteger.ONE) ? true : false);
	}
	
}
