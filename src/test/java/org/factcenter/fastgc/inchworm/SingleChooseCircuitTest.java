package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.inchworm.SingleChooseCircuit.Result;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import test.categories.KnownBad;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;


// Currently not working after refactoring
@Category(KnownBad.class)
public class SingleChooseCircuitTest extends GenericOpTest {

	/*
	 * Data for testing the op.
	 */
	final static int prefixLenBits = 8;

	final static int entryLenBits = 16;

	SingleChooseCircuit myCircuit;

	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		myCircuit = new SingleChooseCircuit(globals, entryLenBits, prefixLenBits);
		return myCircuit;
	}

	@Override
	int getNumberOfInputs() { 
		return prefixLenBits * 2 + entryLenBits + 2 ;    //(prefixOld,prefixNow) + (EntryIn) + (carry, valid) 
	}

	/*-
	 * ----------------------------------------------------------------
	 *                      Data Handlers 
	 * ----------------------------------------------------------------
	 */

	@Test
	public void testPositive() throws IOException {
		BigInteger entryLeft = BigInteger.valueOf(0xaaaa); 
		BigInteger entryRight = BigInteger.valueOf(0x0000);
		
		BigInteger entryNodePrefixLeft = BigInteger.valueOf(0x01);
		BigInteger entryNodePrefixRight = BigInteger.valueOf(0x00);
		
		BigInteger carryLeft = BigInteger.valueOf(0);
		BigInteger carryRight = BigInteger.valueOf(0);

		BigInteger pathPrefixLeft = BigInteger.valueOf(0x00);
		BigInteger pathPrefixRight = BigInteger.valueOf(0x01);
		
		BigInteger validLeft = BigInteger.ONE;
		BigInteger validRight = BigInteger.ZERO;
				
		BigInteger x = myCircuit.packInputs(entryLeft, entryNodePrefixLeft, carryLeft, pathPrefixLeft, validLeft);
		BigInteger y = myCircuit.packInputs(entryRight, entryNodePrefixRight, carryRight, pathPrefixRight, validRight);

		runClient(x);
		BigInteger result = runServer(y);
		Result parsedResult = myCircuit.parseResult(result);
		logResult(parsedResult);
		
		assertEquals(BigInteger.ONE, parsedResult.carry);
		assertEquals(BigInteger.valueOf(0xaaaa), parsedResult.entryToBucket);
		assertEquals(BigInteger.ZERO, parsedResult.valid);
	}

	@Test
	public void testZeros() throws IOException {
		BigInteger entryLeft = BigInteger.valueOf(0x0000); 
		BigInteger entryRight = BigInteger.valueOf(0x0000);
		
		BigInteger entryNodePrefixLeft = BigInteger.valueOf(0x00);
		BigInteger entryNodePrefixRight = BigInteger.valueOf(0x00);
		
		BigInteger carryLeft = BigInteger.valueOf(0);
		BigInteger carryRight = BigInteger.valueOf(0);

		BigInteger pathPrefixLeft = BigInteger.valueOf(0x00);
		BigInteger pathPrefixRight = BigInteger.valueOf(0x00);
		
		BigInteger validLeft = BigInteger.ONE;
		BigInteger validRight = BigInteger.ZERO;
		
		
		BigInteger x = myCircuit.packInputs(entryLeft, entryNodePrefixLeft, carryLeft, pathPrefixLeft, validLeft);
		BigInteger y = myCircuit.packInputs(entryRight, entryNodePrefixRight, carryRight, pathPrefixRight, validRight);

		runClient(x);
		BigInteger result = runServer(y);
		
		logger.debug("result_bla={}", result.toString(16));
		
		Result parsedResult = myCircuit.parseResult(result);
		logResult(parsedResult);
		
		assertEquals(BigInteger.ONE, parsedResult.carry);
		assertEquals(BigInteger.ZERO, parsedResult.entryToBucket);
		assertEquals(BigInteger.ZERO, parsedResult.valid);
	}
	
	@Test
	public void testCarryOneValidZero() throws IOException {
		BigInteger entryLeft = BigInteger.valueOf(0x99ce); 
		BigInteger entryRight = BigInteger.valueOf(0x0000);
		
		BigInteger entryNodePrefixLeft = BigInteger.valueOf(0xa0);
		BigInteger entryNodePrefixRight = BigInteger.valueOf(0x00);
		
		BigInteger carryLeft = BigInteger.ONE;
		BigInteger carryRight = BigInteger.ZERO;

		BigInteger pathPrefixLeft = BigInteger.valueOf(0x2a);
		BigInteger pathPrefixRight = BigInteger.valueOf(0x00);
		
		BigInteger validLeft = BigInteger.ZERO;
		BigInteger validRight = BigInteger.ZERO;
		
	
		BigInteger x = myCircuit.packInputs(entryLeft, entryNodePrefixLeft, carryLeft, pathPrefixLeft, validLeft);
		BigInteger y = myCircuit.packInputs(entryRight, entryNodePrefixRight, carryRight, pathPrefixRight, validRight);

		runClient(x);
		BigInteger result = runServer(y);
		Result parsedResult = myCircuit.parseResult(result);
		logResult(parsedResult);
		
		assertEquals(BigInteger.ONE, parsedResult.carry);
		assertEquals(BigInteger.ZERO, parsedResult.valid);
		assertEquals(BigInteger.ZERO, parsedResult.entryToBucket);
		
	}
	
	private void logResult(Result result) {
		logger.debug("bucketEntry = {}", result.entryToBucket.toString(16));
		logger.debug("carry = {}", result.carry);
		logger.debug("valid = {}", result.valid);
		
	}

	@Test
	public void testNegative() throws IOException {
		BigInteger entryLeft = BigInteger.valueOf(0xaaaa); 
		BigInteger entryRight = BigInteger.valueOf(0x0000);
		
		BigInteger entryNodePrefixLeft = BigInteger.valueOf(0xbb);
		BigInteger entryNodePrefixRight = BigInteger.valueOf(0x00);
		
		BigInteger carryLeft = BigInteger.valueOf(1);
		BigInteger carryRight = BigInteger.valueOf(0);

		BigInteger pathPrefixLeft = BigInteger.valueOf(0xbb);
		BigInteger pathPrefixRight = BigInteger.valueOf(0x00);
				
		BigInteger validLeft = BigInteger.ONE;
		BigInteger validRight = BigInteger.ZERO;
		
		BigInteger x = myCircuit.packInputs(entryLeft, entryNodePrefixLeft, carryLeft, pathPrefixLeft, validLeft);
		BigInteger y = myCircuit.packInputs(entryRight, entryNodePrefixRight, carryRight, pathPrefixRight, validRight);

		runClient(x);
		BigInteger result = runServer(y);
		Result parsedResult = myCircuit.parseResult(result);
		logResult(parsedResult);
		
		assertEquals(BigInteger.ONE, parsedResult.carry);
		assertEquals(BigInteger.ONE, parsedResult.valid);
		assertEquals(BigInteger.ZERO, parsedResult.entryToBucket);
	}
	
	@Test
	public void multiTest() throws Exception {
		for (int i = 0; i < 10000; ++i) {
			logger.debug("TryNumber {}",i);
			BigInteger entryLeft = new BigInteger(entryLenBits, rand); 
			BigInteger entryRight = new BigInteger(entryLenBits, rand);
			
			BigInteger entryNodePrefixLeft = new BigInteger(prefixLenBits, rand);
			BigInteger entryNodePrefixRight = new BigInteger(prefixLenBits, rand);
			
			BigInteger carryLeft = new BigInteger(1, rand);
			BigInteger carryRight = new BigInteger(1, rand);
			logger.debug("carry left, right={},{}", carryLeft, carryRight);

			BigInteger pathPrefixLeft = new BigInteger(prefixLenBits, rand);
			BigInteger pathPrefixRight = pathPrefixLeft;
			
			BigInteger validLeft = new BigInteger(1, rand);
			BigInteger validRight = new BigInteger(1, rand);
			
			BigInteger x = myCircuit.packInputs(entryLeft, entryNodePrefixLeft, carryLeft, pathPrefixLeft, validLeft);
			BigInteger y = myCircuit.packInputs(entryRight, entryNodePrefixRight, carryRight, pathPrefixRight, validRight);

			Future<BigInteger> clientThread = runClient(x);
			BigInteger result = runServer(y);
			
			logger.debug("x=0x{}", x.toString(16));
			logger.debug("y=0x{}", y.toString(16));
			
			Result parsedResult = myCircuit.parseResult(result);
			logger.debug("Actual:");
			logResult(parsedResult);
			
			
			
			Result expected = computeResult(entryLeft, entryRight,
					entryNodePrefixLeft, entryNodePrefixRight,
					carryLeft, carryRight,
					pathPrefixLeft, pathPrefixRight,
					validLeft, validRight);
			
			
			logger.debug("Expected:");
			logResult(expected);
			
			assertEquals(expected.valid, parsedResult.valid);
			assertEquals(expected.carry, parsedResult.carry);
			assertEquals(expected.entryToBucket, parsedResult.entryToBucket);
			
			clientThread.get();
		}
	}
	
	static public Result makeResult(BigInteger bucketEntry, BigInteger carry, BigInteger valid, int entryLenBits){
		BigInteger resBits =  (valid.
				shiftLeft(1).or(carry).
				shiftLeft(entryLenBits).or(bucketEntry));
		return SingleChooseCircuit.parseResult(resBits,entryLenBits);
	}
	
	static private Result makeResult(BigInteger bucketEntry, BigInteger carry, BigInteger valid)
	{
		return makeResult(bucketEntry, carry, valid, entryLenBits);
	}
	
	static public Result computeResult(BigInteger entryLeft, BigInteger entryRight,
								BigInteger nodePrefixLeft, BigInteger nodePrefixRight,
								BigInteger carryLeft, BigInteger carryRight,
								BigInteger pathPrefixLeft, BigInteger pathPrefixRight,
								BigInteger validLeft, BigInteger validRight) {
		
		BigInteger entry = entryLeft.xor(entryRight);
		BigInteger nodePrefix = nodePrefixLeft.xor(nodePrefixRight);
		BigInteger carry = carryLeft.xor(carryRight);
		BigInteger pathPrefix = pathPrefixLeft; // right one is unused!
		BigInteger valid = validLeft.xor(validRight);
		
//		logger.debug("entry={} nodeprefix={} carry={} pathPrefix={} valid={} randBits={}", 
//				entry.toString(16),
//				nodePrefix.toString(16),
//				carry,
//				pathPrefix.toString(16),
//				valid
//				);
		
		if (BigInteger.ONE.equals(carry)){
			// carry set, not writing! passing carry=ONE
			return makeResult(BigInteger.ZERO, carry, valid);
		}
		
		if (BigInteger.ZERO.equals(valid)){
			// invalid entry, don't touch
			return makeResult(BigInteger.ZERO, carry, valid);
		}
		
		if (nodePrefix.equals(pathPrefix)){ // prefix match
			// pop from stash to bucket, set carry and unset valid
			return makeResult(entry, BigInteger.ONE, BigInteger.ZERO);
		}
		
		// not writing, passing on entry, carry and valid
		return makeResult(BigInteger.ZERO, carry, valid);
				
	}
}