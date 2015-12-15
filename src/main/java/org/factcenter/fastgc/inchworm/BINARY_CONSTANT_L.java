package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;
import org.factcenter.fastgc.YaoGC.Wire;

/**
 * Created by talm on 9/9/14.
 */
public class BINARY_CONSTANT_L extends CompositeCircuit {
    int bitWidth;
    int idx;

    public BINARY_CONSTANT_L(CircuitGlobals globals, int bitWidth, int idx) {
        super(globals, 0, bitWidth, 0, "BINARY_CONSTANT_" + bitWidth + "_" + idx);
        this.bitWidth = bitWidth;
        this.idx = idx;
    }

    @Override
    protected void createAllSubCircuits(boolean isForGarbling) {
        for (int i = 0; i < bitWidth; ++i) {
            new Wire(globals, outputWires, i);
        }
    }

    @Override
    protected void connectWires() {    }

    @Override
    protected void defineOutputWires() {
        for (int i = 0; i < bitWidth; ++i) {
            outputWires[i].fixWire((idx >>> i) & 1);
        }
    }
}
