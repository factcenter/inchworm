
package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;
import org.factcenter.fastgc.YaoGC.Wire;
import org.factcenter.fastgc.YaoGC.XOR_2_1;

import java.math.BigInteger;



/**
 * @author mikegarts
 * INPUTA  : [PathPrefixA, 				, StashEntryA0,EntryNodePrefixA0,ValidA0 ... ,StashEntryAN,EntryNodePrefixAN,ValidAN] 
 * INPUTB  : [PathPrefixB (=PathPrefixA), StashEntryB0,EntryNodePrefixB0,ValidB0 ... ,StashEntryBN,EntryNodePrefixBN,ValidBN]
 * Output  : [EntryToBucket, ValidOutBit0....ValidOutBitN] 
 */
public class PopStashEntryCircuit extends CompositeCircuit {

	static public int calcInputBits(int entryLenBits, int prefixLenBits, int stashCapacity){
		// twice : prefix + (entry+prefix+validBit)
		return 2 * ((1 + prefixLenBits + entryLenBits) * stashCapacity + prefixLenBits); 
	}
	
	static public int calcOutputBits(int entryLenBits, int stashCapacity ){
		return entryLenBits + stashCapacity + 1; // Entry + stash valid bits
	}

	private final int stashCapacity;
	private final int prefixLenBits;
	private final int entryLenBits;
	private final int INPUT_B_OFFSET;
	
	private SingleChooseCircuit[] singleChooseArray;
	private XOR_2_1 makeZero;
	private XOR_ML_L xorEntryToBucket;
	private int INPUT_A_OFFSET = 0;
	private BigInteger ENTRY_MASK;
		
	public PopStashEntryCircuit(CircuitGlobals globals, int entryLenBits,
								int prefixLenBits, int stashCapacity) {
		super(globals,
				calcInputBits(entryLenBits, prefixLenBits, stashCapacity), 
				calcOutputBits(entryLenBits, stashCapacity),
				stashCapacity + 2, // singleChoose array, makeZero, xorEntryToBucket
				"PopStashEntryCircuit" + "_" + stashCapacity);
		
		this.stashCapacity = stashCapacity;
		this.entryLenBits = entryLenBits;
		this.prefixLenBits = prefixLenBits;
		this.INPUT_B_OFFSET = calcInputBits(entryLenBits, prefixLenBits, stashCapacity)/2;
		
		ENTRY_MASK = BigInteger.ONE.shiftLeft(entryLenBits).subtract(BigInteger.ONE);
		
		
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		int i = 0;
		singleChooseArray = new SingleChooseCircuit[stashCapacity];

		for (i=0; i < stashCapacity; i++){
			SingleChooseCircuit singleChoose = new SingleChooseCircuit(globals, entryLenBits, prefixLenBits); 
			singleChooseArray[i] = singleChoose;
			subCircuits[i] = singleChoose;			
		}
		makeZero = new XOR_2_1(globals);
		// xor each singleChooseOutput. output is entryLenBits-wide
		xorEntryToBucket = new XOR_ML_L(globals, stashCapacity, entryLenBits); 
			
		subCircuits[i++] = makeZero;
		subCircuits[i++] = xorEntryToBucket;
				
	}

