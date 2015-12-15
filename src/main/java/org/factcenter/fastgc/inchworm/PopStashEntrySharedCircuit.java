
package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;
import org.factcenter.fastgc.inchworm.PopStashEntryCircuit.Result;

import java.math.BigInteger;



/**
 * @author mikegarts
 * INPUTA  : [PopStashEntryCircuitA, RandA] 
 * INPUTB  : [PopStashEntryCircuitA, RandA]
 * Output  : Shared {@link PopStashEntryCircuit} output
 */
public class PopStashEntrySharedCircuit extends CompositeCircuit {

	
	static public int randomBitsLen(int stashCapacity, int entryLenBits){
			//validRandom , entryRandom, entryValidRandom
		return (stashCapacity + entryLenBits) + 1;
	}
	
	static public int calcInputBits(int entryLenBits, int prefixLenBits, int stashCapacity){
		//return 2 * ((1 + prefixLenBits + entryLenBits) * stashCapacity + prefixLenBits);
		return PopStashEntryCircuit.calcInputBits(entryLenBits, prefixLenBits, stashCapacity) + 2 * randomBitsLen(stashCapacity, entryLenBits);
	}
	
	static public int calcOutputBits(int entryLenBits, int stashCapacity ){
		return PopStashEntryCircuit.calcOutputBits(entryLenBits, stashCapacity);
	}

	private final int stashCapacity;
	private final int prefixLenBits;
	private final int entryLenBits;

	private final int INPUT_B_OFFSET;
	
	private PopStashEntryCircuit popStashEntryCircuit;
	private SHARE shareOutput;
	
	
	public PopStashEntrySharedCircuit(CircuitGlobals globals, int entryLenBits,
			int prefixLenBits, int stashCapacity) {
		super(globals,
				calcInputBits(entryLenBits, prefixLenBits, stashCapacity), 
				calcOutputBits(entryLenBits, stashCapacity),
				2, // popStashEntryCircuit, shareOutput
				"PopStashEntryCircuit" + "_" + stashCapacity);
		
		this.stashCapacity = stashCapacity;
		this.entryLenBits = entryLenBits;
		this.prefixLenBits = prefixLenBits;
		
		this.INPUT_B_OFFSET = calcInputBits(entryLenBits, prefixLenBits, stashCapacity)/2;
		
		BigInteger.ONE.shiftLeft(entryLenBits).subtract(BigInteger.ONE);
		
		
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		popStashEntryCircuit = new PopStashEntryCircuit(globals, entryLenBits, prefixLenBits, stashCapacity);
		shareOutput = new SHARE(globals, calcOutputBits(entryLenBits, stashCapacity));
		int i = 0;
		subCircuits[i++] = popStashEntryCircuit;
		subCircuits[i++] = shareOutput;
				
	}

	protected void connectWires() {
		// calc result
		int randomBitsOffset = PopStashEntryCircuit.calcInputBits(entryLenBits, prefixLenBits, stashCapacity) / 2;
		
		for (int i = 0 ; i < popStashEntryCircuit.getInDegree()/2; i++){
			inputWires[i].connectTo(popStashEntryCircuit.inputWires, i);
			inputWires[INPUT_B_OFFSET + i].connectTo(popStashEntryCircuit.inputWires, popStashEntryCircuit.getInDegree()/2 + i);
		}
	
		// reshare result
		shareOutput.connectWiresToInputs(inputWires, randomBitsOffset, inputWires, INPUT_B_OFFSET + randomBitsOffset, popStashEntryCircuit.outputWires, 0);

	}

	protected void defineOutputWires() {
		for (int i = 0 ; i < calcOutputBits(entryLenBits, stashCapacity);i++){
			 shareOutput.outputWires[i].connectTo(outputWires, i);
		}
	}
	
	public BigInteger packInput(BigInteger pathPrefix, BigInteger[] stashEntries, BigInteger[] entryPrefixes, boolean[] validBits, 
			BigInteger randValidBits, BigInteger randEntryBits , BigInteger entryValidRand){
		return entryValidRand.
				shiftLeft(stashCapacity).or(randValidBits).
				shiftLeft(entryLenBits).or(randEntryBits). 
				shiftLeft(PopStashEntryCircuit.calcInputBits(entryLenBits, prefixLenBits, stashCapacity)/2).
				or(popStashEntryCircuit.packInput(pathPrefix, stashEntries, entryPrefixes, validBits));
	}
		
	public Result parseResult(BigInteger resultBits){
		return popStashEntryCircuit.parseResult(resultBits);
	}
	
}