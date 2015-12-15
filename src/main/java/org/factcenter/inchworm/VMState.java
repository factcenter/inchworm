package org.factcenter.inchworm;

import org.factcenter.inchworm.ops.OpDesc;
import org.factcenter.inchworm.ops.concrete.UnalignedMemoryFactory;
import org.factcenter.qilin.comm.SendableInput;
import org.factcenter.qilin.comm.SendableOutput;
import org.factcenter.qilin.util.BitMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static org.factcenter.inchworm.Constants.*;

/**
 * Class containing the current state of the virtual-machine, and utility
 * methods for manipulating (retrieve / write) the state.
 */
public class VMState {
	final static int FILE_MAGIC = 0x696E6368; // "inch";
	final static int FILE_VERSION = 4; // added counter flags in version 2
										// added stack / call / return in
										// version 3.
                                        // added ramloadsize and ramwordsize in version 4

	final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * List of operations in an instruction.
	 */
	ArrayList<OpDesc> opDescs;

	/**
	 * List of counter flag registers. The <i>i</i>th element of the list is the
	 * modulus of the <i>i</i>th counter flag register (the array is always kept
	 * sorted) A counter flag register with modulus <i>x</i> (referred to as
	 * <code>ctr<i>x</i></code> in the assembly) is set to 1 whenever the
	 * instruction counter (the total number of instructions executed so far) is
	 * 0 mod <i>x</i> and is set to 0 otherwise (the value of the register is
	 * overwritten after every instruction).
	 * 
	 * The actual location of the <i>i</i>th counter flag register is
	 * {@link Constants#FIRST_FIXED_SPECIAL} - <i>i</i> - 1.
	 */
	ArrayList<Integer> counterFlags;

	/**
	 * Current opcode inside the VM instruction.
	 */
	private OpDesc currOpcode;


	/**
	 * Locations of the opcode immediate arguments in the instruction word.
     * operandImmediateStart.get(opNum).get(argNum) will return the starting index (in bits) for
     * the argNum'th immediate argument to the opNum'th op in the instruction.
	 */
    List<List<Integer>> operandImmediateStart;

    /**
     * Memory areas,
     * At the moment there can be at most one memory area of each type.
     */
    EnumMap<MemoryArea.Type, MemoryArea> memAreas;

	/**
	 * Stack pointer share.
	 */
	long rSp;

	/**
	 * Currently executing instruction.
	 */
	BitMatrix rNext;


    /**
	 * Constructs a new {@code VMState} object.
	 */
	public VMState() {
		// Instruction opcodes definitions.
        this.memAreas = new EnumMap<MemoryArea.Type, MemoryArea>(MemoryArea.Type.class);
		this.opDescs = new ArrayList<OpDesc>();
		this.counterFlags = new ArrayList<>();
        this.operandImmediateStart = new ArrayList<>();

		// Pointer sizes.
		setWordSize(DEFAULT_WORDSIZE);

		setRamWordSize(DEFAULT_RAMWORDSIZE);
        setRamLoadSize(DEFAULT_RAMLOADSIZE);
		setRegPtrSize(DEFAULT_REGPTRSIZE);
		setRomPtrSize(DEFAULT_ROMPTRSIZE);
		setRamPtrSize(DEFAULT_RAMPTRSIZE);
		setStackPtrSize(DEFAULT_STACKPTRSIZE);
		setFrameSize(DEFAULT_FRAMESIZE);
	}

	/**
	 * Size of register in bits.
	 */
	int wordSize;
	
	/**
	 * Size of addressable RAM word in bits.
	 */
	int ramWordSize;
	/**
	 * Size of pointer to register in bits
	 */
	int regPtrSize;

	/**
	 * Size of external RAM pointer in bits.
	 */
	int ramPtrSize;

    /**
     * number of RAM words to load/store in a single load/store instruction.
     */
    int ramLoadSize;

	/**
	 * Size of pointer to ROM (program memory) in bits.
	 */
	int romPtrSize;

