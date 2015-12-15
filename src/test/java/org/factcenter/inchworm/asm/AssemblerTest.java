package org.factcenter.inchworm.asm;

import org.antlr.runtime.RecognitionException;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.dummy.DummyOPFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class AssemblerTest {
	VMState state;
	Assembler asm;
	
	String fullprog1 =
		".header\n"+
			"wordsize: 13 regptrsize: 8 romptrsize: 8 ramptrsize: 10 counters: 5 9 stackptrsize 5 framesize: 4 instruction: zero xori call return add add mux halt next\n"+
					
		".const\n" +
			
			"label1=15\n" +
			"label2=25\n" +
			
		".data\n" +
			
			"%r0 = 5 6\n"+
		"datalabel1:\n"+
			"%r10 = (label1)\n"+
		"datalabel2: $\n"+
			"%r50 = (datalabel2+5) ($) (datalabel1)\n"+
			
		".code\n"+
			
			"zero %ctr9\n"+
			"xori %r10 < 0x10\n"+
			"call codelabel\n"+
			"return\n"+
			"add %local1 < %local[3], %ctr5\n"+
			"---\n"+
			
		"codelabel:\n"+
			"zero %r10\n"+
			"xori %r30 < 0xffffffff\n"+
			"add %r11 < %r30, %r50\n"+
			"---\n"+
			"";

	String dataOnly =
			".data\n" +
			"%r1 = 7 8 9\n" +
			"%r20: 10 20 30 (codelabel) (datalabel1)\n" +
			"";

	
	void firstAsm()  throws RecognitionException, IOException {
		ByteArrayInputStream in1 = new ByteArrayInputStream(fullprog1.getBytes());
		asm.assemble(in1);
		DisAssembler.disassemble(state, System.out, true);
	}
	
	void secondAsm() throws RecognitionException, IOException {
		ByteArrayInputStream in2 = new ByteArrayInputStream(dataOnly.getBytes());
		asm.assemble(in2, false);
	}
	
	@Before
	public void setUp() throws Exception {
		state = new VMState();
        state.setMemory(new DummyOPFactory());
		asm = new Assembler(state);
	}

	@Test
	public void dataLoadTest() throws RecognitionException, IOException {
		firstAsm();
		assertEquals(5, state.getReg(0));
		assertEquals(6, state.getReg(1));

		secondAsm();
		assertEquals(5, state.getReg(0));
		assertEquals(7, state.getReg(1));
		assertEquals(8, state.getReg(2));
		assertEquals(9, state.getReg(3));
		assertEquals(10, state.getReg(20));
		assertEquals(20, state.getReg(21));
		assertEquals(30, state.getReg(22));
	}
	
	@Test
	public void labelTest() throws RecognitionException, IOException {
		firstAsm();
		assertEquals(15, state.getReg(10));
		assertEquals(16, state.getReg(50));
		assertEquals(51, state.getReg(51));
		assertEquals(10, state.getReg(52));
	}
	
	@Test
	public void labelCarryOverTest() throws RecognitionException, IOException {
		firstAsm();
		secondAsm();
		assertEquals(1, state.getReg(23));
		assertEquals(10, state.getReg(24));
			
	}
		

}
