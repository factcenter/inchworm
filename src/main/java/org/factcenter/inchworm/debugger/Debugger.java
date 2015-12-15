package org.factcenter.inchworm.debugger;

import org.factcenter.inchworm.VMState;

import java.io.IOException;

public interface Debugger {
	void beforeInstruction(VMState state, int instructionCounter);
	void dumpRegs(VMState s) throws IOException;
	void beforeOp();
	void programEnded();
	void outputWritten();
}
