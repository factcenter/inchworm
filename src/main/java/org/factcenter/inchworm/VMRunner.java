package org.factcenter.inchworm;

import org.factcenter.inchworm.app.InchwormIO;
import org.factcenter.inchworm.debugger.Debugger;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.inchworm.ops.OpDesc;
import org.factcenter.inchworm.ops.Operand;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.util.BitMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.factcenter.inchworm.Constants.NamedReg.R_IP;

/**
 * Class containing utility methods for running 2PC programs in the
 * virtual-machine.
 * 
 * @author GILBZ
 */
public class VMRunner implements Runnable {

	/**
	 * Logger.
	 */
	final Logger logger = LoggerFactory.getLogger(getClass());

	/*-
	 * ----------------------------------------------------------------
	 *                      Private data members 
	 * ----------------------------------------------------------------
	 */

	/**
	 * The randomness source for the ops.
	 */
	private Random rand;


	public int getInstructionCounter() {
		return instructionCounter;
	}

	public void resetInstructionCounter() {
		instructionCounter = 0;
	}

	/**
	 * How many instructions have been executed so far.
	 */
	int instructionCounter;

	/**
	 * A Flag specifying that the runner is currently running.
	 */
	AtomicBoolean isRunning = new AtomicBoolean();

	/**
	 * Records the main execution loop IO exception so that it can be queried
	 * later.
	 */
	String ioExceptionMsg = null;

	// Debugger to call into.
	Debugger debugger;
	/**
	 * @return the rand
	 */
	public Random getRand() {
		return rand;
	}

	/**
	 * Id of player running the VM.
	 */
	private int playerId;


	public int getPlayerId() { return playerId; }

	/**
	 * Handler for player IO (in/out ops).
	 */
	InchwormIO ioHandler;


	public InchwormIO getIoHandler() { return ioHandler; }

	public void setIoHandler(InchwormIO ioHandler) { this.ioHandler = ioHandler; }


	private Channel channel;

	/**
	 * Sets the player's communication channel.
	 */
	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	/**
	 * @return the player's communication channel
	 */
	public Channel getChannel() {
		return channel;
	}

	/**
	 * Channel for Yao's circuits I/O streams.
	 */
	private Channel yaoChannel = null;

	/**
	 * Sets the player's communication channel for Yao's circuits.
	 */
	public void setYaoChannel(Channel channel) {
		this.yaoChannel = channel;
	}

	/**
	 * @return the player's communication channel for Yao's circuits.
	 */
	public Channel getYaoChannel() {
		return yaoChannel;
	}

	/**
	 * VMState of the current player.
	 */
	private VMState state;


	/**
	 * Op implementation.
	 */
	private VMOpImplementation opImpl;


	public VMOpImplementation getOpImpl() {
		return opImpl;
	}


	/*-
	 * ----------------------------------------------------------------
	 *                      Constructor(s)
	 * ----------------------------------------------------------------
	 */

	/**
	 * Constructs a new {@code VMRunner} object.
	 * 
	 * @param playerId
	 *            - the id of the player running the virtual-machine.
	 * @param state
	 *            - the current state of the virtual-machine.
	 *
     * @param opImpl A concrete OP implementation. The
	 *            {@link VMOpImplementation#setParameters(int, VMState, VMRunner, java.util.Random)}
     *               and {@link VMOpImplementation#init()} methods should be called before running code using this
     *               VMRunner.

	 *
	 */
	public VMRunner(int playerId, VMState state, VMOpImplementation opImpl, Random rand, Debugger debugger) {
		this.state = state;
		this.playerId = playerId;
		this.rand = rand;
		this.opImpl = opImpl;
		this.debugger = debugger;
	}

	/*-
	 * ----------------------------------------------------------------
	 *                  Public Methods
	 * ----------------------------------------------------------------
	 */

	/**
     * Restart execution (zero the instruction pointer and the {@link #instructionCounter}).
	 */
	public void restart() throws IOException {
		resetInstructionCounter();
		state.setIp(0);
	}


	/**
	 * Send output to a player.
	 * @param stream
	 * @param value
	 * @throws IOException
	 */
	public void outputToPlayer(long stream, BitMatrix value) throws IOException {
		getIoHandler().output(stream, instructionCounter, value);
	}

