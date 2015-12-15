package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.inchworm.PopStashEntryCircuit.Result;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import test.categories.KnownBad;

import java.math.BigInteger;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


// Currently not working after refactoring
@Category(KnownBad.class)
public class PopStashEntryCircuitTest extends GenericOpTest {

	/*
	 * Data for testing the op.
	 */
	final static int prefixLenBits = 8;

	final static int entryLenBits = 16;
	
	final static int stashCapacity = 6;

	PopStashEntryCircuit myCircuit;

	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		myCircuit = new PopStashEntryCircuit(globals, entryLenBits, prefixLenBits, stashCapacity);
		return myCircuit;
	}

	@Override
	int getNumberOfInputs() { 
		return PopStashEntryCircuit.calcInputBits(entryLenBits, prefixLenBits, stashCapacity)/2; 
	}

	/*-
	 * ----------------------------------------------------------------
	 *                      Data Handlers 
	 * ----------------------------------------------------------------
	 */

	@Test
	public void testPackInputs(){
		BigInteger pathPrefix = BigInteger.valueOf(0xaa);
		BigInteger value = BigInteger.valueOf(0);
		BigInteger entryPrefix = BigInteger.valueOf(0xee);
		BigInteger[] stashEntries = new BigInteger[] {value, value, value, value,value,value};
		BigInteger[] entryPrefixes = new BigInteger[] {entryPrefix, entryPrefix, entryPrefix, entryPrefix,entryPrefix,entryPrefix};
		boolean[] validBits = new boolean[] {true,true,true,true,true,true};
		BigInteger actual = myCircuit.packInput(pathPrefix, stashEntries , entryPrefixes, validBits);
		System.out.println(actual.toString(16));
		String expected = "3dc0001ee0000f700007b80003dc0001ee0000aa";		
		assertEquals(expected, actual.toString(16));
	}
	
	
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
		
		
		BigInteger x = myCircuit.packInput(pathPrefix, stashEntriesA, entryPrefixesA, validBitsA);
		BigInteger y = myCircuit.packInput(pathPrefix, stashEntriesB, entryPrefixesB, validBitsB);
		logger.debug("y={}", y.toString(16));
		

		Future<BigInteger> clientThread = runClient(x);
		BigInteger actual = runServer(y);
		
		clientThread.get();
		
		Result parsedResult = myCircuit.parseResult(actual);
		logger.debug("Actual:");
		logResult(parsedResult, logger);
			
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
		Input inputA = new Input(pathPrefix, stashEntries, entryPrefixes, validBits);
		Input inputB = new Input(z, new BigInteger[]{z,z,z,z,z,z},new BigInteger[]{z,z,z,z,z,z},new boolean[]{false,false,false,false,false,false} );
		
		inputA.pathPrefix = inputB.pathPrefix;
		
		BigInteger y = myCircuit.packInput(inputA.pathPrefix, inputA.stashEntries, inputA.entryPrefixes, inputA.validBits);
		BigInteger x = myCircuit.packInput(inputB.pathPrefix, inputB.stashEntries, inputB.entryPrefixes, inputB.validBits);
		
		Future<BigInteger> clientThread = runClient(x);
		BigInteger actual = runServer(y);
		clientThread.get();
		
		Result expected = calcExpected(inputA,inputB);
		logger.debug("Expected:");
		logResult(expected, logger);
		
		Result parsedResult = myCircuit.parseResult(actual);
		logger.debug("Actual:");
		logResult(parsedResult, logger);
			
		assertTrue(areEqual(expected.validBits, parsedResult.validBits));
		assertEquals(expected.entryToBucket, parsedResult.entryToBucket);
		assertEquals(expected.entryValidBit, parsedResult.entryValidBit);
	}
	
	@Test
	public void autoTest() throws Exception{
		for (int i = 0; i < 1000; i++){
			Input inputA = generateRandomInput();
			logger.debug("inputA");
			logInput(inputA, logger);
			Input inputB = generateRandomInput();
			logger.debug("inputB");
			logInput(inputB, logger);
			
			inputA.pathPrefix = inputB.pathPrefix;
			
			BigInteger x = myCircuit.packInput(inputA.pathPrefix, inputA.stashEntries, inputA.entryPrefixes, inputA.validBits);
			BigInteger y = myCircuit.packInput(inputB.pathPrefix, inputB.stashEntries, inputB.entryPrefixes, inputB.validBits);
			
			Future<BigInteger> clientThread = runClient(x);
			BigInteger actual = runServer(y);
			clientThread.get();
			
			Result parsedResult = myCircuit.parseResult(actual);
			logger.debug("Actual:");
			logResult(parsedResult, logger);
			
			Result expected = calcExpected(inputA,inputB);
			logger.debug("Expected:");
			logResult(expected, logger);
			
			assertTrue(areEqual(expected.validBits, parsedResult.validBits));
			assertEquals(expected.entryToBucket, parsedResult.entryToBucket);
			assertEquals(expected.entryValidBit, parsedResult.entryValidBit);
		}
		
	}
	
	static public class Input
	{
		public Input(BigInteger pathPrefix, BigInteger[] stashEntries, BigInteger[] entryPrefixes, boolean[] validBits){
			this.pathPrefix = pathPrefix;
			this.stashEntries = stashEntries;
			this.entryPrefixes = entryPrefixes;
			this.validBits = validBits;
		}
		BigInteger pathPrefix;
		BigInteger[] stashEntries;
		BigInteger[] entryPrefixes;
		boolean[] validBits;
	}
	
	static public Input generateRandomInput(){
		BigInteger pathPrefix = new BigInteger(prefixLenBits, rand);
		BigInteger[] stashEntries = new BigInteger[stashCapacity];
		BigInteger[] entryPrefixes = new BigInteger[stashCapacity];
		boolean[] validBits = new boolean[stashCapacity];
		
		for (int i = 0 ; i < stashCapacity ; i++){
			stashEntries[i] = new BigInteger(entryLenBits, rand);
			entryPrefixes[i] = new BigInteger(prefixLenBits, rand);
			validBits[i] = rand.nextBoolean();
		}
		return new Input(pathPrefix, stashEntries, entryPrefixes, validBits);
		
	}
	
	static public Result calcExpected(Input inputA, Input inputB) {
		
		BigInteger carry = BigInteger.ZERO;
		BigInteger resEntry = BigInteger.ZERO;
		boolean[] resValidBits = new boolean[stashCapacity]; 
		boolean entryValid = false;
		for (int i = 0 ; i < stashCapacity ; i++){
			SingleChooseCircuit.Result lineResult = SingleChooseCircuitTest.computeResult(inputA.stashEntries[i], inputB.stashEntries[i], 
																		inputA.entryPrefixes[i], inputB.entryPrefixes[i], 
																		carry, BigInteger.ZERO, 
																		inputA.pathPrefix, inputB.pathPrefix,
																		inputA.validBits[i]==false ? BigInteger.ZERO : BigInteger.ONE, inputB.validBits[i]==false ? BigInteger.ZERO : BigInteger.ONE);
			carry = lineResult.carry;
			resValidBits[i] = lineResult.valid.equals(BigInteger.ZERO) ? false : true;
			resEntry = resEntry.xor(lineResult.entryToBucket);
			entryValid = entryValid | lineResult.carry.equals(BigInteger.ONE);
		}
		return new Result(resEntry, resValidBits, entryValid);
		
	}



	static public boolean areEqual(boolean[] validBits, boolean[] validBits2) {
		if (validBits.length != validBits2.length) return false;
		
		for (int i = 0 ; i < validBits.length ; i++){
			if (validBits[i] != validBits2[i]){
				return false;
			}
		}
		return true;
	}

	static public void logInput(Input input, Logger logger){
		logger.debug("pathPrefix = " + input.pathPrefix.toString(16));
		for (int i = 0 ; i < input.validBits.length; i++){
			logger.debug("=> entry = " + input.stashEntries[i].toString(16));
			logger.debug("==> entryPrefix = " + input.entryPrefixes[i].toString(16));
			logger.debug("===> valid = " + input.validBits[i]);
		}
	}
	
	static public void logResult(Result parsedResult, Logger logger) {
		logger.debug("EntryToBucket = " + parsedResult.entryToBucket.toString(16));
		logger.debug("EntryValidBit = " + parsedResult.entryValidBit);
		for (boolean validBit : parsedResult.validBits){
			logger.debug("===> valdBit = " + validBit);
		}
		
	}
	
}