package org.factcenter.inchworm;

import org.antlr.runtime.RecognitionException;
import org.factcenter.inchworm.Constants.NamedReg;
import org.factcenter.inchworm.app.InchwormIO;
import org.factcenter.inchworm.asm.Assembler;
import org.factcenter.inchworm.debugger.Debugger;
import org.factcenter.inchworm.ops.OpDesc;
import org.factcenter.inchworm.ops.dummy.DummyOPFactory;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.util.BitMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * Class representing the player in an Inchworm secure computation session.
 * Players functionality are not symmetrical. Communication is also
 * asymmetrical: The right player is a TCP client while the left is the TCP
 * server.
 * 
 * @author GILBZ
 * 
 */
public class Player implements Runnable {

	/**
	 * Logger.
	 */
	final public Logger logger = LoggerFactory.getLogger(getClass());


	/**
	 * Chooser Client for Oblivious Transfers.
	 */
	OTExtender otExtender;

	// A bound on the number of steps to run.
	int maxSteps;

	/*-
	 * ----------------------------------------------------------------
	 *            Private data members (getters / setters)
	 * ----------------------------------------------------------------
	 */

	/**
	 * Player unique identification value.
	 */
	private int playerId;

	/**
	 * @return the playerId
	 */
	public int getPlayerId() {
		return playerId;
	}

    private MemoryFactory defaultMemoryFactory;

	/**
	 * Player's Inchworm virtual machine state object.
	 */
	private VMState state;

	/**
	 * @return the player's state (for JUnit testing of opcodes implementation).
	 */
	public VMState getState() {
		return state;
	}

	/**
	 * Player's Inchworm virtual machine runner.
	 */
	private VMRunner runner;

	/**
	 * @return the player's runner.
	 */
	public VMRunner getRunner() {
		return runner;
	}

	/**
	 * Player's assembler. Assembler state is saved so that new data/code can be
	 * loaded that references old symbols.
	 */
	private Assembler asm;

	/**
	 * Channel to peer (assumes two-party MPC for now).
	 */
	private Channel toPeer = null;

	/**
	 * Sets the player's communication channel.
	 * 
	 * @param channel
	 *            - point-to-point communication channel.
	 */
	public void setChannel(Channel channel) {
		this.toPeer = channel;
		runner.setChannel(channel);
	}

	/**
	 * Sets the player's communication channel for running Yao's circuits.
	 * 
	 * @param channel
	 *            - point-to-point communication channel.
	 */
	public void setYaoChannel(Channel channel) {
		runner.setYaoChannel(channel);
	}

	public void setMaxSteps(int maxSteps) {
		this.maxSteps = maxSteps;
	}

	/**
	 * assembly source code to run.
	 */
	private InputStream sourceCodeFile;

	/**
	 * data file to run.
	 */
	private InputStream dataFile;

	/*-
	 * ----------------------------------------------------------------
	 *                      Constructor(s)
	 * ----------------------------------------------------------------
	 */

	/**
	 * Constructs a new {@code Player} object.
	 * 
	 * @param playerID
	 *            - unique value that identifies the player.
     * @param state
     *            - the VMState. The state memory areas should be set externally before calling {@link #init()}.
	 * @param sourceCodeFile
	 *            - assembly source code to run.
	 * @param DataFile
	 *            - data file to run.
	 * @param opImpl
	 *            - the set of ops the player VM executes.
	 * @param otExtender
	 *            - OT extender server/client pair.
	 */
	public Player(int playerID, VMState state, InputStream sourceCodeFile,
			InputStream DataFile, VMOpImplementation opImpl,
			OTExtender otExtender, InchwormIO ioHandler, Random rand, Debugger debugger) {

		this.playerId = playerID;
		this.sourceCodeFile = sourceCodeFile;
		this.dataFile = DataFile;
		this.otExtender = otExtender;

		// Instantiate the VMState, VMRunner, and the assembler.
		this.state = state;
		this.runner = new VMRunner(getPlayerId(), this.state, opImpl, rand, debugger);
        this.runner.setIoHandler(ioHandler);
		this.asm = new Assembler(state);

	}

	/*-
	 * ----------------------------------------------------------------
	 *                      Public Methods 
	 * ----------------------------------------------------------------
	 */

