package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import test.categories.Slow;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


@Category(Slow.class)
public class SingleWriteStashCircuitTest extends GenericOpTest {

	/*
	 * Data for testing the op.
	 */
	final static int indexWidth = 8;

	final static int dataWidth = 16;

	SingleWriteStashCircuit myCircuit;

	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		myCircuit = new SingleWriteStashCircuit(globals, indexWidth, dataWidth);
		return myCircuit;
	}

	@Override
	int getNumberOfInputs() {
		//return indexWidth * 3 + dataWidth * 3 + 2 + 2; // index, data, carry, valid
		return SingleWriteStashCircuit.calcInDegree(indexWidth, dataWidth)/2;
	}

	/*-
	 * ----------------------------------------------------------------
	 *                      Data Handlers 
	 * ----------------------------------------------------------------
	 */

	private void logResult(SingleWriteStashCircuit.Result result){
		logger.debug("Result : index={} data={} carry={} valid={}", result.index, result.data.toString(16), result.carry, result.valid);
	}
	
	@Test
	public void testPositive() throws IOException {
		BigInteger indexLeftOld = BigInteger.valueOf(0x01); 
		BigInteger indexRightOld = BigInteger.valueOf(0x00);
		
		BigInteger indexLeftNew = BigInteger.valueOf(0x02);
		BigInteger indexRightNew = BigInteger.valueOf(0x00);

		BigInteger dataLeftOld = BigInteger.valueOf(0xaaaa);
		BigInteger dataRightOld = BigInteger.valueOf(0x0000);

		BigInteger dataLeftNew = BigInteger.valueOf(0xbbbb);
		BigInteger dataRightNew = BigInteger.valueOf(0x0000);

		BigInteger carryLeft = BigInteger.valueOf(0);
		BigInteger carryRight = BigInteger.valueOf(0);
		
		BigInteger rShareLeft = BigInteger.valueOf(0x0);
		BigInteger rShareRight = BigInteger.valueOf(0x0);
		
		BigInteger validLeft = BigInteger.ZERO;
		BigInteger validRight = BigInteger.ZERO;
		
		BigInteger validInBlockLeft = BigInteger.ONE;
		BigInteger validInBlockRight = BigInteger.ZERO;
		
		BigInteger x = myCircuit.packInputs(indexLeftOld, indexLeftNew, dataLeftOld, dataLeftNew,
				carryLeft, validLeft, validInBlockLeft, rShareLeft);
				
		BigInteger y = myCircuit.packInputs(indexRightOld, indexRightNew, dataRightOld,
				dataRightNew, carryRight, validRight, validInBlockRight, rShareRight);

		runClient(x);
		BigInteger result = runServer(y);
		logger.debug("[positivie] result=0x{}", result.toString(16));
		
		SingleWriteStashCircuit.Result res = myCircuit.parseResult(result);
		logger.debug("actual: " + res.toString());
		assertEquals(true, res.carry);
		assertEquals(true, res.valid);
		assertEquals(BigInteger.valueOf(0xbbbb).toString(16), res.data.toString(16));
		assertEquals(res.index, 2);
	}
	
	@Test
	public void testSameIndex() throws IOException {
		BigInteger indexLeftOld = BigInteger.valueOf(0x01); // empty cell!!
		BigInteger indexRightOld = BigInteger.valueOf(0x00);
		
		BigInteger indexLeftNew = BigInteger.valueOf(0x00);
		BigInteger indexRightNew = BigInteger.valueOf(0x01);

		BigInteger dataLeftOld = BigInteger.valueOf(0xaaaa);
		BigInteger dataRightOld = BigInteger.valueOf(0x0000);

		BigInteger dataLeftNew = BigInteger.valueOf(0xbbbb);
		BigInteger dataRightNew = BigInteger.valueOf(0x0000);

		BigInteger carryLeft = BigInteger.valueOf(0);
		BigInteger carryRight = BigInteger.valueOf(0);
		
		BigInteger rShareLeft = BigInteger.valueOf(0x0);
		BigInteger rShareRight = BigInteger.valueOf(0x0);
		
		BigInteger validLeft = BigInteger.ONE;
		BigInteger validRight = BigInteger.ZERO;
		
		BigInteger validInBlockLeft = BigInteger.ONE;
		BigInteger validInBlockRight = BigInteger.ZERO;
		
		
		BigInteger x = myCircuit.packInputs(indexLeftOld, indexLeftNew, dataLeftOld, dataLeftNew,
				carryLeft, validLeft, validInBlockLeft,rShareLeft);
				
		BigInteger y = myCircuit.packInputs(indexRightOld, indexRightNew, dataRightOld,
				dataRightNew, carryRight, validRight, validInBlockRight, rShareRight);

		runClient(x);
		BigInteger result = runServer(y);
		logger.debug("[positivie] result=0x{}", result.toString(16));
		
		SingleWriteStashCircuit.Result res = myCircuit.parseResult(result);
		logger.debug("actual: " + res.toString());
		assertEquals(true, res.carry);
		assertEquals(true, res.valid);
		assertEquals(BigInteger.valueOf(0xbbbb).toString(16), res.data.toString(16));
		assertEquals(res.index, 1);
	}

	@Test
	public void testNoWriteOnCarrySet() throws IOException {
		BigInteger indexLeftOld = BigInteger.valueOf(0x00); // empty cell!!
		BigInteger indexRightOld = BigInteger.valueOf(0x00);
		
		BigInteger indexLeftNew = BigInteger.valueOf(0x51);
		BigInteger indexRightNew = BigInteger.valueOf(0x54);

		BigInteger dataLeftOld = BigInteger.valueOf(0xaaaa);
		BigInteger dataRightOld = BigInteger.valueOf(0x0000);

		BigInteger dataLeftNew = BigInteger.valueOf(0xbbbb);
		BigInteger dataRightNew = BigInteger.valueOf(0x0000);

		BigInteger carryLeft = BigInteger.valueOf(0);
		BigInteger carryRight = BigInteger.valueOf(1);
		
		BigInteger validLeft = BigInteger.ZERO;
		BigInteger validRight = BigInteger.ZERO;
		
		BigInteger rShareLeft = BigInteger.valueOf(0x000000);
		BigInteger rShareRight = BigInteger.valueOf(0x000000);
		
		BigInteger validInBlockLeft = BigInteger.ONE;
		BigInteger validInBlockRight = BigInteger.ZERO;
		
		BigInteger x = myCircuit.packInputs(indexLeftOld, indexLeftNew, dataLeftOld, dataLeftNew,
				carryLeft, validLeft, validInBlockLeft,rShareLeft);
				
		BigInteger y = myCircuit.packInputs(indexRightOld, indexRightNew, dataRightOld,
				dataRightNew, carryRight, validRight, validInBlockRight ,rShareRight);

		runClient(x);
		BigInteger result = runServer(y);
		logger.debug("[positivie] result=0x{}", result.toString(16));
		
		SingleWriteStashCircuit.Result res = myCircuit.parseResult(result);
		logResult(res);
		
		assertEquals(true, res.carry);
		assertEquals(false, res.valid);
		assertEquals(BigInteger.valueOf(0xaaaa).toString(16), res.data.toString(16));
		assertEquals(res.index, 0);
		
	}
	
	@Test
	public void testPassCarry() throws IOException {
		BigInteger indexLeftOld = BigInteger.valueOf(0x01); // non-empty cell
		BigInteger indexRightOld = BigInteger.valueOf(0x00);
		
		BigInteger indexLeftNew = BigInteger.valueOf(0x02);
		BigInteger indexRightNew = BigInteger.valueOf(0x00);

		BigInteger dataLeftOld = BigInteger.valueOf(0xaaaa);
		BigInteger dataRightOld = BigInteger.valueOf(0x0000);

		BigInteger dataLeftNew = BigInteger.valueOf(0xbbbb);
		BigInteger dataRightNew = BigInteger.valueOf(0x0000);

		BigInteger carryLeft = BigInteger.valueOf(1);
		BigInteger carryRight = BigInteger.valueOf(0);
		
		BigInteger validLeft = BigInteger.ZERO;
		BigInteger validRight = BigInteger.ZERO;
		
		BigInteger rShareLeft = BigInteger.valueOf(0x000000);
		BigInteger rShareRight = BigInteger.valueOf(0x000000);
		
		BigInteger validInBlockLeft = BigInteger.ONE;
		BigInteger validInBlockRight = BigInteger.ZERO;
		
		BigInteger x = myCircuit.packInputs(indexLeftOld, indexLeftNew, dataLeftOld, dataLeftNew,
				carryLeft, validLeft, validInBlockLeft,rShareLeft);
				
		BigInteger y = myCircuit.packInputs(indexRightOld, indexRightNew, dataRightOld,
				dataRightNew, carryRight, validRight, validInBlockRight ,rShareRight);
		
		runClient(x);
		BigInteger result = runServer(y);
		logger.debug("[negative] result=0x{}", result.toString(16));

		SingleWriteStashCircuit.Result res = myCircuit.parseResult(result);
		logResult(res);
		assertEquals(true, res.carry);
		assertEquals(false, res.valid);
		assertEquals(BigInteger.valueOf(0xaaaa).toString(16), res.data.toString(16));
		assertEquals(res.index, 1);
		
	}
	
	@Test
	public void testNegative() throws IOException {
		BigInteger indexLeftOld = BigInteger.valueOf(0x01); 
		BigInteger indexRightOld = BigInteger.valueOf(0x00);
		
		BigInteger indexLeftNew = BigInteger.valueOf(0x02);
		BigInteger indexRightNew = BigInteger.valueOf(0x00);

		BigInteger dataLeftOld = BigInteger.valueOf(0xaaaa);
		BigInteger dataRightOld = BigInteger.valueOf(0x0000);

		BigInteger dataLeftNew = BigInteger.valueOf(0xbbbb);
		BigInteger dataRightNew = BigInteger.valueOf(0x0000);

		BigInteger carryLeft = BigInteger.valueOf(0);
		BigInteger carryRight = BigInteger.valueOf(0);
		
		BigInteger validLeft = BigInteger.ZERO;
		BigInteger validRight = BigInteger.ONE;
		
		BigInteger rShareLeft = BigInteger.valueOf(0x000000);
		BigInteger rShareRight = BigInteger.valueOf(0x000000);
		
		BigInteger validInBlockLeft = BigInteger.ONE;
		BigInteger validInBlockRight = BigInteger.ZERO;
		
		BigInteger x = myCircuit.packInputs(indexLeftOld, indexLeftNew, dataLeftOld, dataLeftNew,
				carryLeft, validLeft, validInBlockLeft,rShareLeft);
				
		BigInteger y = myCircuit.packInputs(indexRightOld, indexRightNew, dataRightOld,
				dataRightNew, carryRight, validRight, validInBlockRight ,rShareRight);
		
		runClient(x);
		BigInteger result = runServer(y);
		logger.debug("[negative] result=0x{}", result.toString(16));

		SingleWriteStashCircuit.Result res = myCircuit.parseResult(result);
		logResult(res);
		assertEquals(false, res.carry);
		assertEquals(true, res.valid);
		assertEquals(BigInteger.valueOf(0xaaaa).toString(16), res.data.toString(16));
		assertEquals(res.index, 1);
	}
	
	@Test
	public void multiTest() throws Exception {
		for (int i = 0; i < 100000; ++i) {
			logger.debug("TryNumber {}",i);
			BigInteger indexLeftOld = new BigInteger(indexWidth, rand);
			BigInteger indexRightOld = new BigInteger(indexWidth, rand);

			BigInteger indexLeftNew = new BigInteger(indexWidth, rand);
			BigInteger indexRightNew = new BigInteger(indexWidth, rand);

			BigInteger dataLeftOld = new BigInteger(dataWidth, rand);
			BigInteger dataRightOld = new BigInteger(dataWidth, rand);

			BigInteger dataLeftNew = new BigInteger(dataWidth, rand);
			BigInteger dataRightNew = new BigInteger(dataWidth, rand);

			BigInteger carryLeft = new BigInteger(1,rand);
			BigInteger carryRight = new BigInteger(1,rand);
			
			BigInteger validLeft = new BigInteger(1,rand);
			BigInteger validRight = new BigInteger(1,rand);
			
			BigInteger rShareLeft = new BigInteger(dataWidth, rand);
			BigInteger rShareRight = new BigInteger(dataWidth, rand);

			BigInteger validInBlockLeft = new BigInteger(1,rand);
			BigInteger validInBlockRight = new BigInteger(1,rand);
			
			logger.debug("left="+rShareLeft.toString(16)+" right="+rShareRight.toString(16));
			
			BigInteger x = myCircuit.packInputs(indexLeftOld, indexLeftNew, dataLeftOld, dataLeftNew,
					carryLeft, validLeft,validInBlockLeft ,rShareLeft);
					
			BigInteger y = myCircuit.packInputs(indexRightOld, indexRightNew, dataRightOld,
					dataRightNew, carryRight, validRight,validInBlockRight, rShareRight);

			Future<BigInteger> clientThread = runClient(x);
			BigInteger result = runServer(y);
			SingleWriteStashCircuit.Result expected = computeResult(
					indexLeftOld, indexRightOld,
					indexLeftNew, indexRightNew, 
					dataLeftOld, dataRightOld,
					dataLeftNew, dataRightNew,
					carryLeft, carryRight, 
					validLeft, validRight,
					validInBlockLeft, validInBlockRight,
					rShareLeft, rShareRight);
			
			logger.debug("result=0x{}", result.toString(16));			
			logResult(expected);

			SingleWriteStashCircuit.Result actual = myCircuit.parseResult(result);
			logResult(actual);
			
			assertEquals(expected.valid, actual.valid);
			assertEquals(expected.carry, actual.carry);
			assertEquals(expected.data.toString(16), actual.data.toString(16));
			assertEquals(expected.index, actual.index);
			
			clientThread.get();
		}
	}
	
	@Test
	public void testPackBits(){
		
		BigInteger one = BigInteger.ONE;
		System.out.println(one.shiftLeft(1));
		
		CircuitGlobals globals = new CircuitGlobals(rand, 80);
		SingleWriteStashCircuit singleWriteStash = new SingleWriteStashCircuit(globals, 8, 8);

		BigInteger r = BigInteger.ZERO;
		BigInteger carry = BigInteger.ZERO;
		BigInteger dataNew = new BigInteger(1, new byte[] {-61});//BigInteger.valueOf(-61);//Converters.toBigInt(new byte[] {-61});
		BigInteger dataOld = BigInteger.ZERO;
		BigInteger indexNew = BigInteger.valueOf(0x51);
		BigInteger indexOld = BigInteger.ZERO;
		BigInteger packed = singleWriteStash.packInputs(indexOld, indexNew, dataOld,  dataNew ,  carry         , BigInteger.ZERO, BigInteger.ZERO, r);
		BigInteger packed2 = singleWriteStash.packInputs(indexOld, indexNew, dataOld, dataNew,   BigInteger.ONE, BigInteger.ZERO, BigInteger.ZERO, r);
		
		logger.debug("p1 = {} p2 = {}", packed.toString(16), packed2.toString(16));
		
		assertNotEquals(packed, packed2);
		
	}
	
	private SingleWriteStashCircuit.Result makeResult(BigInteger index, BigInteger data, BigInteger carry, BigInteger valid, BigInteger randBits)
	{
		BigInteger resBits =  (valid.
				shiftLeft(1).or(carry).
				shiftLeft(dataWidth).or(data).
				shiftLeft(indexWidth).or(index))
				.xor(randBits);
		return myCircuit.parseResult(resBits);
	}
	
	private SingleWriteStashCircuit.Result computeResult(BigInteger indexLeftOld, BigInteger indexRightOld,
														 BigInteger indexLeftNew, BigInteger indexRightNew,
														 BigInteger dataLeftOld, BigInteger dataRightOld,
														 BigInteger dataLeftNew, BigInteger dataRightNew,
														 BigInteger carryLeft, BigInteger carryRight,
														 BigInteger validLeft, BigInteger validRight,
														 BigInteger validInBlockLeft, BigInteger validInBlockRight,
														 BigInteger rShareLeft, BigInteger rShareRight) {
		
	
		BigInteger indexOld = indexLeftOld.xor(indexRightOld);
		BigInteger indexNew = indexLeftNew.xor(indexRightNew);
		BigInteger randBits = rShareLeft.xor(rShareRight);
		BigInteger dataOld = dataLeftOld.xor(dataRightOld);
		BigInteger dataNew = dataLeftNew.xor(dataRightNew);
		BigInteger carry = carryLeft.xor(carryRight);
		BigInteger valid = validLeft.xor(validRight);
		BigInteger validInBlock = validInBlockLeft.xor(validInBlockRight);

		if (carry.equals(BigInteger.ONE) || validInBlock.equals(BigInteger.ZERO)){
			// carry set or this is a dummy block - doing nothing
			return makeResult(indexOld, dataOld, carry, valid, randBits);
		}
		if (valid.equals(BigInteger.ZERO) || indexOld.equals(indexNew)){
			// empty cell or our index!, lets write. setting carry and valid
			return makeResult(indexNew, dataNew, BigInteger.ONE, BigInteger.ONE, randBits);
		}
		
		// non empty cell. keeping old value
		return makeResult(indexOld, dataOld, carry, valid, randBits);
		
	}
}