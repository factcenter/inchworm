package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.AND_2_1;
import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class AND_2_1Test extends GenericOpTest {

	@Override
	Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode) {
		return AND_2_1.newInstance(globals, serverMode);
	}

	@Override
	int getNumberOfInputs() { return 1; }

	@Test
	public void test11() throws IOException {
		runClient(BigInteger.valueOf(1));
		int result = runServer(BigInteger.valueOf(1)).intValue();
		assertEquals(1, result);
	}
	@Test
	public void test01() throws IOException {
		runClient(BigInteger.valueOf(0));
		int result = runServer(BigInteger.valueOf(1)).intValue();
		assertEquals(0, result);
	}
	@Test
	public void test10() throws IOException {
		runClient(BigInteger.valueOf(1));
		int result = runServer(BigInteger.valueOf(0)).intValue();
		assertEquals(0, result);
	}	@Test
	public void test00() throws IOException {
		runClient(BigInteger.valueOf(0));
		int result = runServer(BigInteger.valueOf(0)).intValue();
		assertEquals(0, result);
	}


}