	/**
	 * Request input from a player.
	 * @return
	 * @throws IOException
	 */
	public BitMatrix getInputFromPlayer() throws IOException {
		return getIoHandler().input(instructionCounter);
	}

	@Override
	public void run() {
		run(-1);
	}

	/**
	 * Inchworm main execution loop.
	 */
	public void run(int maxSteps) {
		isRunning.set(true);
		try {
			state.setHalt(false);
			for (int i = 0; !state.shouldHalt() && (maxSteps <= 0 || i < maxSteps); ++i) {
				runNextInstruction();
			}
		} catch (IOException e) {
			// Record the exception so that it can be queried later.
			ioExceptionMsg = e.getMessage();
		} finally {
			isRunning.set(false);
			logger.debug("Last instruction counter = {}",
					instructionCounter - 1);
		}
	}

	/*-
	 * ----------------------------------------------------------------
	 *            Private Methods + Op Execution wrappers 
	 * ----------------------------------------------------------------
	 */

	/**
	 * Fetch and execute a single instruction of the loaded program.
	 * 
	 * @return true when program ends, false otherwise.
	 * @throws IOException
	 */
	private boolean runNextInstruction() throws IOException {
		// Set the counter flag registers.
		if (getPlayerId() == 0) {
			state.setCounterFlags(instructionCounter);
		} else {
			state.zeroCounterFlags();
		}

		// Fetch and run next instruction.
		MemoryArea rom = state.getMemory(MemoryArea.Type.TYPE_ROM);
        BitMatrix nextInstructionShare = rom.loadOblivious(state.getNamedReg(R_IP), 1);

		if (debugger != null) {
			debugger.beforeInstruction(state, instructionCounter);
		}

        boolean shouldHalt = setAndExecute(nextInstructionShare, instructionCounter);
		++instructionCounter;
		return shouldHalt;

	}


	private final int getWordLen(MemoryArea.Type area) {
		switch (area) {
		case TYPE_RAM:
			return state.getRamWordSize();
		case TYPE_REG:
			return state.getWordSize();
		case TYPE_ROM:
			return state.getInstByteLen() * 8;
		case TYPE_STACK:
			return state.getStackItemSize();
		}
		assert (false);
		return -1;
	}

	/**
	 * Holds intermediate values during operand dereferencing.
	 */
	static class ArgDereference {
		BitMatrix value;

		/**
         * If true, {@link #value} is known by all parties, otherwise it is a value share..
		 */
		boolean isPublic;

		/**
		 * If null, {@link #value} is the final dereferenced value. Otherwise,
		 * this is the memory area into which value is an index.
		 */
		MemoryArea.Type nextArea;

		/**
		 * Number of words the next dereference should read.
		 */
		int width;

        ArgDereference() { this(null, false, null, 1); }

        ArgDereference(BitMatrix value, boolean isPublic, MemoryArea.Type nextArea, int width) {
			this.isPublic = isPublic;
			this.value = value;
			this.nextArea = nextArea;
			this.width = width;
		}
	}

	/**
	 * Do a single indirection step.
	 * @param cur
	 * @throws IOException
	 */
	private void dereferenceLoadStep(ArgDereference cur) throws IOException {
		MemoryArea mem = state.getMemory(cur.nextArea);

		if (cur.isPublic) {
			cur.value = mem.load((int) cur.value.toInteger(64), cur.width);
			// After the first load, the contents are no longer public.
			cur.isPublic = false;
		} else {
			cur.value = mem.loadOblivious(cur.value, cur.width);
		}
		cur.nextArea = null;
	}

	/**
     * Do a single indirection step (storing a value.
     * (doesn't modify cur)
	 * @param cur
	 * @throws IOException
	 */
    private void dereferenceStoreStep(ArgDereference cur, BitMatrix value) throws IOException {
		MemoryArea mem = state.getMemory(cur.nextArea);

		if (cur.isPublic) {
			mem.store((int) cur.value.toInteger(64), value);
		} else {
			mem.storeOblivious(cur.value, value);
		}
	}