	/**
	 * Size of pointer to STACK in bits.
	 */
	int stackPtrSize;

	/**
	 * Number of local registers in a single frame.
	 */
	int cntLocalRegisters;

	/**
	 * Number of bits per instruction. Note that {@link #instLen} is
	 * {@link #instructionBitSize}/8 rounded up to the nearest integer
	 * (instructions are byte-aligned in the ROM}).
	 */
	int instructionBitSize;

	/**
	 * Number of bytes in an instruction.
	 */
	int instLen;


    /**
     * The secure computation requested a halt.
     */
    boolean shouldHalt;


    /**
     * Has the computation requested a halt?
     * @return
     */
    public boolean shouldHalt() {
        return shouldHalt;
    }

    /**
     * Set (or reset) the halt flag.
     * @param shouldHalt
     */
    public void setHalt(boolean shouldHalt) {
        this.shouldHalt = shouldHalt;
    }




	/**
	 * Sets the size of register in bits.
	 * 
	 * @param wordSize
	 *            - size of register.
	 */
	public void setWordSize(int wordSize) {
		this.wordSize = wordSize;
	}
	
	public void setRamWordSize(int wordSize) {
		this.ramWordSize = wordSize;
	}

    public void setRamLoadSize(int loadSize) { this.ramLoadSize = loadSize; }

	/**
	 * Sets the pointer to register size in bits
	 * 
	 * @param ptrSize
	 *            - size of pointer
	 */
	public void setRegPtrSize(int ptrSize) {
		regPtrSize = ptrSize;
	}

	/**
	 * Set the size of external ROM pointer in bits.
	 * 
	 * @param ptrSize
	 *            - size of pointer to ROM
	 */
	public void setRomPtrSize(int ptrSize) {
		romPtrSize = ptrSize;
	}

	/**
	 * Set the size of external RAM pointer in bits.
	 * 
	 * @param ptrSize
	 *            - size of pointer to RAM
	 */
	public void setRamPtrSize(int ptrSize) {
		ramPtrSize = ptrSize;
	}

	/**
	 * Set the size of the stack pointer in bits.
	 * 
	 * @param ptrSize
	 *            - size of pointer to stack
	 */
	public void setStackPtrSize(int ptrSize) {
		stackPtrSize = ptrSize;
	}

	/**
	 * Set the number of local registers in a single stack frame.
	 * 
	 * @param numRegs
	 *            - number of local registers.
	 */
	public void setFrameSize(int numRegs) {
		cntLocalRegisters = numRegs;
	}

	/**
	 * Offset of the first special register from the end of the register file
	 * (registers below this are 'general purpose')
	 */
	public int getFirstSpecialOffset() {
		return FIRST_FIXED_SPECIAL.getOffset() + counterFlags.size()
				+ cntLocalRegisters;
	}

	/**
	 * Retrieve the counter flag registers
	 * 
	 * @return - ArrayList containing the defined counter-flags.
	 */
	public ArrayList<Integer> getCounterFlags() {
		return counterFlags;
	}

	/**
	 * Return the offset from the end of the register file of the counter flag
	 * with a specified modulus. Counter flags are stored before the fixed
	 * registers in reverser order (the ith counter is at offset i
	 * {@link Constants#FIRST_FIXED_SPECIAL}+index+1).
	 * 
	 * @param modulus
	 * @return the index of the counter flag or a negative value if the counter
	 *         wasn't declared.
	 */
	public int getCounterRegisterLocation(int modulus) {
		int idx = Collections.binarySearch(counterFlags, modulus);
		if (idx < 0)
			return -1;

		return getNumRegs() - (FIRST_FIXED_SPECIAL.getOffset() + idx + 1);
	}

