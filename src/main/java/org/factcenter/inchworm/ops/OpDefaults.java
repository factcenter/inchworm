package org.factcenter.inchworm.ops;

import org.factcenter.inchworm.MemoryArea;
import org.factcenter.inchworm.VMState;
import org.factcenter.qilin.util.BitMatrix;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.factcenter.inchworm.Constants.NamedReg;
import static org.factcenter.inchworm.Constants.NamedReg.*;
import static org.factcenter.inchworm.ops.OpDesc.*;
import static org.factcenter.inchworm.ops.OpDesc.ImmediateWidth.IMM_FLAG;
import static org.factcenter.inchworm.ops.OpDesc.ImmediateWidth.IMM_ROMPTR;

/**
 * Created by talm on 8/18/14.
 */
public class OpDefaults {
    /**
     * Possible ops in an instruction. Note: If you change this, you must change
     * the corresponding definition in InchwormAsm.g
     * @author talm
     *
     */
    public enum Op {
        OP_ZERO("zero"),
        OP_XORI("xori"),
        OP_ADD("add"),
        OP_SUB("sub"),
        OP_XOR("xor"),
        OP_AND("and"),
        OP_OR("or"),
        OP_ROL("rol"),
        OP_MUX("mux"),
        OP_MUL("mul"),
        OP_DIV("div"),
        OP_NEXT("next"),
        OP_LOAD("load"),
        OP_STORE("store"),
        OP_LOADREG("loadreg"),
        OP_STOREREG("storereg"),
        OP_IN("in"),
        OP_OUT("out"),
        OP_HALT("halt"),
        OP_CALL("call"),
        OP_RETURN("return"),
        OP_ERR("");

        public final String name;

        public String toString() { return name; }

        Op(String name) {
            this.name = name;
        }

        static public Op getEnum(String name) {
            for (Op op : Op.values()) {
                if (op.name.equalsIgnoreCase(name))
                    return op;
            }
            return OP_ERR;
        }
    }


    public final static MemoryArea.Type[] nullPath = new MemoryArea.Type[0];
    public final static MemoryArea.Type[] reg1 = {MemoryArea.Type.TYPE_REG};
    public final static MemoryArea.Type[] reg2 = {MemoryArea.Type.TYPE_REG, MemoryArea.Type.TYPE_REG};
    public final static MemoryArea.Type[] ram1 = {MemoryArea.Type.TYPE_RAM};
    public final static MemoryArea.Type[] ram2 = {MemoryArea.Type.TYPE_REG, MemoryArea.Type.TYPE_RAM};

    public final static Operand[] ARG_IMMEDIATE = {
            new Operand(Operand.ArgSource.ARG_IMMEDIATE, 0, nullPath, 1),
            new Operand(Operand.ArgSource.ARG_IMMEDIATE, 1, nullPath, 1)
    };
    public final static Operand[] ARG_REG = {
            new Operand(Operand.ArgSource.ARG_IMMEDIATE, 0, reg1, 1),
            new Operand(Operand.ArgSource.ARG_IMMEDIATE, 1, reg1, 1),
            new Operand(Operand.ArgSource.ARG_IMMEDIATE, 2, reg1, 1),
            new Operand(Operand.ArgSource.ARG_IMMEDIATE, 3, reg1, 1)
    };

    public final static Operand ARG_REGw2 = new Operand(Operand.ArgSource.ARG_IMMEDIATE, 0, reg1, 2);

    public final static Operand[] ARG_RAM = {
            new Operand(Operand.ArgSource.ARG_IMMEDIATE, 0, ram1, 1),
            new Operand(Operand.ArgSource.ARG_IMMEDIATE, 1, ram1, 1),
    };
    public final static Operand[] ARG_IND_REG = {
            new Operand(Operand.ArgSource.ARG_IMMEDIATE, 0, reg2, 1),
            new Operand(Operand.ArgSource.ARG_IMMEDIATE, 1, reg2, 1),
    };

