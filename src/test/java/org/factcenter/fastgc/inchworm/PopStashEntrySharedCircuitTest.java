package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.inchworm.PopStashEntryCircuit.Result;
import org.factcenter.fastgc.inchworm.PopStashEntryCircuitTest.Input;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import test.categories.KnownBad;

import java.math.BigInteger;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


// Currently not working after refactoring
@Category(KnownBad.class)
public class PopStashEntrySharedCircuitTest extends GenericOpTest {

	/*
	 * Data for testing the op.
	 */
	final static int prefixLenBits = 8;

	final static int entryLenBits = 16;
	
	final static int stashCapacity = 6;

	PopStashEntrySharedCircuit myCircuit;

	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		myCircuit = new PopStashEntrySharedCircuit(globals, entryLenBits, prefixLenBits, stashCapacity);
		return myCircuit;
	}

	@Override
	int getNumberOfInputs() { 
		return PopStashEntrySharedCircuit.calcInputBits(entryLenBits, prefixLenBits, stashCapacity)/2; 
	}

	/*-
	 * ----------------------------------------------------------------
	 *                      Data Handlers 
	 * ----------------------------------------------------------------
	 */

//	@Test
//	public void testPackInputs(){
//		BigInteger pathPrefix = BigInteger.valueOf(0xaa);
//		BigInteger value = BigInteger.valueOf(0);
//		BigInteger entryPrefix = BigInteger.valueOf(0xee);
//		BigInteger[] stashEntries = new BigInteger[] {value, value, value, value,value,value};
//		BigInteger[] entryPrefixes = new BigInteger[] {entryPrefix, entryPrefix, entryPrefix, entryPrefix,entryPrefix,entryPrefix};
//		boolean[] validBits = new boolean[] {true,true,true,true,true,true};
//		BigInteger actual = myCircuit.packInput(pathPrefix, stashEntries , entryPrefixes, validBits);
//		System.out.println(actual.toString(16));
//		String expected = "3dc0001ee0000f700007b80003dc0001ee0000aa";		
//		assertEquals(expected, actual.toString(16));
//	}
	
	
	@Test
	public void testPositive() throws Exception{
				
		BigInteger zero = BigInteger.ZERO;
		BigInteger pathPrefix = BigInteger.valueOf(0x03);
		
		BigInteger[] stashEntriesA = new BigInteger[] {BigInteger.valueOf(0xaaa1), BigInteger.valueOf(0xaaa2), BigInteger.valueOf(0xaaa3),
													   BigInteger.valueOf(0xaaa4), BigInteger.valueOf(0xaaa5), BigInteger.valueOf(0xaaa6)};
		BigInteger[] entryPrefixesA = new BigInteger[] {BigInteger.valueOf(0x01), BigInteger.valueOf(0x02),BigInteger.valueOf(0x03),
														BigInteger.valueOf(0x04),BigInteger.valueOf(0x05),BigInteger.valueOf(0x06)};
		boolean[] validBitsA = new boolean[] {true,true,true,true,true,true};
		
		BigInteger[] stashEntriesB = new BigInteger[] {zero,zero,zero,zero,zero,zero};
		BigInteger[] entryPrefixesB = new BigInteger[] {zero,zero,zero,zero,zero,zero};
		boolean[] validBitsB = new boolean[] {false,false,false,false,false,false};
		
		
		BigInteger x = myCircuit.packInput(pathPrefix, stashEntriesA, entryPrefixesA, validBitsA, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO);
		BigInteger y = myCircuit.packInput(pathPrefix, stashEntriesB, entryPrefixesB, validBitsB, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO);
		logger.debug("y={}", y.toString(16));
		

		Future<BigInteger> clientThread = runClient(x);
		BigInteger actual = runServer(y);
		
		clientThread.get();
		
		Result parsedResult = myCircuit.parseResult(actual);
		logger.debug("Actual:");
		logResult(parsedResult);
			
		assertEquals(BigInteger.valueOf(0xaaa3), parsedResult.entryToBucket);
		assertTrue(areEqual(new boolean[] {true,true,false,true,true,true}, parsedResult.validBits));
		assertEquals(true, parsedResult.entryValidBit);
	}
	
	@Test
	public void testNegative() throws Exception{
		BigInteger pathPrefix = BigInteger.valueOf(0x01);
		BigInteger value = BigInteger.valueOf(0xffff);
		BigInteger entryPrefix = BigInteger.valueOf(0x00);
		BigInteger[] stashEntries = new BigInteger[] {value, value, value, value,value,value};
		BigInteger[] entryPrefixes = new BigInteger[] {entryPrefix, entryPrefix, entryPrefix, entryPrefix,entryPrefix,entryPrefix};
		boolean[] validBits = new boolean[] {true,true,true,true,true,true};
		
		BigInteger z = BigInteger.ZERO;
		BigInteger one = BigInteger.ONE;
		BigInteger entryRandA = BigInteger.valueOf(0xaaaa);
		BigInteger entryRandB = BigInteger.valueOf(0xffff);
		BigInteger validRandBitsA = BigInteger.valueOf(0x00);
		InputWithRandom inputA = new InputWithRandom(pathPrefix, stashEntries, entryPrefixes, validBits,entryRandA,validRandBitsA,z);
		InputWithRandom inputB = new InputWithRandom(z, 
				new BigInteger[]{z,z,z,z,z,z},
				new BigInteger[]{z,z,z,z,z,z},
				new boolean[]{false,false,false,false,false,false},
				entryRandB,
				z,z);
		
		inputB.pathPrefix = inputA.pathPrefix;
		
		BigInteger x = myCircuit.packInput(inputA.pathPrefix, inputA.stashEntries, inputA.entryPrefixes, inputA.validBits, inputA.validRandBits, inputA.entryRandBits, inputA.entryValidRandBits);
		BigInteger y = myCircuit.packInput(inputB.pathPrefix, inputB.stashEntries, inputB.entryPrefixes, inputB.validBits, inputB.validRandBits, inputB.entryRandBits, inputB.entryValidRandBits);
		
		System.out.println(x.toString(16));
		System.out.println(y.toString(16));
		
		Future<BigInteger> clientThread = runClient(x);
		BigInteger actual = runServer(y);
		clientThread.get();
		
		Result expected = calcExpected(inputA,inputB);
		logger.debug("Expected:");
		logResult(expected);
		
		Result parsedResult = myCircuit.parseResult(actual);
		logger.debug("Actual:");
		logResult(parsedResult);
			
		assertEquals(expected.entryToBucket.toString(16), parsedResult.entryToBucket.toString(16));
		assertTrue(areEqual(expected.validBits, parsedResult.validBits));
		assertEquals(expected.entryValidBit, parsedResult.entryValidBit);
	}
	
	@Test
	public void autoTest() throws Exception{
		for (int i = 0; i < 1000; i++){
			InputWithRandom inputA = generateRandomInputWithRandom();
			logger.debug("inputA");
			logInput(inputA);
			InputWithRandom inputB = generateRandomInputWithRandom();
			logger.debug("inputB");
			logInput(inputB);
			
			inputB.pathPrefix = inputA.pathPrefix;
			
			BigInteger x = myCircuit.packInput(inputA.pathPrefix, inputA.stashEntries, inputA.entryPrefixes, inputA.validBits, inputA.validRandBits, inputA.entryRandBits, inputA.entryValidRandBits);
			BigInteger y = myCircuit.packInput(inputB.pathPrefix, inputB.stashEntries, inputB.entryPrefixes, inputB.validBits, inputB.validRandBits, inputB.entryRandBits, inputB.entryValidRandBits);
			
			Future<BigInteger> clientThread = runClient(x);
			BigInteger actual = runServer(y);
			clientThread.get();
			
			Result parsedResult = myCircuit.parseResult(actual);
			logger.debug("Actual:");
			logResult(parsedResult);
			
			Result expected = calcExpected(inputA,inputB);
			logger.debug("Expected:");
			logResult(expected);
			
			assertEquals(expected.entryToBucket, parsedResult.entryToBucket);
			assertTrue(areEqual(expected.validBits, parsedResult.validBits));
			assertEquals(expected.entryValidBit, parsedResult.entryValidBit);
		}
		
	}
	
	static public class InputWithRandom extends PopStashEntryCircuitTest.Input
	{
		public InputWithRandom(BigInteger pathPrefix,
				BigInteger[] stashEntries, BigInteger[] entryPrefixes,
				boolean[] validBits, BigInteger entryRandBits, BigInteger validRandBits,
				BigInteger entryValidRandBits) {
			super(pathPrefix, stashEntries, entryPrefixes, validBits);
			this.entryRandBits = entryRandBits;
			this.validRandBits = validRandBits;
			this.entryValidRandBits = entryValidRandBits;
		}

		BigInteger entryRandBits;
		BigInteger validRandBits;
		BigInteger entryValidRandBits;
	}
	
	public InputWithRandom generateRandomInputWithRandom(){
		Input input = PopStashEntryCircuitTest.generateRandomInput();
		BigInteger entryRandBits = new BigInteger(entryLenBits, rand);
		BigInteger validRandBits = new BigInteger(stashCapacity, rand);
		BigInteger entryValidRandBits = new BigInteger(1, rand);
		return new InputWithRandom(input.pathPrefix, input.stashEntries, input.entryPrefixes, input.validBits, entryRandBits, validRandBits, entryValidRandBits);
	}
	
	private Result calcExpected(InputWithRandom inputA, InputWithRandom inputB) {
		
		Result res = PopStashEntryCircuitTest.calcExpected(inputA, inputB);
		//BigInteger entryMask = BigInteger.ONE.shiftLeft(entryLenBits).subtract(BigInteger.ONE);
		res.entryToBucket = res.entryToBucket.xor(inputA.entryRandBits).xor(inputB.entryRandBits);
				
		for (int i = 0 ; i < stashCapacity; i++){
			boolean valRandA = inputA.validRandBits.and(BigInteger.ONE).equals(BigInteger.ONE);
			boolean valRandB = inputB.validRandBits.and(BigInteger.ONE).equals(BigInteger.ONE);
			
			res.validBits[i] = res.validBits[i] ^ valRandA ^ valRandB;
			
			inputA.validRandBits = inputA.validRandBits.shiftRight(1);
			inputB.validRandBits = inputB.validRandBits.shiftRight(1);
			
		}
		
		boolean entryValidRandom = inputA.entryValidRandBits.xor(inputB.entryValidRandBits).equals(BigInteger.ONE);
		res.entryValidBit = res.entryValidBit ^ entryValidRandom; 
		
		return res;
		
	}


	static private boolean areEqual(boolean[] validBits, boolean[] validBits2) {
		return PopStashEntryCircuitTest.areEqual(validBits, validBits2);
	}

	private void logInput(InputWithRandom input){
		PopStashEntryCircuitTest.logInput(input, logger);
		logger.debug("===> randEntry = " + input.validRandBits.toString(16));
		logger.debug("===> randEntry = " + input.entryRandBits.toString(16));
		
	}
	
	private void logResult(Result parsedResult) {
		PopStashEntryCircuitTest.logResult(parsedResult, logger);
	}
	
}