	/**
	 * Return the index into the register file of the local register with a
	 * specified index. Local registers are stored before the counter registers
	 * in reversed order
	 * 
	 * @param regIndex
	 * @return the index of the register.
	 */
	public int getLocalRegisterLocation(int regIndex) {
		if (regIndex < this.cntLocalRegisters)
			return getNumRegs()
					- (FIRST_FIXED_SPECIAL.getOffset() + counterFlags.size()
							+ regIndex + 1);
		else
			return -1;
	}

    /**
     * Return the index into the register file of the local register with minimal
     * index (this is the actually the <i>largest</i> local register, since they
     * are stored in reversed order).
     *
     * @return the index of the register.
     */
    public int getLocalRegistersBaseIndex() {
        return getLocalRegisterLocation(cntLocalRegisters - 1);
    }

	/**
	 * Add a modulus to the counter flag list. The list is kept in sorted order.
	 * 
	 * @param modulus
	 */
	public void addCounter(int modulus) {
		int idx = Collections.binarySearch(counterFlags, modulus);
		if (idx >= 0)
			return; // counter already exists

		// Add the modulus in the appropriate position to maintain sorted order.
		counterFlags.add(-(idx + 1), modulus);
	}


    /**
     * Returns the actual length of the immediate in bits, as determined
     * by the parameters of this VMState.
     * @param wid "logical" width.
     * @return
     */
    public int getImmediateLength(OpDesc.ImmediateWidth wid) {
        int bitLen;
        switch(wid) {
            case IMM_FLAG:
                bitLen = 1;
                break;
            case IMM_RAMPTR:
                bitLen = ramPtrSize;
                break;
            case IMM_REGPTR:
                bitLen = regPtrSize;
                break;
            case IMM_ROMPTR:
                bitLen = romPtrSize;
                break;
            case IMM_STACKPTR:
                bitLen = stackPtrSize;
                break;
            case IMM_WORD:
            default:
                bitLen = wordSize;
                break;

        }
        return bitLen;
    }


	/**
	 * Add an op to the instruction description.
	 * 
	 * Ops should only be added after the pointer sizes are set.
	 * 
	 * @param opDesc
	 *            - supported opcodes information.
	 */
	public void addOp(OpDesc opDesc) {
		opDescs.add(opDesc);

        int instrLen = 0;

        List<Integer> imStarts = new ArrayList<>();

        for (OpDesc.ImmediateWidth wid : opDesc.immediates) {
            int bitLen = getImmediateLength(wid);
            imStarts.add(instructionBitSize + instrLen);
            instrLen += bitLen;
        }

        operandImmediateStart.add(imStarts);

        instructionBitSize += instrLen;

		instLen = instructionBitSize >>> 3;
		if ((instructionBitSize & 7) != 0)
			++instLen;
	}

	/**
	 * Sets the virtual-machine supported opcodes information.
	 * 
	 * @param opDescs
	 *            - opcodes to set.
	 */
	public void setOpDescs(Collection<OpDesc> opDescs) {
		this.opDescs.clear();
        operandImmediateStart.clear();
		for (OpDesc opDesc : opDescs) {
			addOp(opDesc);
		}
	}

	/**
	 * Return the current {@link MemoryArea} of a specific type.
     * Creates a new area if none existed.
	 */
	public MemoryArea getMemory(MemoryArea.Type memAreaType) {
        return memAreas.get(memAreaType);
	}

    /**
     * Set a specific memory area.
     * @param memAreaType
     * @param mem
     */
    public void setMemory(MemoryArea.Type memAreaType, MemoryArea mem) {
        memAreas.put(memAreaType, mem);
    }

    /**
     * Set all memory areas.
     * @param allAreas
     */
    public void setMemory(Map<MemoryArea.Type, MemoryArea> allAreas) {
        memAreas.clear();
        memAreas.putAll(allAreas);
    }