    public final static Operand[] ARG_IND_RAM = {
            new Operand(Operand.ArgSource.ARG_IMMEDIATE, 0, ram2, 1),
            new Operand(Operand.ArgSource.ARG_IMMEDIATE, 1, ram2, 1),
    };


    public final static Operand.PublicValueComputer NamedRegComputer = new Operand.ConstantWidthValueComputer() {
        @Override
        public BitMatrix computeConstant(VMState state, int idx) {
            return BitMatrix.valueOf(state.getNumRegs() - idx, 32);
        }
    };

    /**
     * A value computer that computes pointers to local registers.
     * Giving a negative width here will compute the width in multiples
     * of the frame size rather than number of registers.
     */
    public final static Operand.PublicValueComputer LocalReg = new Operand.AutoRegisteringValueComputer() {
        @Override
        public BitMatrix computeConstant(VMState state, int idx) {
            return BitMatrix.valueOf(state.getLocalRegisterLocation(idx), state.getRegPtrSize());
        }

        @Override
        public int computeWidth(VMState state, int width) {
            if (width > 0)
                return width;
            else
                return state.getFrameSize() * (-width); // Multiples of the entire frame.
        }
    };

    /**
     * A value computer that computes pointers to the entire local register frame
     * (ignores the source index).
     * Giving a negative width here will compute the width in multiples
     * of the frame size rather than number of registers.
     */
    public final static Operand.PublicValueComputer LocalFrame = new Operand.AutoRegisteringValueComputer() {
        @Override
        public BitMatrix computeConstant(VMState state, int idx) {
            return BitMatrix.valueOf(state.getLocalRegistersBaseIndex(), state.getRegPtrSize());
        }

        @Override
        public int computeWidth(VMState state, int width) {
            if (width > 0)
                return width;
            else
                return state.getFrameSize() * (-width); // Multiples of the entire frame.
        }
    };


    public final static Operand.PublicValueComputer CounterReg = new Operand.ConstantWidthValueComputer() {
        @Override
        public BitMatrix computeConstant(VMState state, int idx) {
            return BitMatrix.valueOf(state.getCounterRegisterLocation(idx), state.getRegPtrSize());
        }
    };

    public final static NopComputer DefaultNop = new AutoRegisteringNopComputer() {
        @Override
        public BitMatrix[] getNopValues(OpDesc op, VMState state) {
            if (op.immediates == null || op.immediates.length == 0)
                return new BitMatrix[0];

            List<BitMatrix> args = new ArrayList<>();

            if (op.immediates != null) {
            	for (int i = 0; i < op.immediates.length; ++i) {
            		ImmediateWidth wid = op.immediates[i];
                    int length = state.getImmediateLength(wid);
                    switch (wid) {
                        case IMM_FLAG:
                            args.add(BitMatrix.valueOf(0, length));
                            break;
                        case IMM_REGPTR:
                        	if ((op.name.equals("store")) && (i == 0)) {
                        		args.add(BitMatrix.valueOf(state.getNumRegs() - R_LASTMEMCELL.getOffset(), length));
                        	}
                        	// todo: this also needs to happen to loadreg...
                        	else if (op.name.equals("storereg") && (i==0)) {
                        		args.add(BitMatrix.valueOf(state.getNumRegs() - R_STOREREG_NOP_DST.getOffset(), length));
                        	}
                        	else if (op.name.equals("load") && (i==1)) {
                        		// load source has to be a valid memory address.
                        		// it's not always like that if it's the nop register as it may contain total garbage.
                        		// so we load from the last memory cell.
                        		args.add(BitMatrix.valueOf(state.getNumRegs() - R_LASTMEMCELL.getOffset(), length));
                        	}
                        	else {
                        		args.add(BitMatrix.valueOf(state.getNumRegs() - R_NOP.getOffset(), length));
                        	}
                            
                            break;
                        case IMM_RAMPTR:
                            // We use the last word in RAM as the RAM NOP destination.
                            args.add(BitMatrix.valueOf(state.getRamSize() - 1, length));
                            break;
                        case IMM_WORD:
                        case IMM_ROMPTR:
                            args.add(BitMatrix.valueOf(0, length));
                            break;
                        default:
                            assert false; // shouldn't happen.

                    }

                }
            }
            return args.toArray(new BitMatrix[args.size()]);
        }
    };


