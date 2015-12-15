package org.factcenter.inchworm.asm;

import org.factcenter.inchworm.MemoryArea;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.OpDesc;
import org.factcenter.inchworm.ops.Operand;
import org.factcenter.inchworm.ops.dummy.DummyOPFactory;
import org.factcenter.qilin.comm.SendableInputStream;
import org.factcenter.qilin.util.BitMatrix;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import static org.factcenter.inchworm.Constants.FIRST_FIXED_SPECIAL;
import static org.factcenter.inchworm.Constants.NamedReg;

/**
 * Given a machine state description, disassemble the ROM into human-readable form
 * 
 * @author talm
 * 
 */
public class DisAssembler {
    public static String getReg(VMState state, int regIdx) {
        int numRegs = state.getNumRegs();
        String regText = "";

        NamedReg reg = NamedReg.getNamedReg(numRegs - regIdx);

        if (reg != null) {
            switch (reg) {
                case R_IN:
                    regText = "%in";
                    break;
                case R_OUT1:
                    regText = "%out1";
                    break;
                case R_OUT2:
                    regText = "%out2";
                    break;
                case R_CARRY:
                    regText = "%carry";
                    break;
                case R_ZERO:
                    regText = "%zero";
                    break;
                case R_IP:
                    regText = "%ip";
                    break;
                case R_NOP:
                    regText = "%nop";
                    break;
                case R_CTRL:
                    regText = "%ctrl";
                    break;
                case R_SIGN:
                    regText = "%sign";
                    break;
                case R_OVERFLOW:
                    regText = "%overflow";
                    break;
                case R_FLAGS:
                    regText = "%flags";
                    break;
                default:
                    assert(false); // missed something
            }
        } else {
            regText = getRegName(state, regIdx, numRegs);
            if (regText == null) {
                if (regIdx > 32)
                    regText = String.format("%%r[%02xh]", regIdx);
                else
                    regText = String.format("%%r%d", regIdx);
            }
        }
        return regText;
    }

	/**
	 * Return the name name of the passed counter / frame register or
	 * null otherwise.
	 * @param state
	 * @param regIndex
	 * @param numRegs
	 * @return
	 */
	private static String getRegName(VMState state, int regIndex, int numRegs) {
		String regName = null;
		
		int indexCtrHigh = numRegs - (FIRST_FIXED_SPECIAL.getOffset() + 1);
		int indexCtrLow = indexCtrHigh - state.getCounterFlags().size() + 1;
		if ((regIndex >= indexCtrLow) && (indexCtrHigh >= regIndex)) {
			// This is a counter flag
			regName = String.format("%%ctr%d",
					state.getCounterFlags().get(numRegs - regIndex - FIRST_FIXED_SPECIAL.getOffset() - 1));
		} else {

			int indexLocalHigh = state.getLocalRegisterLocation(0);
			int indexLocalLow = state.getLocalRegisterLocation(state.getFrameSize() - 1);
			;
			if ((regIndex >= indexLocalLow) && (indexCtrHigh >= regIndex)) {
				// This is a local flag
				regName = String.format("%%local%d", indexLocalHigh - regIndex);
			}
		}
		return regName;
	}
	
	
	/**
	 * Writes the dis-assembly of the VMState header information to the specified output stream.
	 * @param state - Inchworm virtual machine state object.
	 * @param out - output stream.
	 */
	public static void headerOutput(VMState state, PrintStream out) {
		ArrayList<OpDesc> ops = state.getOpDescs();

		out.println(".header");
		out.println("\twordsize: " + state.getWordSize());
		out.println("\tregptrsize: " + state.getRegPtrSize());
		out.println("\tromptrsize: " + state.getRomPtrSize());
		out.println("\tramptrsize: " + state.getRamPtrSize());
		out.println("\tstackptrsize: " + state.getStackPtrSize());
		out.println("\tframesize: " + state.getFrameSize());

		ArrayList<Integer> counterFlags = state.getCounterFlags();
		if (counterFlags.size() > 0) {
			out.print("\tcounters:");
			for (Integer modulus : counterFlags) {
				out.print(" ");
				out.print(modulus);
			}
			out.println();
		}
		out.print("\tinstruction:");
		for (OpDesc desc : ops) {
			out.print(" ");
			out.print(desc.name);
			if (desc.freq != 1) {
				out.print(":");
				out.print(desc.freq);
			}
		}
		out.println();
	}

