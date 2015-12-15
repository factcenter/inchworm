package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.*;

import java.math.BigInteger;

/**
 * 
 * @author mikegarts
 * <pre>
 * {@code
 *         INPUT: [
 *         Entry0, EntryNodePrefix0, Carry0, PathPrefix0			  		, valid0, 
 *         Entry1, EntryNodePrefix1, Carry1, PathPrefix1=PathPrefix0(unused), valid1, 
 *         ] 
 *         OUTPUT: [EntryToXorToBucket, EntryBackToStash, ValidToStash, CarryToNextChooseCircuit]
 *         compareOut = entryNodePrefix==CurrentPositionPrefix and carry==0
 *         popToBucket = valid AND compareOut
 *         >ValidToStash = valid AND (not popToBucket)
 *         >CarryToNextChooseCircuit = carry OR popToBucket
 *         if popToBucket:
 *         	>EntryToXorToBucket = Entry
 *          >EntryBackToStash = 0
 *         else:
 *         	>EntryToXorToBucket = 0
 *          >EntryBackToStash = Entry
 * }
 * </pre>
 */
public class SingleChooseCircuit extends CompositeCircuit {

	
	private final int entryLenBits;
	private final int prefixLenBits;
	
	// OFFSETS in input bits
	private final int ENTRY0;
	private final int ENTRY_NODE_PREFIX0;
	private final int CARRY0;
	private final int PATH_PREFIX0;
	private final int RANDOM_BITS0;
	private final int ENTRY1;
	private final int ENTRY_NODE_PREFIX1;
	private final int CARRY1;
	private final int PATH_PREFIX1_ZEROS;
	private final int RANDOM_BITS1;
	private final int VALID0;
	private final int VALID1;
	
	private XOR_2L_L unshareEntry;
	private XOR_2L_L unshareEntryNodePrefix;
	
	private CompareWithCarryCircuit comparePrefixes;

	private XOR_2L_L xorToCreateZeroEntry;
	private MUX_2L_L muxToBucket; 
//	private MUX_2L_L muxToStash;
	
//	private SHARE reShareToBucket;
//	private SHARE reShareToStash;
//	private SHARE reshareCarry;
//	private SHARE reshareValid;

	private XOR_2_1 unshareValid;
	private AND_2_1 andPopToBucket;
	private INVERTER negatePopToBucket;
	private AND_2_1 andCalcValudOut;
	private OR_2_1 orCalcCarryOut;
	
	private XOR_2_1 unshareCarry;
	private int CARRY_OUT_INDEX;
	private int ENTRY_OUT_INDEX;
	private int VALID_OUT_INDEX;
	//public final BigInteger ENTRY_MASK;

	static int NUM_SUBCIRCUITS = 11;
	private static BigInteger ENTRY_MASK;
	
	public static int calcInDegree(int entryLenBits,
			int prefixLenBits){
		// NO resharing at all
		return 2 * prefixLenBits      //(EntryNodePrefixA, EntryNodePrefixB)
				+ 2 * prefixLenBits    // (CurrentPrefixA, CurrentAPrefixB=zeros) 
				+ 2 * entryLenBits     // (Entry)X(left,right)
				+ 2					   // (carryIn) X (left, right)
				+ 2;                   // (validIn) X (left, right)
	}
	
	public static int calcOutDegree(int entryLenBits){
		return entryLenBits + 2;  // entry to bucket, carry, valid
	}
	