	protected void connectWires() {
		// creating zero
		inputWires[0].connectTo(makeZero.inputWires, 0);
		inputWires[0].connectTo(makeZero.inputWires, 1);
		Wire zeroWire = makeZero.outputWires[0];
				
		for (int stashEntryIndex = 0 ; stashEntryIndex < stashCapacity ; stashEntryIndex++){
			
			// pathPrefix + current entry offset
			int currentEntryOffsetA = prefixLenBits + stashEntryIndex * (entryLenBits + prefixLenBits + 1);
			
			// middle of input + pathPrefix + current entry offset
			int currentEntryOffsetB = INPUT_B_OFFSET + currentEntryOffsetA;
			
			int validBitOffsetInEntry = entryLenBits + prefixLenBits;
			
			SingleChooseCircuit singleChoose = singleChooseArray[stashEntryIndex]; 
			
			// connect Entry and EntryNodePrefix (cont. in input)
			singleChoose.setEntryAndPrefix(inputWires, currentEntryOffsetA, inputWires, currentEntryOffsetB);
			
			// connect pathPrefix 
			singleChoose.setPathPrefix(inputWires, INPUT_A_OFFSET , 
					                   inputWires, INPUT_B_OFFSET);	
			
			// connect valid bit
			singleChoose.setValid(inputWires[currentEntryOffsetA + validBitOffsetInEntry], 
					              inputWires[currentEntryOffsetB + validBitOffsetInEntry]);
			
			// connect output to ML-xor
			xorEntryToBucket.setInputLine(singleChoose.outputWires, singleChoose.entryOutIndex(), stashEntryIndex);
			
			if (0 == stashEntryIndex){
				// wire the first carry (zero)
				singleChoose.setCarry(zeroWire, zeroWire);
				singleChooseArray[1].setCarry(singleChoose.getCarry(), zeroWire);
				//singleChooseArray[1].setCarry(zeroWire, zeroWire);
				
			}else if (stashEntryIndex < stashCapacity - 1){ 
				// not the first one and not the last one
				Wire carryOut = singleChoose.getCarry();
				// connect carry from one circuit to next one. starting from the second up to before last
				singleChooseArray[stashEntryIndex + 1].setCarry(carryOut, zeroWire);
			}			
		}
	}

	protected void defineOutputWires() {
		for (int i = 0 ; i < entryLenBits ; i++){
			 xorEntryToBucket.outputWires[i].connectTo(outputWires, i);
		}
		
		for (int stashEntryIndex = 0 ; stashEntryIndex < stashCapacity ; stashEntryIndex++){
			// pathPrefix + current entry offset
			 singleChooseArray[stashEntryIndex].validOutWire().connectTo(outputWires, stashEntryIndex + entryLenBits);
		}
		
		 singleChooseArray[stashCapacity - 1].carryOutWire().connectTo(outputWires, calcOutputBits(entryLenBits, stashCapacity) - 1);
	}
	
	
	/**
	 * Packs the input to a BigInteger as the circuit is supposed to get
	 * pathPrefix+[entry0|entryNodePrefix0|validBit0 ... entryN|entryNodePrefixN|validBitN]
	 * @param pathPrefix
	 * @param stashEntries
	 * @param entryPrefixes
	 * @param validBits
	 * @return
	 */
	public BigInteger packInput(BigInteger pathPrefix, BigInteger[] stashEntries, BigInteger[] entryPrefixes, boolean[] validBits){
		if (stashEntries.length != stashCapacity || entryPrefixes.length != stashCapacity || validBits.length != stashCapacity){
			throw new RuntimeException("Wrong input " + stashCapacity + " " + stashEntries.length + " " + entryPrefixes.length + " " + validBits);
		}
			
		BigInteger packetBits = BigInteger.ZERO;
		for (int i = stashCapacity - 1 ; i >= 0 ; i--){
			packetBits = packetBits.
					shiftLeft(1).or(validBits[i] == true ? BigInteger.ONE : BigInteger.ZERO)
					.shiftLeft(prefixLenBits).or(entryPrefixes[i])
					.shiftLeft(entryLenBits).or(stashEntries[i]);
		}
		return packetBits.shiftLeft(prefixLenBits).or(pathPrefix);
	}
	
	static public class Result{
		public Result(BigInteger entryToBucket, boolean[] validBits, boolean entryValid){
			this.entryToBucket = entryToBucket;
			this.validBits = validBits;
			this.entryValidBit = entryValid;
		}
		public BigInteger entryToBucket;
		public boolean[] validBits;
		public boolean entryValidBit;
	}
	
	public Result parseResult(BigInteger resultBits){
		BigInteger entry = resultBits.and(ENTRY_MASK);
		
		boolean[] validBits = new boolean[stashCapacity];
		resultBits = resultBits.shiftRight(entryLenBits);
		
		for (int i = 0 ; i < stashCapacity ; i++){
			boolean valid = resultBits.and(BigInteger.ONE).equals(BigInteger.ZERO) ? false : true;
			validBits[i] = valid;
			resultBits = resultBits.shiftRight(1);
		}
		boolean entryValidBit = resultBits.and(BigInteger.ONE).equals(BigInteger.ONE);
		return new Result(entry, validBits, entryValidBit);
	}
	
}