    /**
     * Set all memory areas using a memory factory.
     * Note that this is currently the only way to allow unaligned access to RAM.
     * @param memFactory
     */
    public void setMemory(MemoryFactory memFactory) {
        memAreas.clear();
        UnalignedMemoryFactory unalignedMemoryFactory = new UnalignedMemoryFactory(memFactory);
        if (ramLoadSize > 1) {
            // RAM allows unaligned access
            unalignedMemoryFactory.allowUnaligned(MemoryArea.Type.TYPE_RAM);
        }

        for (MemoryArea.Type type : MemoryArea.Type.values()) {
            memAreas.put(type, unalignedMemoryFactory.createNewMemoryArea(type));
        }
    }

	/**
	 * Initialize (or Reset) ROM, possibly with new parameters. Call this only
	 * after defining an instruction and setting {@link #regPtrSize} and {@link #romPtrSize}.
	 * 
	 * @param fillNops
	 *            whether to fill the RAM with NOPS (otherwise it will be filled
	 *            with zeros)
	 */
	public void initROM(boolean fillNops) throws IOException {
		// Allocate and init the byte buffer needed for storing program memory
		// by initializing first argument of all instruction opcodes to NOP.
        assert(getRegPtrSize() > 0 && getRomPtrSize() > 0);


        MemoryArea rom = getMemory(MemoryArea.Type.TYPE_ROM);
        assert (rom != null);

        rom.init(getInstByteLen() * 8, getRomSize());

		if (fillNops) {
			// Fill ROM with pointers to NOP register
			BitMatrix instr = rom.load(0, 1); // same instruction everywhere, why load it all the time?
			for (int j = 0; j < getOpDescs().size(); ++j) {
                BitMatrix[] nopArgs = getOpDescs().get(j).getNopArgs(this);
                for (int k = 0; k < nopArgs.length; ++k) {
                    putOpArg(instr, j, k, nopArgs[k]);
                }
			}
			for (int i = 0; i < getRomSize(); ++i) {
                rom.store(i, instr);
			}
		} else
            rom.reset();
	}

	/**
	 * Initialize memory areas (setting sizes and initial contents -- ROM to NOPs, etc).
     * The memory areas must be created and set (using {@link #setMemory(MemoryArea.Type, MemoryArea)}
     * or  {@link #setMemory(java.util.Map)}) before calling this method.
     *
     * Call this only after defining an instruction and setting {@link #regPtrSize} and {@link #romPtrSize}
     *
     * This potentially does IO if the memory implementations require it.
	 */
	public void initMemory() throws IOException {
		// Allocate and init to 0 the byte buffer needed for storing the
		// registers.
		//int regbitsize = getNumRegs() * getWordSize();

		MemoryArea regs = getMemory(MemoryArea.Type.TYPE_REG);
        assert(regs != null);
        regs.init(getWordSize(), getNumRegs());

		// Sanity check for the local registers count.
		if (getNumRegs() - (FIRST_FIXED_SPECIAL.getOffset() + counterFlags.size()) <= cntLocalRegisters) {
			String message = String.format(
					"Number of local registers (%d)  is too big",
					cntLocalRegisters);
			throw new UnsupportedArgException(message);
		}

		// Allocate the stack.
        // TODO: Make the stack a standard MemoryArea
		MemoryArea stack = getMemory(MemoryArea.Type.TYPE_STACK);
        if (stack != null)
            stack.init(cntLocalRegisters * getWordSize() + getRomPtrSize(), getStackSize());
		rSp = 0;

		initROM(true);

        MemoryArea ram = getMemory(MemoryArea.Type.TYPE_RAM);
        if (ram != null)
            ram.init(getRamWordSize() * getRamLoadSize(), getRamSize() / getRamLoadSize());
	}

	/**
	 * Return the index of the next matching Op in the instruction
	 * 
	 * @param start
	 *            index at which to start search
	 * @param op
	 *            the current op
	 * @return the index of the next matching op, -1 if next matching op is
	 *         before current op, -2 if no matching op.
	 */
	public int getNextMatchingOp(int start, OpDesc op) {
		for (int i = start; i < opDescs.size(); ++i) {
			if (opDescs.get(i).equals(op))
				return i;
		}
		for (int i = 0; i < start; ++i) {
			if (opDescs.get(i).equals(op))
				return -1;
		}
		return -2;
	}