	public SingleChooseCircuit(CircuitGlobals globals, int entryLenBits,
			int prefixLenBits) {
		super(globals,
				calcInDegree(entryLenBits, prefixLenBits), 				   
				calcOutDegree(entryLenBits),     
				NUM_SUBCIRCUITS, 
				"SingleChooseCircuit" + entryLenBits + "_" + prefixLenBits);
		
		this.entryLenBits = entryLenBits;
		this.prefixLenBits = prefixLenBits;

		int randomLength = 0;//2 * entryLenBits + 2;// two entries and a carry bit + valid bit
		// calculate offsets
		ENTRY0 				= 0;
		ENTRY_NODE_PREFIX0 	= ENTRY0 				+ entryLenBits;
		CARRY0 				= ENTRY_NODE_PREFIX0 	+ prefixLenBits;
		PATH_PREFIX0 		= CARRY0 				+ 1;
		VALID0				= PATH_PREFIX0 			+ prefixLenBits;
		RANDOM_BITS0 		= VALID0 				+ 1;
		ENTRY1 				= RANDOM_BITS0 			+ randomLength;
		ENTRY_NODE_PREFIX1 	= ENTRY1 				+ entryLenBits;
		CARRY1 				= ENTRY_NODE_PREFIX1 	+ prefixLenBits;
		PATH_PREFIX1_ZEROS 	= CARRY1 				+ 1;
		VALID1				= PATH_PREFIX1_ZEROS 	+ prefixLenBits;
		RANDOM_BITS1 		= VALID1 				+ 1;
		
		ENTRY_OUT_INDEX = 0;
		CARRY_OUT_INDEX = ENTRY_OUT_INDEX + entryLenBits;
		VALID_OUT_INDEX = CARRY_OUT_INDEX + 1;
		
		ENTRY_MASK = BigInteger.ONE.shiftLeft(entryLenBits).subtract(BigInteger.ONE);
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		unshareEntry = new XOR_2L_L(globals, entryLenBits);
		unshareEntryNodePrefix = new XOR_2L_L(globals, prefixLenBits);
		
		comparePrefixes = new CompareWithCarryCircuit(globals, prefixLenBits);

		xorToCreateZeroEntry = new XOR_2L_L(globals, entryLenBits);
		muxToBucket = new MUX_2L_L(globals, entryLenBits); 
//		muxToStash = new MUX_2L_L(globals, entryLenBits);
		
		//reShareToBucket = new SHARE(globals, entryLenBits);
		//reShareToStash = new SHARE(globals, entryLenBits);
		//reshareCarry = new SHARE(globals, 1);

		unshareValid = new XOR_2_1(globals);
		andPopToBucket = AND_2_1.newInstance(globals, isForGarbling); // popToBucket 
		negatePopToBucket = new INVERTER(globals);
		andCalcValudOut = AND_2_1.newInstance(globals, isForGarbling);
		orCalcCarryOut = OR_2_1.newInstance(globals, isForGarbling);
		//reshareValid = new SHARE(globals, 1);
		unshareCarry = new XOR_2_1(globals);
		
		int i = 0;
		subCircuits[i++] = unshareEntry;
		subCircuits[i++] = unshareEntryNodePrefix;
		subCircuits[i++] = comparePrefixes;
		subCircuits[i++] = xorToCreateZeroEntry;
		subCircuits[i++] = muxToBucket;
//		subCircuits[i++] = muxToStash;
		
//		subCircuits[i++] = reShareToBucket;
//		subCircuits[i++] = reShareToStash;
//		subCircuits[i++] = reshareCarry;
//		subCircuits[i++] = reshareValid;
		
		subCircuits[i++] = unshareValid;
		subCircuits[i++] = andPopToBucket;
		subCircuits[i++] = negatePopToBucket;
		subCircuits[i++] = andCalcValudOut;
		subCircuits[i++] = orCalcCarryOut;
		
		subCircuits[i++] = unshareCarry;
		
	}

	@Override
	protected void connectWires() {

		//unshare prefix
		for (int i = 0; i < prefixLenBits; ++i){
			// UNSHARE prefix!
			inputWires[ENTRY_NODE_PREFIX0 + i].connectTo(unshareEntryNodePrefix.inputWires, unshareEntryNodePrefix.X(i));
			inputWires[ENTRY_NODE_PREFIX1 + i].connectTo(unshareEntryNodePrefix.inputWires, unshareEntryNodePrefix.Y(i));
			
			// connect to compareWithCarry
			unshareEntryNodePrefix.outputWires[i].connectTo(comparePrefixes.inputWires, i);
			inputWires[PATH_PREFIX0 + i].connectTo(comparePrefixes.inputWires, i + prefixLenBits + 1);
		}
		// wire in carry inputs!
		inputWires[CARRY0].connectTo(comparePrefixes.inputWires, comparePrefixes.carryIndexX());
		inputWires[CARRY1].connectTo(comparePrefixes.inputWires, comparePrefixes.carryIndexY());
		
		
		for (int i = 0; i < entryLenBits; ++i){
			// unshare ENTRY
			inputWires[ENTRY0 + i].connectTo(unshareEntry.inputWires, unshareEntry.X(i));
			inputWires[ENTRY1 + i].connectTo(unshareEntry.inputWires, unshareEntry.Y(i));
			
			// create zero entry (UNSHARED xor UNSHARED)
			unshareEntry.outputWires[i].connectTo(xorToCreateZeroEntry.inputWires, xorToCreateZeroEntry.X(i));
			unshareEntry.outputWires[i].connectTo(xorToCreateZeroEntry.inputWires, xorToCreateZeroEntry.Y(i));
			
			// connect to mux inputs; control will be connected few lines later
			
			// mux to bucket
			unshareEntry.outputWires[i].connectTo(muxToBucket.inputWires, i);
			xorToCreateZeroEntry.outputWires[i].connectTo(muxToBucket.inputWires, i + entryLenBits);
		}
		
		// unshare valid and carry
		inputWires[CARRY0].connectTo(unshareCarry.inputWires, 0);
		inputWires[CARRY1].connectTo(unshareCarry.inputWires, 1);
		
		inputWires[VALID0].connectTo(unshareValid.inputWires, 0);
		inputWires[VALID1].connectTo(unshareValid.inputWires, 1);
		
		// calc popToBucket bit
		comparePrefixes.outputWires[0].connectTo(andPopToBucket.inputWires, 0);
		unshareValid.outputWires[0].connectTo(andPopToBucket.inputWires, 1);
		
		// connect control bit to muxes
		andPopToBucket.outputWires[0].connectTo(muxToBucket.inputWires, 2 * entryLenBits);

		// calculate validOut
		andPopToBucket.outputWires[0].connectTo(negatePopToBucket.inputWires, 0);
		negatePopToBucket.outputWires[0].connectTo(andCalcValudOut.inputWires, 0);
		unshareValid.outputWires[0].connectTo(andCalcValudOut.inputWires, 1);
		
		// calculate carryOut 
		unshareCarry.outputWires[0].connectTo(orCalcCarryOut.inputWires, 0);
		andPopToBucket.outputWires[0].connectTo(orCalcCarryOut.inputWires, 1);

	}

