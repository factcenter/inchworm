package org.factcenter.inchworm.debugger;

import jline.console.ConsoleReader;
import jline.console.completer.StringsCompleter;
import org.factcenter.inchworm.Constants.NamedReg;
import org.factcenter.inchworm.MemoryArea;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.app.TwoPcApplication;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;

import static org.factcenter.inchworm.Constants.NamedReg.R_IP;

public class ConsoleDebugger implements Debugger {
	private ConsoleReader console;
	private PrintWriter out;
	private HashSet<Long> breakpoints;
	boolean breakOnNextInstruction;
	
	static HashMap<String, NamedReg> parseNamedRegs() {
		HashMap<String, NamedReg> result = new HashMap<String, NamedReg>();
		for (NamedReg r : NamedReg.values()) {
			result.put(r.toString().split("_")[1].toLowerCase(), r);
		}
		return result;
	}
	
	static HashMap<String, NamedReg> regNamesToNamedRegs = parseNamedRegs();
	
	private TwoPcApplication app;
	
	public ConsoleDebugger(TwoPcApplication app) {
		this();
		this.app = app;
	}
	
	private String DumpRam(VMState state, int start, int end) throws IOException {
		// Compose values from both shares. (we clone because load might
		// give a reference to "real" regs)
		int ramsize = state.getRamSize();
		BitMatrix leftMem = app.getLeftPlayer().getState().getMemory(MemoryArea.Type.TYPE_RAM).load(0, state.getRamSize());
		BitMatrix rightMem = app.getRightPlayer().getState().getMemory(MemoryArea.Type.TYPE_RAM).load(0, state.getRamSize());
		BitMatrix myShare = leftMem.clone();
		myShare.xor(rightMem);
		//BitMatrix myShare = state.getMemory(MemoryArea.Type.TYPE_RAM)
		//		.load(0, state.getRamSize()).clone();
		// Get the share of the other player registers.

		String formatStr = " %0" + String.format("%d", state.getRamWordSize()/4) + "x";
		String formatPtrStr = "  %08x:";
		StringBuilder sb = new StringBuilder("");
		int wordsPerLine = 16;
		int wordSize = state.getRamWordSize();
		for (int i = start; i < Math.min(start+end, state.getRamSize()); ++i) {
			if ((i) % wordsPerLine == 0) {
				sb.append("\n");
				sb.append(String.format(formatPtrStr, i));
			}
			long regValue = myShare.getBits(i * wordSize, wordSize);
			sb.append(String.format(formatStr, regValue));
		}
		return sb.toString();

	}
	
	public ConsoleDebugger() {
		breakpoints = new HashSet<Long>(); 
		try {
			console = new ConsoleReader();
			out = new PrintWriter(console.getOutput());
		}
		catch (IOException e) { 
			
		}
		
		// Some of these aren't implemented yet.
		String[] commands = {"reg", "dumpregs", "step", "continue", "bp", "dbp", "lbp", "local", "locals", "quit", "dumpram"};
		console.addCompleter(new StringsCompleter(commands));
		breakOnNextInstruction = false;
	
	}
	private VMState leftState() {
		return app.getLeftPlayer().getState();
	}
	