	public void init() {
		try {
            VMOpImplementation opImpl = runner.getOpImpl();
            opImpl.setParameters(getPlayerId(), state, runner, runner.getRand());

            if (playerId == 0) {

                if (sourceCodeFile != null) {
				/*-
				 * 1) Load program into the VM.
				 */
                    asm.assemble(sourceCodeFile);
                    sourceCodeFile.close();
                    logger.trace("Source file assembly done.");

                    //
                    // FOR DEBUG ONLY:
                    //
                    // DisAssembler.disassemble(state, System.out, true);

				/*
				 * 2) Send header to right player.
				 */
                    state.storeHeader(toPeer);
                    toPeer.flush();
                    logger.trace("Header info sent to right player.");

				/*-
				 * 3) Read data into the VM.
				 */
                    if (dataFile != null) {
                        loadPlayerData(dataFile, true);
                        logger.trace("Data file loading done.");
                    }
                } else {
                    logger.warn("No source code file for player 0!");
                }
			} else {

				if (sourceCodeFile == null) {

					/*-
					 *  1) Get program header from left player and update the VM accordingly.
					 */
					state.loadHeader(toPeer);
					logger.trace("Received VM header from left player.");
					logger.trace("wordSize = {}", state.getWordSize());
					logger.trace("regPtrSize = {}", state.getRegPtrSize());
					logger.trace("romPtrSize = {}", state.getRomPtrSize());
					logger.trace("ramPtrSize = {}", state.getRamPtrSize());
					logger.trace("stackPtrSize = {}", state.getStackPtrSize());
					logger.trace("frameSize = {}", state.getFrameSize());
					logger.trace("numOps = {}", state.getOpDescs().size());

					// Loop and add all ops to the instruction description.
					for (OpDesc op : state.getOpDescs()) {
						logger.trace("opName = {}, freq = {}", op.name, op.freq);
					}

					// Must be done after setting pointer sizes and instruction
					// set.
					state.initMemory();

					/*-
					 * 2) Set all program memory (rom) to 0.
					 */
					state.initROM(false);
					logger.trace("VM state initialized.");

				} else {
					/*-
					 * 1) Load program into the VM.
					 */
					asm.assemble(sourceCodeFile);
					sourceCodeFile.close();
					logger.trace("Source file assembly done.");

					// Temp workaround for setting the right player program
					// memory to zeroes.
                    state.initROM(false);

					/*
					 * 2) Compare header with left player.
					 */
					if (!compareHeaders())
						throw new MismatchingHeadersException(
								"Player's headers do not match");

					// TODO: Allow "public" programs (programs will be compared
					// as well as headers).

				}

				/*-
				 * 3) Read data into the VM.
				 */

				if (dataFile != null) {
					loadPlayerData(dataFile, true);
					dataFile.close();
					logger.debug("Loaded data file.");
				} else {
					logger.info("No data file.");
				}

			}

            // Initialization may depend on pointer sizes, etc., so it must be called after initMemory.
            opImpl.init();

        } catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (RecognitionException e) {
			e.printStackTrace();
			System.exit(-1);
		}



    }