	/**
	 * Get the value of a register from the packed array.
	 * 
	 * @param regNum
	 * @return The register value
	 */
	public long getReg(int regNum) throws IOException {
        BitMatrix reg = getMemory(MemoryArea.Type.TYPE_REG).load(regNum, 1);
        return reg.toInteger(getWordSize());
	}

	/**
	 * Set the value of a register in the packed array.
	 * 
	 * @param regNum
	 *            the index of the register to set
	 * @param value
	 *            the value to set.
	 */
	public void putReg(int regNum, BitMatrix value) throws IOException {
        getMemory(MemoryArea.Type.TYPE_REG).store(regNum, value);
	}

    public void putReg(int regNum, long value) throws IOException {
        putReg(regNum, BitMatrix.valueOf(value, wordSize));
    }

	/**
	 * Retrieve a single op argument from ROM. Call this only after
	 * {@link #initMemory()}
	 * 
	 * Note that this is used only for disassembly (during debugging). During
	 * execution, the instruction number is not known to the parties.
	 * 
	 * @param instNum
	 *            the index of the instruction
	 * @param opNum
	 *            the index of the op inside the instruction
	 * @param argNum
	 *            the index of the argument to the op.
	 * @return the requested op argument in the LSBits.
	 */
	public BitMatrix getOpArg(int instNum, int opNum, int argNum) throws IOException {
		OpDesc op = opDescs.get(opNum);

        BitMatrix instr = getMemory(MemoryArea.Type.TYPE_ROM).load(instNum, 1);

		int bitLocation = operandImmediateStart.get(opNum).get(argNum);
        int bitLen = getImmediateLength(op.immediates[argNum]);

        return instr.getSubMatrixCols(bitLocation, bitLen);
	}

	/**
	 * Write a single op argument to ROM
	 * 
	 * @param instNum
	 *            the index of the instruction
	 * @param opNum
	 *            the index of the op inside the instruction
	 * @param argNum
	 *            the index of the argument to the op.
	 * @param val
	 *            the value of the op.
	 */
	public void putOpArg(int instNum, int opNum, int argNum, BitMatrix val) throws IOException {
        MemoryArea rom = getMemory(MemoryArea.Type.TYPE_ROM);
        BitMatrix instr = rom.load(instNum, 1);
		putOpArg(instr, opNum, argNum, val);
        rom.store(instNum, instr);
	}

    /**
     * Write a single op argument to an instruction
     *
     * @param instr
     *            the  instruction to which the value should be written.
     * @param opNum
     *            the index of the op inside the instruction
     * @param argNum
     *            the index of the argument to the op.
     * @param val
     *            the value of the op.
     */
    public void putOpArg(BitMatrix instr, int opNum, int argNum, BitMatrix val) {
        OpDesc op = opDescs.get(opNum);

        int bitLocation = operandImmediateStart.get(opNum).get(argNum);
        int bitLen = getImmediateLength(op.immediates[argNum]);

        instr.setBits(bitLocation, bitLen, val);
    }

	/**
	 * Writes the VMState header info into the compiled binary file (*.ivm).
	 * 
	 * @param out
	 *            - point-to-point communication channel for sending and
	 *            receiving data.
	 * @throws IOException
	 */
	public void storeHeader(SendableOutput out) throws IOException {
		// Initial magic
		out.writeInt(FILE_MAGIC);
		out.writeInt(FILE_VERSION);

		// Write header.
		out.writeInt(getWordSize());
		out.writeInt(getRamWordSize());
		out.writeInt(getRegPtrSize());
		out.writeInt(getRomPtrSize());
		out.writeInt(getRamPtrSize());
        out.writeInt(getRamLoadSize());
		out.writeInt(getStackPtrSize());
		out.writeInt(getFrameSize());

		// Write counter flag moduli
		out.writeInt(counterFlags.size());
		for (Integer modulus : counterFlags) {
			out.writeInt(modulus);
		}

		// Write op descriptions
		out.writeInt(opDescs.size());
		for (OpDesc op : opDescs) {
            out.writeObject(op);
		}
		out.flush();
	}