	private VMState rightState() {
		return app.getRightPlayer().getState();
	}
	
private String getRegValueForPrinting(int regNo, VMState state) throws IOException {
	Long value = new Long(leftState().getReg(regNo) ^ rightState().getReg(regNo));
	return value.toString() + "(" + String.format("%08X", value) + ")";
}
	
public void beforeInstruction(VMState state, int instructionCounter) {
	try {
		state.getReg(state.getNumRegs() - R_IP.getOffset());
		long ip = app.getLeftPlayer().getState().getReg(state.getNumRegs() - R_IP.getOffset()) ^ app.getRightPlayer().getState().getReg(state.getNumRegs() - R_IP.getOffset());
		if ((instructionCounter != 0) && (!breakpoints.contains(new Long(ip))) && (!breakOnNextInstruction)) {
			return;
		}
		
		breakOnNextInstruction = false;
		
		boolean continueRunning = false;
		
		console.setPrompt(String.format("debugger (%d)> ", ip));
		while (!continueRunning) {
			String line = normalizeInputCommand(console.readLine());
			String[] parts = line.split(" ");
			switch (parts[0]) {
			case "r":
			case "reg":
			{
				if (parts.length != 2) {
					out.println("reg should have exactly one argument - reg number.");
					break;
				}
				
				String regIdStr = parts[1]; // can either be a special register name, a hexadecimal number or a decimal number.
				Integer regNo = -1;
				
				try {
					if (regNamesToNamedRegs.containsKey(regIdStr)){
						// todo: this offset calculation logic appears in a bunch of places, and is always confusing (no need for -1, and the mere existence of this enum).
						// it should be extracted into a method somewhere appropriate.
						regNo = state.getNumRegs() - regNamesToNamedRegs.get(regIdStr).getOffset(); // Offset from end of register file. No need for -1, the offsets are from the first number AFTER the end of the register number.
					}
					else if (regIdStr.toLowerCase().startsWith("0x")) {
						regNo = Integer.parseInt(regIdStr.substring(2), 16);
					}
					else {
						regNo = Integer.parseInt(regIdStr);
					}
				}
				catch (java.lang.NumberFormatException e) {

				}
				
				if (regNo > state.getNumRegs() - 1) {
					out.println("Register " + regNo.toString() + " out of range.");
					break;
				}
				
				if (regNo != -1) {
					out.println(getRegValueForPrinting(regNo, state));
				}
				else {
					out.println("Unknown register " + regIdStr);
				}
			}
				break;
			case "dumpregs":
			{
				dumpRegs(state);
			}
			break;
			case "local":
			{
				if (parts.length != 2) {
					out.println("local should have exactly one argument - reg number.");
					break;
				}
				
				String regIdStr = parts[1]; // can either be a special register name, a hexadecimal number or a decimal number.
				Integer regNo = -1;
				
				try {
					if (regIdStr.toLowerCase().startsWith("0x")) {
						regNo = Integer.parseInt(regIdStr.substring(2), 16);
					}
					else {
						regNo = Integer.parseInt(regIdStr);
					}
				}
				catch (java.lang.NumberFormatException e) {

				}
				
				if (regNo > state.getFrameSize() - 1) {
					out.println("Local register " + regNo.toString() + " out of range.");
					break;
				}
				
				if (regNo != -1) {
					out.println(getRegValueForPrinting(state.getLocalRegisterLocation(regNo), state));
				}
				else {
					out.println("Unknown register " + regIdStr);
				}
			}
				break;
			case "bp":
			{
				if (parts.length != 2) {
					out.println("bp should have exactly one argument - breakpoint address.");
					break;
				}
				String breakInstructionNoStr = parts[1];
				breakpoints.add(new Long(Long.parseLong(breakInstructionNoStr)));
			}
				break;
			case "dbp":
			{
				if (parts.length != 2) {
					out.println("dbp should have exactly one argument - breakpoint address.");
					break;
				}
				String breakInstructionNoStr = parts[1];
				breakpoints.remove(new Long(Long.parseLong(breakInstructionNoStr)));
			}
				break;
			case "s":
			case "step":
			{
				if (parts.length != 1) {
					out.println("step does not have any arugments.");
					break;
				}
				breakOnNextInstruction = true;
				continueRunning = true;
				
			}
				break;
			case "locals":
				if (parts.length != 1) {
					out.println("locals does not have any arugments.");
					break;
				}
				HashMap<Long, Long> regNoToVal = new HashMap<Long, Long>(); 
				for (int i = 0; i < state.getFrameSize(); ++i) {
					regNoToVal.put(new Long(i), new Long(state.getReg(state.getLocalRegisterLocation(i))));
				}
				out.println(regNoToVal);
				break;
			case "lbp":
			{
				if (parts.length != 1) {
					out.println("lbp does not have any arugments.");
					break;
				}
				out.println(breakpoints.toString());
			}
				break;
			case "c":
			case "continue":
			{
				continueRunning = true;
				break;
			}
			case "q":
			case "quit":
			{
				System.exit(1);
				break;
			}
			case "dumpram":
				if ((parts.length != 1) && (parts.length != 3)) {
					out.println("dumpram has 0 or 2 arguments.");
					break;
				}
				if (parts.length == 3) {
					String rangeStartStr = parts[1];
					String rangeEndStr = parts[2];
					Long rangeStart = new Long(Long.parseLong(rangeStartStr));
					Long rangeEnd = new Long(Long.parseLong(rangeEndStr));
					out.println(DumpRam(state, rangeStart.intValue(), rangeEnd.intValue()));
				}
				else {
					out.println(DumpRam(state, 0, state.getRamSize()));
				}
				
				break;
			default:
				out.println("Unknown command.");
				break;
			}
		}
	}
	catch (IOException e) { 
		
	}
}

public void dumpRegs(VMState state) throws IOException {
	// prints the registers in ascending order.
	for (int i=0; i<state.getNumRegs() + 1 - state.getFirstSpecialOffset(); ++i) {
		out.print(new Integer(i).toString() + '=' + getRegValueForPrinting(i, state) + ' ');
	}
	for (String n : regNamesToNamedRegs.keySet()) {
		// todo: again, duplication
		int regNo = state.getNumRegs() - regNamesToNamedRegs.get(n).getOffset();
		out.print(n + '=' + getRegValueForPrinting(regNo, state) + ' ');
		
	}
	out.println();
	out.flush();
}
	
public void beforeOp() {}
public void programEnded() {}
public void outputWritten() {}

private void printRegisterValue(int regNo) {}
private void stepInstruction() {}
private void stepOp() {}
private void printIp() {}
private void printLocalRegisters() {}
private void setBreakpointAt(int ip) {}
private void unsetBreakpointAt(int ip) {}
private void printBreakpoints() {}
private String normalizeInputCommand(String command) {
	return command.replaceAll("\\s+", " ").trim();
}
}