	/**
     * Follow the path, stopping before the final
     * dereference.
	 * @param arg
	 * @return
	 */
    private ArgDereference followIndexPath(Operand arg, BitMatrix[] immediateShares) throws IOException {

		ArgDereference cur = new ArgDereference();

		switch (arg.source) {
		case ARG_IMMEDIATE:
			cur.value = immediateShares[arg.sourceIdx];
			break;
		case ARG_PUBLIC:
			cur.isPublic = true;
			if (arg.publicValueComputer != null)
                    cur.value = arg.publicValueComputer.computeConstant(state, arg.sourceIdx);
			else
                    cur.value = BitMatrix.valueOf(arg.sourceIdx, state.getWordSize());
			break;
		default:
			assert false;
			return null;
		}


		if (arg.path == null || arg.path.length == 0)
			return cur;

		for (int i = 0; i < arg.path.length - 1; ++i) {
			cur.nextArea = arg.path[i];
			dereferenceLoadStep(cur);

		}

		cur.nextArea = arg.path[arg.path.length - 1];
		if (arg.publicValueComputer != null)
			cur.width = arg.publicValueComputer.computeWidth(state, arg.width); // maybe publicvaluecomputer could be useful to getaround the memory read width hack...
		else
			cur.width = arg.width;
		return cur;
	}

    private BitMatrix loadArg(Operand arg, BitMatrix[] immediateShares) throws IOException {
		ArgDereference cur = followIndexPath(arg, immediateShares);
		if (cur.nextArea != null)
			dereferenceLoadStep(cur);
		return cur.value;
	}

    private void storeArg(Operand arg, BitMatrix[] immediateShares, BitMatrix value) throws IOException {
        // We first need to load references until the final one, which tells us where to store.
		ArgDereference cur = followIndexPath(arg, immediateShares);
		assert (cur.nextArea != null); // we can't store into a constant value
		dereferenceStoreStep(cur, value);
	}

	/**
	 * Loops through the current instruction (VM's rNext) and executes the
	 * opcodes.
	 * 
	 * @param rNext
	 *            - the current instruction to execute
	 * @param instructionCounter
	 *            - current instruction counter
	 * @return true when program ends.
	 * @throws IOException
	 */
	private boolean setAndExecute(BitMatrix rNext, int instructionCounter)
			throws IOException {

		/*-
		 * 1) Update VMState with the new instruction.
		 */
		state.setNextInstruction(rNext);
		// dumpRegs(4, 52, String.format("Instruction counter = %d",
		// instructionCounter));

		/*-
		 * 2) Execute the loaded instruction.
		 *    VM should return true when program ends (halt-reg != 0).
		 */
		for (int opNdx = 0; opNdx < state.getnumOpcodes(); opNdx++) {
			if (runOp(opNdx, instructionCounter)) {
				
				OpDesc currOpcode = state.getCurrOpcode();

                BitMatrix[] immShares = state.getOpImmediateShares(currOpcode, opNdx);

                int numInArgs = currOpcode.inArgs == null ? 0 : currOpcode.inArgs.length;
                int numOutArgs = currOpcode.outArgs == null ? 0 : currOpcode.outArgs.length;

				BitMatrix[] inArgs = new BitMatrix[numInArgs];


				logger.trace("  loading inputs", currOpcode.name);
				for (int i = 0; i < numInArgs; ++i) {
					inArgs[i] = loadArg(currOpcode.inArgs[i], immShares);
				}

				logger.trace("  executing op", currOpcode.name);
				OpAction action = getOpImpl().getOpAction(currOpcode.name);

				if (logger.isDebugEnabled()) {
					StringBuilder args = new StringBuilder();

					for (int i = 0; i < numInArgs; ++i) {
						args.append(inArgs[i]);
						if (i < numInArgs - 1)
							args.append(",");
					}
					logger.debug("Calling {}({})", currOpcode.name, args);
				}

				BitMatrix[] outputs = action.doOp(state, inArgs);
				if (this.debugger != null) {
//					System.out.println(((ConsoleDebugger)this.debugger).DumpRam(state, 23, 1) + '\n');
//					System.out.flush();
				}
                // Make sure we have the right number of outputs
				// (we allow more outputs than we need for cases like NEXT,
				// which ignores the flags).
				assert((outputs == null && numOutArgs == 0) || outputs.length >= numOutArgs) :
                        currOpcode.name + " returned " + (outputs == null ? "null" : outputs.length) + " arguments (expected " + numOutArgs + ")";


				if (logger.isDebugEnabled()) {
					if (outputs != null) {
						StringBuilder outs = new StringBuilder();

						for (int i = 0; i < outputs.length; ++i) {
							outs.append(outputs[i]);
							if (i < outputs.length - 1)
								outs.append(",");
						}
						logger.debug("{} returned: {}", currOpcode.name, outs);
					} else
						logger.debug("{} returned null", currOpcode.name);
				}

				logger.trace("  storing outputs", currOpcode.name);
				for (int i = 0; i < numOutArgs; ++i) {
					storeArg(currOpcode.outArgs[i], immShares, outputs[i]);
				}


				logger.debug("finished Op: {}", currOpcode.name);

			}

		}

		return false;
	}

