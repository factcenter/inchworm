package org.factcenter.inchworm;


import java.util.HashMap;
import java.util.Map;

public class Constants {
	public final static int DEFAULT_WORDSIZE = 32;
	public final static int DEFAULT_RAMWORDSIZE = -1; // -1 means same as wordsize
    public final static int DEFAULT_RAMLOADSIZE = 1; // Load/store 1 word at a time by default.
	public final static int DEFAULT_REGPTRSIZE = 8;
	public final static int DEFAULT_ROMPTRSIZE = 8;
	public final static int DEFAULT_RAMPTRSIZE = 10;
	/**
	 * Max of 16 items in stack.
	 */
	public final static int DEFAULT_STACKPTRSIZE = 4;
	/**
	 * Max 4 local registers per frame.
	 */
	public final static int DEFAULT_FRAMESIZE = 4;


    public enum NamedReg {
        /**
         * Offset of NOP register from end of register file.
         * Note that this must be an even address, and two
         * nop registers are allocated (for operations that
         * affect two consecutive registers).
         */
        R_NOP(2),

        /**
         * Offset of IN register from end of register file.
         */
        R_IN(3),
        /**
         * Offset of OUT1 register from end of register file.
         */
        R_OUT1(4),

        /**
         * Offset of OUT2 register from end of register file.
         */
        R_OUT2(5),
        /**
         * Offset of IP (instruction pointer) register from end of register file.
         */
        R_IP(6),

        /**
         * Offset of ZERO flag register from end of register file.
         */
        R_ZERO(7),

        /**
         * Offset of CARRY flag register from end of register file.
         */
        R_CARRY(8),

        /**
         * Offset of CTRL register from end of register file
         */
        R_CTRL(9),
        // Sign register offset. todo: I *DEFINITELY* broke something here. must find the place where the start of the local registers in the register file is determined and move that one down.
        R_SIGN(10),
        R_OVERFLOW(11),
        R_FLAGS(12),
        // todo: will this mess up my code generation for registers?
        R_LASTMEMCELL(13),
        R_STOREREG_NOP_DST(14);

        int offset;


        NamedReg(int offset) {
            this.offset = offset;
        }


        final static Map<Integer, NamedReg> regFromInteger;

        static {
            regFromInteger = new HashMap<>();
            for (NamedReg reg : NamedReg.values()) {
                regFromInteger.put(reg.offset, reg);
            }
        }


        public int getOffset() {
            return offset;
        }

        public static NamedReg getNamedReg(int offset) {
            return regFromInteger.get(offset);
        }
    }
	/**
	 * Offset of the first "fixed-position" special register
	 * (below this are the counter flags -- the number of counter flags is a VM parameter).
	 * 
	 */
	public final static NamedReg FIRST_FIXED_SPECIAL = NamedReg.R_STOREREG_NOP_DST;

}
