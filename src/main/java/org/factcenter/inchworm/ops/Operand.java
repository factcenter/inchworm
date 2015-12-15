package org.factcenter.inchworm.ops;

import org.factcenter.inchworm.MemoryArea;
import org.factcenter.inchworm.VMState;
import org.factcenter.qilin.util.BitMatrix;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
* Created by talm on 8/16/14.
*/
public class Operand {

    public interface PublicValueComputer {
        public BitMatrix computeConstant(VMState state, int idx);
        public int computeWidth(VMState state, int width);
    }

    /**
     * Convenience class that maintains a registry of value computers by name
     * (this allows for easier serialization).
     * Use only for singletons created in a static initialization block (the registry
     * will contain only the last generated instance of the class, and is not thread-safe).
     */
    public abstract static class AutoRegisteringValueComputer implements PublicValueComputer {
        static Map<String, PublicValueComputer> valueComputerRegistry = new HashMap<>();

        public static PublicValueComputer getValueComputerByName(String name) {
            return valueComputerRegistry.get(name);
        }

        public AutoRegisteringValueComputer() {
            valueComputerRegistry.put(getClass().getName(), this);
        }
    }

    /**
     * Convenience class for PublicValueComputer implementations that don't modify width.
     */
    public abstract static class ConstantWidthValueComputer extends AutoRegisteringValueComputer {
        @Override
        public int computeWidth(VMState state, int width) {
            return width;
        }
    }

    /**
     * Argument types
     */
    public enum ArgSource {
        /**
         * A public value (if {@link #publicValueComputer} is null, then {@link #sourceIdx} is the value,
         * otherwise the constant is the value of {@link #publicValueComputer} on input {@link #sourceIdx}.
         */
        ARG_PUBLIC,

        /**
         * An immediate value (appearing in r_next).
         */
        ARG_IMMEDIATE,
    }

    public ArgSource source;

    /**
     * Index of the source operand.
     * For immediates, this is the index of the immediate relative to the index of the first previously unused
     * immediate operand (this means '0' is the 'default' value; e.g., if this is the third immediate-source operand,
     * but the first two operands both used the same source, then index 0 means it uses the second immediate operand).
     *
     * For public values, if {@link #publicValueComputer} is null, this is the actual constant (otherwise it is given as an input
     * to the publicValueComputer).
     */
    public int sourceIdx;

    public PublicValueComputer publicValueComputer;

    /**
     * "Path" to actual argument.
     * path[-1] is defined as the constant/immediate (depending on source).
     * If path is null or of length 0, arg is the public value/immediate itself.
     * If path has length {@code k > 0}, the argument is resolved by the following recursion:
     * path[i] = memArea_i [path[i-1]].
     *
     * For example, a register whose address is an immediate operand would have path length 1,
     * with path[0] containing TYPE_REG.
     */
    public MemoryArea.Type[] path;

    /**
     * Width of the final resolved argument, in words (wordsize depends on the memory area).
     * Width 2 means the argument consists of two consecutive words
     */
    public int width;


    public Operand(ArgSource source, PublicValueComputer publicValueComputer, int sourceIdx, MemoryArea.Type[] path, int width) {
        this.source = source;
        this.sourceIdx = sourceIdx;
        this.path = path;
        this.width = width;
        this.publicValueComputer = publicValueComputer;
    }

    public Operand(ArgSource source, int sourceIdx, MemoryArea.Type[] path, int width) {
        this(source, null, sourceIdx, path, width);
    }

    public Operand(int sourceIdx, MemoryArea.Type[] path, int width) {
        this(ArgSource.ARG_IMMEDIATE, null, sourceIdx, path, width);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof  Operand))
            return false;

        Operand other = (Operand) obj;

        boolean same = true;
        same &= source == other.source;
        same &= sourceIdx == other.sourceIdx;
        same &= width == other.width;

        same &= Objects.deepEquals(path, other.path);
        same &= Objects.equals(publicValueComputer, other.publicValueComputer);

        return same;
    }

}