	/**
	 * Save the VMState as a compiled binary file (*.ivm).
	 * 
	 * @param out
	 *            - point-to-point communication channel for sending and
	 *            receiving data.
	 * @throws IOException
	 */
	public void store(SendableOutput out) throws IOException {
		storeHeader(out);
		out.writeObject(getMemory(MemoryArea.Type.TYPE_REG));
		out.writeObject(getMemory(MemoryArea.Type.TYPE_ROM));
		out.flush();
	}

	/**
	 * Loads the header info from a compiled ivm file into the VMState.
	 * 
	 * @param in
	 *            - point-to-point communication channel for sending and
	 *            receiving data.
	 * @throws IOException
	 */
	public void loadHeader(SendableInput in) throws IOException {
		int magic = in.readInt();
		if (magic != FILE_MAGIC) {
			// TODO: better error handling.
			throw new IOException("Bad file magic!");
		}
		int version = in.readInt();
		if (version != FILE_VERSION) {
			// TODO: better error handling.
			throw new IOException("Unsupported file version!");
		}

		// Read header.
		setWordSize(in.readInt());
		setRamWordSize(in.readInt());
		setRegPtrSize(in.readInt());
		setRomPtrSize(in.readInt());
		setRamPtrSize(in.readInt());
        setRamLoadSize(in.readInt());
		setStackPtrSize(in.readInt());
		setFrameSize(in.readInt());

		// Read counter flag moduli.
		int numCounters = in.readInt();
		counterFlags.clear();
		for (int i = 0; i < numCounters; ++i) {
			addCounter(in.readInt());
		}

		// Read op descriptions.
		int numOps = in.readInt();

		opDescs.clear();
        operandImmediateStart.clear();
		for (int i = 0; i < numOps; ++i) {
            OpDesc op = in.readObject(OpDesc.class);
			addOp(op);
		}
	}

	/**
	 * Checks that both headers have the same properties.
	 * 
	 * @return true if headers match, false otherwise.
	 */
	public boolean compareHeaders(VMState stateOther) {

		if (getWordSize() != stateOther.getWordSize())
			return false;
        if (getRamWordSize() != stateOther.getRamWordSize())
            return false;
        if (getRegPtrSize() != stateOther.getRegPtrSize())
			return false;
		if (getRomPtrSize() != stateOther.getRomPtrSize())
			return false;
		if (getRamPtrSize()!= stateOther.getRamPtrSize())
			return false;
        if (getRamLoadSize()!= stateOther.getRamLoadSize())
            return false;
        if (getStackPtrSize() != stateOther.getStackPtrSize())
			return false;
		if (getFrameSize() != stateOther.getFrameSize())
			return false;
		if (this.getOpDescs().size() != stateOther.getOpDescs().size())
			return false;

		// Compare counter flag moduli.
		for (int i = 0; i < counterFlags.size(); ++i) {
			int modulus1 = counterFlags.get(i);
			int modulus2 = stateOther.counterFlags.get(i);
			if (modulus1 != modulus2)
				return false;
		}

		// Compare all ops.
		for (int i = 0; i < getOpDescs().size(); ++i) {
			OpDesc op1 = getOpDescs().get(i);
			OpDesc op2 = stateOther.getOpDescs().get(i);
            if (!op1.equals(op2))
                return false;
		}

		return true;
	}

	/**
	 * Loads a compiled ivm file into the VMState.
	 * 
	 * @param in
	 *            - point-to-point communication channel for sending and
	 *            receiving data.
	 * @throws IOException
	 */
	public void load(SendableInput in) throws IOException {
		loadHeader(in);
		initMemory();

		setMemory(MemoryArea.Type.TYPE_REG, in.readObject(MemoryArea.class));
		setMemory(MemoryArea.Type.TYPE_ROM, in.readObject(MemoryArea.class));
	}