	@Override
	protected void defineOutputWires() {
		for (int i = 0; i < entryLenBits; ++i) {
			 muxToBucket.outputWires[i].connectTo(outputWires, ENTRY_OUT_INDEX + i);
		}
		
		// carry bit
		 orCalcCarryOut.outputWires[0].connectTo(outputWires, CARRY_OUT_INDEX);

		// valid bit
		 andCalcValudOut.outputWires[0].connectTo(outputWires, VALID_OUT_INDEX);
	}
	
	public void setEntryAndPrefix(Wire[] wiresA, int startPosA, Wire[] wiresB, int startPosB){
		for (int i = 0; i < entryLenBits; i++){
			wiresA[startPosA + i].connectTo(inputWires, ENTRY0 + i);
			wiresB[startPosB + i].connectTo(inputWires, ENTRY1 + i);
		}
		for (int i = 0; i < prefixLenBits; i++){
			wiresA[startPosA + entryLenBits + i].connectTo(inputWires, ENTRY_NODE_PREFIX0 + i);
			wiresB[startPosB + entryLenBits + i].connectTo(inputWires, ENTRY_NODE_PREFIX1 + i);
		}
	}
	
	public void setPathPrefix(Wire[] wiresA, int startPosA, Wire[] wiresB, int startPosB){
		for (int i=0; i < prefixLenBits ; i++){
			wiresA[startPosA + i].connectTo(inputWires, PATH_PREFIX0 + i);
			wiresB[startPosB + i].connectTo(inputWires, PATH_PREFIX1_ZEROS + i);
		}
	}
	
	public void setCarry(Wire a, Wire b){
		a.connectTo(inputWires, CARRY0);
		b.connectTo(inputWires, CARRY1);
	}
			
	public void setValid(Wire a, Wire b){
		a.connectTo(inputWires, VALID0);
		b.connectTo(inputWires, VALID1);
	}
	
	public Wire getCarry(){
		return outputWires[CARRY_OUT_INDEX];
		//return orCalcCarryOut.outputWires[0];
	}
	
	public int entryOutIndex(){
		return ENTRY_OUT_INDEX;
	}
	
	public Wire validOutWire(){
		return outputWires[VALID_OUT_INDEX];
		//return andCalcValudOut.outputWires[0];
	}
	
	public Wire carryOutWire(){
		return outputWires[CARRY_OUT_INDEX];
	}

	/**
	 * Combine data from one side into inputs for circuit
	 *
	 *     INPUT: [
	 *     Entry0, EntryNodePrefix0, Carry0, PathPrefix0, valid0, randomBits0]
	 *     ] 
	 */
	public BigInteger packInputs(BigInteger entry, 
			BigInteger entryNodePrefix, 
			BigInteger carry, 
			BigInteger pathPrefix,
			BigInteger valid) {
		return 	valid
				.shiftLeft(prefixLenBits).or(pathPrefix)
				.shiftLeft(1).or(carry)
				.shiftLeft(prefixLenBits).or(entryNodePrefix)
				.shiftLeft(entryLenBits).or(entry);
	}
	
	
	static public class Result
	{
		public Result(BigInteger entryToBucket, BigInteger carry, BigInteger valid) {
			this.entryToBucket = entryToBucket;
			this.carry = carry;
			this.valid = valid;
		}
		public BigInteger entryToBucket;
		public BigInteger carry;
		public BigInteger valid;
	}
	
	
	public Result parseResult(BigInteger resultBits)
	{		
		return parseResult(resultBits, entryLenBits);
	}
	
	public static Result parseResult(BigInteger resultBits, int entryLenBits){

		BigInteger entryToBucket = resultBits.and(ENTRY_MASK);
				
		BigInteger carry = resultBits.shiftRight(entryLenBits).and(BigInteger.ONE);
		BigInteger valid = resultBits.shiftRight(entryLenBits).shiftRight(1).and(BigInteger.ONE);		
				
		return new Result(entryToBucket, 
				carry,
				valid);
	}
	
}