	/*
	private void runStoreMem(Player player, int destRegIndex, long srcReg1Index)
			throws IOException {
		long memAddressShare = readFromRegisters(destRegIndex);
		BitMatrix dataToWrite = ops.getLoad().load(state.wordSize,
				srcReg1Index, state.getRegs());
		player.logger.debug("runStoreMem - dataToWrite {}", dataToWrite.getBits(0, state.wordSize));
		ops.getLoadStoreMem().store((int) memAddressShare, dataToWrite);

	} */

	/*
	private void runLoadMem(Player player, int destRegIndex, long srcReg1Index)
			throws IOException {

		long indexShare = readFromRegisters(srcReg1Index);
		player.logger.debug("runLoadMem - read indexShare {}", indexShare);
		BitMatrix bmMemoryValue = ops.getLoadStoreMem().loadMem(
				(int) indexShare);
		storeToRegs(destRegIndex, bmMemoryValue);
	}
	 */


	/**
	 * Dump the current registers value.
	 * 
	 * @param cntRegs
	 *            - number of regs to dump (0 = dump all).
	 * @param startReg
	 *            - first reg to dump.
	 * @param text
	 *            - the formatted dump caption.
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private void dumpRegs(long cntRegs, long startReg, String text)
			throws IOException {

		if (getPlayerId() == 0) {
			// Compose values from both shares. (we clone because load might give a reference to "real" regs)
			BitMatrix myShare = state.getMemory(MemoryArea.Type.TYPE_REG).load(0, state.getNumRegs()).clone();
			// Get the share of the other player registers.
			BitMatrix shareOther = channel.readObject(BitMatrix.class);
			myShare.xor(shareOther);

			// Loop and output the registers.
			if (cntRegs == 0) {
				cntRegs = state.getNumRegs();
				startReg = 0;
			}
			String formatStr = " %08d";
			String formatPtrStr = "  %%r[%04d]:";
			StringBuilder sb = new StringBuilder(text);
			int regsPerLine = 8;
			int wordSize = state.wordSize;
			for (int i = (int) startReg; i < startReg + cntRegs; ++i) {
				if ((i - startReg) % regsPerLine == 0) {
					logger.debug(sb.toString());
					sb = new StringBuilder();
					sb.append(String.format(formatPtrStr, i));
				}
				long regValue = myShare.getBits(i * wordSize, wordSize);
				sb.append(String.format(formatStr, regValue));
			}
			logger.debug(sb.toString());

		} else {
			// Send the registers share to the other player.
			BitMatrix myShare = state.getMemory(MemoryArea.Type.TYPE_REG).load(0, state.getNumRegs());
			channel.writeObject(myShare);
			channel.flush();
		}

	}

	/**
	 * Check if the current op should be executed.
	 * 
	 * @param currOpIndex
	 *            the op index in the current instruction.
	 * @param instructionCounter
	 *            instruction counter value.
	 * @return true if the current opcode of the VM instruction should be
	 *         executed.
	 */
	private boolean runOp(int currOpIndex, int instructionCounter) {

		if (currOpIndex <= state.getnumOpcodes()) {
			OpDesc currOp = state.opDescs.get(currOpIndex);
			// Remember the opcode name.
			state.setCurrOpcode(currOp);
			if ((instructionCounter % currOp.freq) == 0)
				return true;
		} else {
			state.setCurrOpcode(null);
		}
		return false;

	}
}