	/*-
	 * ----------------------------------------------------------------
	 *    Public Methods for getting / setting registers and memory.
	 * ----------------------------------------------------------------
	 */

	/**
	 * Retrieve the immediate value at the specified argument location.
	 * 
	 * @param opIndex
	 *            - the index of the op inside the instruction.
	 * @param argIndex
	 *            - the index of the argument to the op.
	 */
	public BitMatrix getImmediateValue(BitMatrix rNext, int opIndex, int argIndex) {
		int bitLocation = operandImmediateStart.get(opIndex).get(argIndex);

        OpDesc desc = opDescs.get(opIndex);

		return rNext.getSubMatrixCols(bitLocation, getImmediateLength(desc.immediates[argIndex]));
	}


    /**
     * Returns shares of the immediate arguments for a given
     * op.
     * @return
     */
    public BitMatrix[] getOpImmediateShares(BitMatrix rNext, OpDesc opDesc, int opNdx) {
        int numImmediates = opDesc.immediates != null ? opDesc.immediates.length : 0;
        BitMatrix[] immShares = new BitMatrix[numImmediates];

        for (int i = 0; i < numImmediates; ++i) {
            immShares[i] = getImmediateValue(rNext, opNdx, i);
        }

        return immShares;
    }

    /**
     * Call {@link #getOpImmediateShares(BitMatrix, OpDesc, int)} using the
     * currently loaded instruction (in {@link #rNext}).
     */
    public BitMatrix[] getOpImmediateShares(OpDesc opDesc, int opNdx) {
        return getOpImmediateShares(rNext, opDesc, opNdx);
    }

    /**
     * Get the value of a named register.
     * @param reg
     * @return
     * @throws IOException
     */
    public BitMatrix getNamedReg(NamedReg reg) throws IOException {
        return getMemory(MemoryArea.Type.TYPE_REG).load(getNumRegs() - reg.getOffset(), 1);
    }

	/**
	 * Get the value (local share) stored in the instruction pointer register.
	 */
	public long getIp() throws IOException {
		return getReg(getNumRegs() - NamedReg.R_IP.getOffset());
	}

	/**
	 * Set the value stored in the instruction pointer register.
     * Note that this sets the local *share* of the IP; one
     * party should call setIp(0) for the actual IP to have the correct value.
	 * 
	 * @param value
	 *            - the value to set.
	 */
	public void setIp(long value) throws IOException {
		putReg(getNumRegs() - NamedReg.R_IP.getOffset(), value);
	}

	/**
	 * Get the value stored in the stack pointer register.
	 */
	public long getSp() {
		return rSp;
	}

	/**
	 * Set the value of the stack pointer register.
	 * 
	 * @param value
	 *            - the value to set.
	 */
	public void setSp(long value) {
		rSp = value;
	}

	/**
	 * Set all the defined counter flags. The counter flag register with modulus
	 * x is set to 1 if the instructionCounter is 0 mod x, and to 0 otherwise.
	 * This method should be called by one of the parties; the other parties
	 * should call {@link #zeroCounterFlags()} in order to ensure that the
	 * combined shares have the correct value.
	 * 
	 * @param instructionCounter
	 *            - the current instruction counter.
	 */
	public void setCounterFlags(int instructionCounter) throws IOException {
		for (Integer modulus : counterFlags) {
			putReg(getCounterRegisterLocation(modulus),
					((instructionCounter % modulus) == 0) ? 1 : 0);
		}
	}

	/**
	 * Zero all the counter flag registers. This should be called before
	 * executing every instruction by all parties except one; one party should
	 * call {@link #setCounterFlags(int)}.
	 */
	public void zeroCounterFlags() throws IOException {
		for (Integer modulus : counterFlags) {
			putReg(getCounterRegisterLocation(modulus), 0);
		}
	}