	/**
	 * This method handles the setup and execution loop of the loaded program,
	 * assuming players functionality are not symmetrical
	 */
	@Override
	public void run() {
		// Start the execution loop of loaded program.
		if (playerId == 0) {
			// todo: fix this hack. find the right place to initialize constant register values. this HAS to be done in
			// the left player only for the non-secure version because if both sides have the same value then
			// combining their secret shares would set the value to 0!
			try {
				// todo: this is a hack, and if this changes, so should the special setting of width in dereferenceLoadStep.
				// we set the destination of nop stores to be the last register word in memory.
				state.getMemory(MemoryArea.Type.TYPE_REG).store(state.getNumRegs()- NamedReg.R_LASTMEMCELL.getOffset(), BitMatrix.valueOf(state.getRamSize()-state.getWordSize() / 8, state.getWordSize()));
				state.getMemory(MemoryArea.Type.TYPE_REG).store(state.getNumRegs()- NamedReg.R_STOREREG_NOP_DST.getOffset(), BitMatrix.valueOf(state.getNumRegs() - NamedReg.R_NOP.getOffset(), state.getWordSize()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		runner.run(maxSteps);
		// todo: hack, vmrunner should report failure in a useful way.
		if (runner.ioExceptionMsg != null) {
			System.exit(-1);
		}
	}

	/**
	 * @return the player's communication channel
	 */
	public Channel getChannel() {
		return toPeer;
	}

	/*-
	 * ----------------------------------------------------------------
	 *                      Private Methods 
	 * ----------------------------------------------------------------
	 */

	/**
	 * Checks that both players have the same program headers
	 * 
	 * @return true if headers match, false otherwise.
	 * @throws IOException
	 */
	private boolean compareHeaders() throws IOException {

		// Get program header from left player.
		VMState tempState = new VMState();
		tempState.loadHeader(toPeer);
		return state.compareHeaders(tempState);

	}

	/**
	 * Load new code into the state. The new code may change the header
	 * (including the word size, number of registers, etc)
	 * 
	 * Closes the codeStream before returning.
	 * 
	 * @param codeStream
	 * @param useProgramSymbols
	 * @param isPublic set to true if both players call this method (they should be using the same code).
	 * 	Otherwise, the other player should call P@link {@link #otherPlayerLoadsCode()}}
	 */
	public void loadNewPlayerCode(InputStream codeStream, boolean useProgramSymbols, boolean isPublic)
			throws RecognitionException, IOException {
		Assembler localAsm = asm;
		if (!useProgramSymbols)
			localAsm = new Assembler(state);

		localAsm.assemble(codeStream, false);
		codeStream.close();
        runner.restart();
		if (isPublic) {
			// TODO: We need to compare code as well.
			if (!compareHeaders())
				throw new MismatchingHeadersException("Player's headers do not match");
		} else {
			state.storeHeader(toPeer);
		}
	}
	
	/**
	 * Call this if the other player is loading "secret" code (using {@link #loadNewPlayerCode(InputStream, boolean, boolean)}).
	 * This method will load the header from the other player, and zero the current player's share of the ROM (allowing the
	 * program to be controlled by the other player).
	 * @throws IOException
	 */
	public void otherPlayerLoadsCode() throws IOException {
		state.loadHeader(toPeer);
		runner.restart();
		state.initROM(false); // fill ROM with zeros
	}

	/**
	 * Method for reading player's data into the VM registers.
	 * 
	 * @param dataStream
	 *            - data file to run.
	 * @param useProgramSymbols
	 *            use the symbols defined in the program file (so the data file
	 *            can reference them). When true, the data file symbols are
	 *            saved (and can be referenced in successive loads).
	 * 
	 * @throws RecognitionException
	 *             , IOException
	 */
	public void loadPlayerData(InputStream dataStream, boolean useProgramSymbols)
			throws IOException, RecognitionException {

		// Create a temporary assembler and VM (with the same basic
		// configuration).
		// We do this to prevent the assembler from overwriting the header or
		// code sections.
		VMState tempState = new VMState();
		tempState.setWordSize(state.getWordSize());
		tempState.setRamWordSize(state.getRamWordSize());
		tempState.setRegPtrSize(state.getRegPtrSize());
		tempState.setRomPtrSize(state.getRomPtrSize());
		tempState.setRamPtrSize(state.getRamSize());

        // This sets the temporary state's register memory to point to the real state's register memory,
        // so the assembler actions that affect registers will affect both.
		tempState.setMemory(MemoryArea.Type.TYPE_REG, state.getMemory(MemoryArea.Type.TYPE_REG));
        tempState.setMemory(MemoryArea.Type.TYPE_ROM, new DummyOPFactory().createNewMemoryArea(MemoryArea.Type.TYPE_ROM));

		Assembler localAsm = useProgramSymbols ? new Assembler(tempState,
				asm.getSymbols()) : new Assembler(tempState);

		localAsm.assemble(dataStream, false);
		dataStream.close();
	}

	/**
	 * Method that reads data into the VM registers.
	 * 
	 * @param dataFile
	 *            - data file to run.
	 * @param asm
	 *            - Inchworm assembler.
	 * @throws org.antlr.runtime.RecognitionException
	 */
	@SuppressWarnings("unused")
	private void loadPlayerData(InputStream dataFile, Assembler asm)
			throws org.antlr.runtime.RecognitionException {

		try {
			/*-
			 * NOTE: This method breaks the code if the loaded assembly
			 *       file contains the header section.
			 */
			asm.assemble(dataFile, false);
			dataFile.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Assembler getAsm() {
		return asm;
	}
}