    public static class NamedRegDest extends Operand {
        public NamedRegDest(NamedReg reg, int width) {
            super(ArgSource.ARG_PUBLIC, NamedRegComputer, reg.getOffset(), reg1, width);
        }
        public NamedRegDest(NamedReg reg) {
            this(reg, 1);
        }
    }

    public final static Map<Op, OpDesc> opDefaults;

    static {
        opDefaults = new EnumMap<>(Op.class);

        opDefaults.put(Op.OP_ZERO, new OpDesc(Op.OP_ZERO.toString(), 1, DefaultNop, ARG_REG[0],
                new Operand(Operand.ArgSource.ARG_PUBLIC, 0, null, 1) // input is a public 0.
                ));
        opDefaults.put(Op.OP_XORI, new OpDesc(Op.OP_XORI.toString(), 1, DefaultNop, ARG_REG[0], ARG_REG[0], ARG_IMMEDIATE[1]));
        Operand[] addOut = {
                ARG_REG[0], new NamedRegDest(R_CARRY), new NamedRegDest(R_ZERO), new NamedRegDest(R_SIGN), new NamedRegDest(R_OVERFLOW), new NamedRegDest(R_FLAGS) 
        };
        opDefaults.put(Op.OP_ADD, new OpDesc(Op.OP_ADD.toString(), 1, DefaultNop, addOut, ARG_REG[1], ARG_REG[2]));
        Operand[] subOut = {
                ARG_REG[0], new NamedRegDest(R_CARRY), new NamedRegDest(R_ZERO), new NamedRegDest(R_SIGN), new NamedRegDest(R_OVERFLOW), new NamedRegDest(R_FLAGS) 
        };
        opDefaults.put(Op.OP_SUB, new OpDesc(Op.OP_SUB.toString(), 1, DefaultNop, subOut, ARG_REG[1], ARG_REG[2]));
        opDefaults.put(Op.OP_XOR, new OpDesc(Op.OP_XOR.toString(), 1, DefaultNop, ARG_REG[0], ARG_REG[1], ARG_REG[2]));
        opDefaults.put(Op.OP_AND, new OpDesc(Op.OP_AND.toString(), 1, DefaultNop, ARG_REG[0], ARG_REG[1], ARG_REG[2]));
        opDefaults.put(Op.OP_OR, new OpDesc(Op.OP_OR.toString(), 1, DefaultNop, ARG_REG[0], ARG_REG[1], ARG_REG[2]));
        opDefaults.put(Op.OP_ROL, new OpDesc(Op.OP_ROL.toString(), 1, DefaultNop, ARG_REG[0], ARG_REG[1], ARG_REG[2]));
        opDefaults.put(Op.OP_MUX, new OpDesc(Op.OP_MUX.toString(), 1, DefaultNop, ARG_REG[0], ARG_REG[1], ARG_REG[2], ARG_REG[3]));
        opDefaults.put(Op.OP_MUL, new OpDesc(Op.OP_MUL.toString(), 1, DefaultNop, ARG_REGw2, ARG_REG[1], ARG_REG[2]));
        opDefaults.put(Op.OP_DIV, new OpDesc(Op.OP_DIV.toString(), 1, DefaultNop, ARG_REGw2, ARG_REG[1], ARG_REG[2]));
        opDefaults.put(Op.OP_NEXT, new OpDesc(Op.OP_NEXT.toString(), 1, DefaultNop, new NamedRegDest(R_IP), ARG_REG[0], ARG_REG[1]));
        opDefaults.put(Op.OP_LOAD, new OpDesc(Op.OP_LOAD.toString(), 1, DefaultNop, ARG_REG[0], ARG_IND_RAM[1]));
        opDefaults.put(Op.OP_STORE, new OpDesc(Op.OP_STORE.toString(), 1, DefaultNop, ARG_IND_RAM[0], ARG_REG[1]));
        opDefaults.put(Op.OP_LOADREG, new OpDesc(Op.OP_LOADREG.toString(), 1, DefaultNop, ARG_REG[0], ARG_IND_REG[1]));
        opDefaults.put(Op.OP_STOREREG, new OpDesc(Op.OP_STOREREG.toString(), 1, DefaultNop, ARG_IND_REG[0], ARG_REG[1]));

        opDefaults.put(Op.OP_IN, new OpDesc(Op.OP_IN.toString(), 1, DefaultNop, new NamedRegDest(R_IN)));

        // we use width 2 for the out1 input to deal with the different outputs to each party.
        opDefaults.put(Op.OP_OUT, new OpDesc(Op.OP_OUT.toString(), 1, DefaultNop,
                (Operand) null, // no output
                new NamedRegDest(R_OUT2, 2), // we use OUT2 as base because the indices are counted from the end
                new NamedRegDest(R_CTRL)
        ));

        opDefaults.put(Op.OP_HALT, new OpDesc(Op.OP_HALT.toString(), 1, DefaultNop,
                (Operand) null, // no output
                new NamedRegDest(R_CTRL)
        ));


        // if stack becomes a standard memory area (and sp a standard register)
        // Call and return will also read  sp, read indirect from stack through sp (to get current frame),
        // and output sp as well as indirect to stack through sp,

        ImmediateWidth[] callWidths = { IMM_FLAG, IMM_ROMPTR };
        opDefaults.put(Op.OP_CALL,  new OpDesc(Op.OP_CALL.toString(), 1, DefaultNop, callWidths,
                new NamedRegDest(R_IP), // outputs new IP
                ARG_IMMEDIATE[0], // A flag determining whether to perform the call
                ARG_IMMEDIATE[1], // The call address
                new NamedRegDest(R_IP), // reads current IP (to be copied to output in case of NOP).
                new Operand(Operand.ArgSource.ARG_PUBLIC, LocalFrame, 0, reg1, -1) // we use negative width to
                // specify that we need a multiple of frame size (i.e., all of the local registers in this case).
        ));


        Operand[] returnArgs = {
                new NamedRegDest(R_IP), // outputs new IP and new contents for local registers.
                new Operand(Operand.ArgSource.ARG_PUBLIC, LocalFrame, 0, reg1, -1) // we use negative width to
                    // specify that we need a multiple of frame size rather than word size
                    // (i.e., all of the local registers in this case).
                };
        ImmediateWidth[] retWidths = { IMM_FLAG };

        opDefaults.put(Op.OP_RETURN, new OpDesc(Op.OP_RETURN.toString(), 1, DefaultNop, retWidths,
                returnArgs,
                ARG_IMMEDIATE[0],           // Flag determining whether to perform the return
                new NamedRegDest(R_IP)      // Current Ip
                ));


    }

    public static OpDesc getDefaultDesc(String opName) {
        return opDefaults.get(opName);
    }

    public static OpDesc getDefaultDesc(Op opName) {
        return opDefaults.get(opName);
    }

    /**
     * Return the value computer by name (used in OpDesc deserialization).
     */
    public static Operand.PublicValueComputer getPublicValueComputerByName(String name) {
        if (name.equals(""))
            return null;
        return Operand.AutoRegisteringValueComputer.getValueComputerByName(name);
    }

    public static String getValueComputerName(Operand.PublicValueComputer pvc) {
        if (pvc == null)
            return "";
        return pvc.getClass().getName();
    }


    public static NopComputer getNopComputerByName(String nopComputerName) {
        return AutoRegisteringNopComputer.getComputerByName(nopComputerName);
    }


    public static String getNopComputerName(NopComputer nc) {
        if (nc == null)
            return "";
        return nc.getClass().getName();
    }

}