	/**
	 * Update VM rNext with the currently executing instruction.
	 * 
	 * @param rNextReal
	 *            - the instruction to set in the virtual-machine.
	 */
	public void setNextInstruction(BitMatrix rNextReal) {
		rNext = rNextReal;
	}


	/**
	 * Returns the VM local registers.
	 */
	public BitMatrix getLocalRegs() throws IOException {
        // We assume they are consecutive in memory, starting with 0
        BitMatrix localRegs = getMemory(MemoryArea.Type.TYPE_REG).load(getLocalRegistersBaseIndex(), cntLocalRegisters);

		return localRegs;
	}

	/**
	 * Sets the VM local registers.
	 */
	public void setLocalRegs(BitMatrix frame) throws IOException {
		// Loop and set the values into the local registers
        getMemory(MemoryArea.Type.TYPE_REG).store(getLocalRegistersBaseIndex(), frame);
	}

	/**
	 * Returns the high part of the specified long number.
	 */
	final public int getHighWord(long number) {

		long result = number >>> wordSize;
		long mask = (1L << wordSize) - 1;
		return (int) (result & mask);
	}

	/**
	 * Returns the low part of the specified long number.
	 */
	final public long getLowWord(long number) {
		long mask = (1L << wordSize) - 1;
		return number & mask;
	}

	/*-
	 * ================================ 
	 *   Trivial Getters and Setters
	 * ================================
	 */

	/**
	 * Return the number of bits in a word (each register is one word).
	 */
	public int getWordSize() {
		return wordSize;
	}


    /**
     * Return the number of bits in a RAM word (may be smaller than register word).
     */
    public int getRamWordSize() { return ramWordSize > 0 ? ramWordSize : wordSize; }

	/**
	 * Return the number of bits needed to hold a pointer to a register.
	 */
	public int getRegPtrSize() {
		return regPtrSize;
	}

	/**
	 * Return the number of of registers.
	 */
	public int getNumRegs() {
		return 1 << regPtrSize;
	}

	/**
	 * Return the number of bits needed to hold a pointer to a word in RAM.
	 * 
	 */
	public int getRamPtrSize() {
		return ramPtrSize;
	}

	/**
	 * Return the RAM size in words (see {@link #getWordSize()})
	 * 
	 */
	public int getRamSize() {
		return 1 << ramPtrSize;
	}

	/**
	 * Return the number of bits needed to hold a pointer to an instruction in
	 * RAM.
	 * 
	 */
	public int getRomPtrSize() {
		return romPtrSize;
	}

	/**
	 * Return the ROM size in instructions.
	 * 
	 */
	public int getRomSize() {
		return 1 << romPtrSize;
	}

	public int getStackPtrSize() {
		return stackPtrSize;
	}

	public int getStackItemSize() {
		return getMemory(MemoryArea.Type.TYPE_STACK).getBlockSize();
	}

	public int getStackSize() {
		return 1 << stackPtrSize;
	}

	/**
	 * @return {@link #cntLocalRegisters}
	 */
	public int getFrameSize() {
		return cntLocalRegisters;
	}

	/**
	 * Size of an instruction in bits.
	 * 
	 * @return
	 */
	public int getInstructionBitSize() {
		return instructionBitSize;
	}

	/**
	 * Return the size of the inchworm instruction in bytes.
     * (this is determined by {@link #instructionBitSize} rounded up to the nearest byte).
	 */
	public int getInstByteLen() {
		return instLen;
	}

	public int getnumOpcodes() {
		return opDescs.size();
	}

	public OpDesc getCurrOpcode() {
		return currOpcode;
	}

	public void setCurrOpcode(OpDesc currOpcode) {
		this.currOpcode = currOpcode;
	}

	public ArrayList<OpDesc> getOpDescs() {
		return opDescs;
	}

    public int getRamLoadSize() {
        return ramLoadSize;
    }
}