	/**
	 * Writes the dis-assembly of the VMState data (registers) to the specified output stream.
	 * @param state - Inchworm virtual machine state object.
	 * @param out - output stream.
	 */
	public static void dataOutput(VMState state, PrintStream out) throws IOException {
		out.println(".data");
		int regpad = (state.getWordSize() / 4) + ((state.getWordSize() % 4 == 0) ? 0 : 1);
		String formatStr = " 0x%0" + regpad + "x";

		int ptrPad = (state.getRegPtrSize() / 4) + ((state.getRegPtrSize() % 4 == 0) ? 0 : 1);
		String formatPtrStr = "  %%r[0x%0" + ptrPad + "x]:";

		int width = 8;
		for (int i = 0; i < state.getNumRegs(); ++i) {
			if (i % width == 0) {
				out.println();
				out.print(String.format(formatPtrStr, i));
			}
			out.print(String.format(formatStr, state.getReg(i)));
		}
		out.println();
	}

    public static String getOperandDescription(VMState state, Operand arg, BitMatrix[] immediates) {
        String val = "";

        BitMatrix source;


        switch(arg.source) {
            case ARG_IMMEDIATE:
                source = immediates[arg.sourceIdx];
                break;
            case ARG_PUBLIC:
                if (arg.publicValueComputer != null)
                    source = arg.publicValueComputer.computeConstant(state, arg.sourceIdx);
                else
                    source = BitMatrix.valueOf(arg.sourceIdx, state.getWordSize());
                break;
            default:
                assert false;
                return null;
        }

        val = source.toBigInteger(source.getNumCols()).toString();
        if (arg.path != null && arg.path.length > 0) {
            int i = 0;
            if (arg.path[i] == MemoryArea.Type.TYPE_REG) {
                // We have special support for a first register dereference -- since these
                // can be named registers.

                val = getReg(state, (int) source.toInteger(state.getRegPtrSize()));
                ++i;
            }
            for (; i < arg.path.length; ++i) {
                switch(arg.path[i]) {
                    case TYPE_REG:
                        val = String.format("r[%s]", val);
                        break;
                    case TYPE_RAM:
                        val = String.format("m[%s]", val);
                        break;
                    case TYPE_ROM:
                        val = String.format("c[%s]", val);
                        break;
                }
            }
        }

        return val;
    }

	@SuppressWarnings("incomplete-switch")
	/**
	 * Writes the dis-assembly of the VMState code to the specified output stream.
	 * @param state - Inchworm virtual machine state object.
	 * @param out - output stream.
	 * @param skipNops - do not write NOP ops if True.
	 */
	public static void codeOutput(VMState state, PrintStream out, boolean skipNops) throws IOException {
		ArrayList<OpDesc> ops = state.getOpDescs();

		out.println(".code");

        MemoryArea rom = state.getMemory(MemoryArea.Type.TYPE_ROM);

		// Loop over instructions
		for (int i = 0; i < state.getRomSize(); ++i) {
			// Loop over ops
			for (int j = 0; j < ops.size(); ++j) {
				OpDesc opDesc = ops.get(j);
                BitMatrix[] nopArgs = opDesc.getNopArgs(state);
				boolean isNop = true;
                for (int k = 0; k < nopArgs.length; ++k)
                    isNop &= state.getOpArg(i, j, k).equals(nopArgs[k]);

				if (skipNops && isNop)
					continue;

				out.print("\t"); // indent
				out.print(opDesc.name);

				if (isNop) {
					out.println(" nop");
					continue;
				}

                BitMatrix instruction = rom.load(i, 1);
                BitMatrix[] immediates = state.getOpImmediateShares(instruction, opDesc, j);

                if (opDesc.outArgs != null) {
                    for (int k = 0; k < opDesc.outArgs.length; ++k) {
                        String argStr = getOperandDescription(state, opDesc.outArgs[k], immediates);

                        out.print(" " + argStr);
                        if (k < opDesc.outArgs.length - 1)
                            out.print(",");
                        else
                            out.print(" <");
                    }
                }

                if (opDesc.inArgs != null) {
                    for (int k = 0; k < opDesc.inArgs.length; ++k) {
                        String argStr = getOperandDescription(state, opDesc.inArgs[k], immediates);

                        out.print(" " + argStr);
                        if (k < opDesc.inArgs.length - 1)
                            out.print(",");
                    }
                }

				out.println();
			}
			out.println(String.format("--- // %03x:", i + 1));
		}
	}

	/**
	 * Writes the dis-assembly of the VMState to the specified output stream.
	 * @param state - Inchworm virtual machine state object.
	 * @param out - output stream.
	 * @param skipNops - do not write NOP ops if True.
	 */
	public static void disassemble(VMState state, PrintStream out, boolean skipNops) throws IOException {
		headerOutput(state, out);
		dataOutput(state, out);
		codeOutput(state, out, skipNops);
	}

	public static void main(String[] args) {
		VMState state = new VMState();
        state.setMemory(new DummyOPFactory());
		try {
			SendableInputStream in = new SendableInputStream(new FileInputStream("test-asm.ivm"));
			state.load(in);
			in.close();

			DisAssembler.disassemble(state, System.out, true);
		} catch (IOException e) {
			System.err.println(e.toString());
			return;
		}
	}

}
