package org.factcenter.inchworm.ops;

import org.factcenter.inchworm.MemoryArea;
import org.factcenter.inchworm.VMState;
import org.factcenter.qilin.comm.Sendable;
import org.factcenter.qilin.comm.SendableInput;
import org.factcenter.qilin.comm.SendableOutput;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Supported opcodes order and frequency information.
 */
public class OpDesc implements Sendable {

    public enum ImmediateWidth {
        /**
         * Single bit.
         */
        IMM_FLAG,

        /**
         * Constant (up to word size)
         */
        IMM_WORD,

        /**
         * Pointer to register memory
         */
        IMM_REGPTR,

        /**
         * Pointer to RAM
         */
        IMM_RAMPTR,

        /**
         * Pointer to ROM
         */
        IMM_ROMPTR,

        /**
         * Pointer to STACK
         */
        IMM_STACKPTR,
    }

    /**
     * Compute the values of immediate arguments that will make this
     * op a NOP.
     *
     */
    public interface NopComputer {
        /**
         * Return an array of values to be used as the immediate arguments
         * for the given op.
         *
         * The length of the array should match the length of {@link OpDesc#immediates}.
         * @return
         */
        public BitMatrix[] getNopValues(OpDesc op, VMState state);
    }

    /**
     * Convenience class that maintains a registry of nop computers by name
     * (this allows for easier serialization).
     * Use only for singletons created in a static initialization block (the registry
     * will contain only the last generated instance of the class, and is not thread-safe).
     */
    public abstract static class AutoRegisteringNopComputer implements NopComputer {
        static Map<String, NopComputer> nopComputerRegistry = new HashMap<>();

        public static NopComputer getComputerByName(String name) {
            return nopComputerRegistry.get(name);
        }

        public AutoRegisteringNopComputer() {
            nopComputerRegistry.put(getClass().getName(), this);
        }
    }

    public String name;

    public int freq;

    /**
     * Immediate operands.
     * Each element in the array is the width of the corresponding
     */
    public ImmediateWidth immediates[];

    /**
     * Sources (these will be read)
     */
    public Operand[] inArgs;

    /**
     * Destinations (these will be written)
     */
    public Operand[] outArgs;

    public NopComputer nopComputer;


    /**
     * Default constructor. This should only be used for deserialization.
     */
    public OpDesc() {

    }

    /**
     * Copy constructor that allows changing frequency.
     * @param defaults
     * @param freq
     */
    public OpDesc(OpDesc defaults, int freq) {
        this(defaults.name, freq, defaults.nopComputer, defaults.immediates, defaults.outArgs, defaults.inArgs);
    }


    private OpDesc(String name, int freq, NopComputer nopComputer, ImmediateWidth[] immediates, Operand... inArgs) {
        this.name = name;
        this.freq = freq;
        this.inArgs = inArgs;
        this.immediates = immediates;
        this.nopComputer = nopComputer;
    }

    public OpDesc(String name, int freq, NopComputer nopComputer, ImmediateWidth[] immediates, Operand[] outArgs, Operand... inArgs) {
        this(name, freq, nopComputer, immediates, inArgs);
        this.outArgs = outArgs;
    }

    public OpDesc(String name, int freq, NopComputer nopComputer, ImmediateWidth[] immediates, Operand outArg, Operand... inArgs) {
        this(name, freq, nopComputer, immediates, inArgs);
        if (outArg != null) {
            Operand[] outArgs = {outArg};
            this.outArgs = outArgs;
        }
    }


    public OpDesc(String name, int freq, NopComputer nopComputer, Operand[] outArgs, Operand... inArgs) {
        this(name, freq, nopComputer, null, outArgs, inArgs);

        computeImmedates();
    }

    public OpDesc(String name, int freq, NopComputer nopComputer, Operand outArg, Operand... inArgs) {
        this(name, freq, nopComputer, null, (Operand[]) null, inArgs);
        if (outArg != null) {
            Operand[] outArgs = {outArg};
            this.outArgs = outArgs;
        }
        computeImmedates();
    }


    /**
     * Add item to an arraylist in an index that may be greater than current size.
     * Grows the arraylist with nulls.
     */
    <E> void poke(ArrayList<E> list, int pos, E val) {
        for (int i = list.size(); i <= pos; ++i) {
            list.add(i, null);
        }
        list.set(pos, val);
    }

    /**
     * Compute default widths for immediate operands.
     * This is used if explicit widths are not given.
     */
    void computeImmedates() {
        ArrayList<ImmediateWidth> widths = new ArrayList<>();

        if (outArgs != null) {
            for (Operand operand : outArgs) {
                ImmediateWidth wid = guessImmediateWidth(operand);
                if (wid != null)
                    poke(widths, operand.sourceIdx, wid);
            }
        }


        if (inArgs != null) {
            for (Operand operand : inArgs) {
                ImmediateWidth wid = guessImmediateWidth(operand);
                if (wid != null)
                    poke(widths, operand.sourceIdx, wid);
            }
        }

        immediates = widths.toArray(new ImmediateWidth[widths.size()]);
    }

    /**
     * Make an educated guess as to the width of an immediate operand.
     * This might overestimate the width (e.g., if only a single
     * bit is needed, it might allocate an entire word).
     * @param operand
     * @return
     */
    private ImmediateWidth guessImmediateWidth(Operand operand) {
        if (operand.source != Operand.ArgSource.ARG_IMMEDIATE)
            return null;

        ImmediateWidth wid = ImmediateWidth.IMM_WORD;

        if (operand.path != null && operand.path.length > 0) {
            switch (operand.path[0]) {
                case TYPE_RAM:
                    return ImmediateWidth.IMM_RAMPTR;
                case TYPE_REG:
                    return ImmediateWidth.IMM_REGPTR;
                case TYPE_ROM:
                    return ImmediateWidth.IMM_ROMPTR;
                case TYPE_STACK:
                    return ImmediateWidth.IMM_STACKPTR;
            }
        }
        return ImmediateWidth.IMM_WORD;
    }


    public BitMatrix[] getNopArgs(VMState state) {
        if (nopComputer == null)
            return null;
        return nopComputer.getNopValues(this, state);
    }

    @Override
    public void writeTo(SendableOutput out) throws IOException {
        out.writeUTF(name);
        out.writeInt(freq);
        out.writeObject(immediates); // can write array of enums automatically
        out.writeUTF(OpDefaults.getNopComputerName(nopComputer));
        writeOperands(out, outArgs);
        writeOperands(out, inArgs);
    }

    private void writeOperands(SendableOutput out, Operand[] args) throws IOException {
        if (args == null) {
            out.writeInt(0);
            return;
        }
        out.writeInt(args.length);
        for (Operand op : args) {
            out.writeObject(op.source);
            out.writeInt(op.sourceIdx);
            out.writeNullableObject(op.path);
            out.writeInt(op.width);

            out.writeUTF(OpDefaults.getValueComputerName(op.publicValueComputer));
        }
    }

    private Operand[] readOperands(SendableInput in) throws IOException {
        int len = in.readInt();
        if (len == 0)
            return null;
        Operand[] args = new Operand[len];
        for (int i = 0; i < args.length; ++i) {
            Operand.ArgSource src = in.readObject(Operand.ArgSource.class);
            int srcIdx = in.readInt();
            MemoryArea.Type[] path = in.readNullableObject(MemoryArea.Type[].class);
            int width = in.readInt();


            String valueComputerName = in.readUTF();

            Operand.PublicValueComputer pvc = null;
            if (valueComputerName != null)
                pvc = OpDefaults.getPublicValueComputerByName(valueComputerName);

            args[i] = new Operand(src, pvc, srcIdx, path, width);
        }
        return args;
    }

    @Override
    public void readFrom(SendableInput in) throws IOException {
        name = in.readUTF();
        freq = in.readInt();
        immediates = in.readObject(ImmediateWidth[].class);

        String nopComputerName = in.readUTF();
        if (nopComputerName != null)
            nopComputer = OpDefaults.getNopComputerByName(nopComputerName);

        outArgs = readOperands(in);
        inArgs = readOperands(in);
    }


    /**
     * Equality test ignores frequency!
     *
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof  OpDesc))
            return false;
        OpDesc other = (OpDesc) obj;

//        if (!(freq == other.freq))
//            return false;

        if (!name.equals(other.name))
            return false;

        if (!Objects.deepEquals(immediates, other.immediates))
            return false;

        if (!Objects.deepEquals(outArgs, other.outArgs))
            return false;

        if (!Objects.deepEquals(inArgs, other.inArgs))
            return false;

        if (!Objects.equals(nopComputer, other.nopComputer))
            return false;

        return true;

    }


}