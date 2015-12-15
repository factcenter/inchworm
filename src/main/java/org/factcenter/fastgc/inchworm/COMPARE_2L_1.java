package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;
import org.factcenter.fastgc.YaoGC.OR_L_1;
import org.factcenter.fastgc.YaoGC.XOR_2L_L;

/**
 * Compare two values of size L, output 1 if they are equal and 0 otherwise.
 */
public class COMPARE_2L_1 extends CompositeCircuit  {
    int bitWidth;

    XOR_2L_L xor;
    OR_L_1 or;
    INVERTER not;

    public COMPARE_2L_1(CircuitGlobals globals, int bitWidth) {
        super(globals, bitWidth * 2, 1, bitWidth > 1 ? 3 : 2, "COMPARE_2x" + bitWidth + "_1");
        this.bitWidth = bitWidth;
    }

    @Override
    protected void createAllSubCircuits(boolean isForGarbling) {
        int i = 0;
        subCircuits[i++] = xor = new XOR_2L_L(globals, bitWidth);
        if (bitWidth > 1)
            subCircuits[i++] = or = new OR_L_1(globals, bitWidth);
        subCircuits[i++] = not = new INVERTER(globals);
    }

    @Override
    protected void connectWires() {
        for (int i = 0; i < bitWidth * 2; ++i)
            inputWires[i].connectTo(xor.inputWires, i);

        if (bitWidth > 1) {
            for (int i = 0; i < bitWidth; ++i)
                xor.outputWires[i].connectTo(or.inputWires, i);

            or.outputWires[0].connectTo(not.inputWires, 0);
        } else {
            xor.outputWires[0].connectTo(not.inputWires, 0);
        }
    }

    @Override
    protected void defineOutputWires() {
        not.outputWires[0].connectTo(outputWires, 0);
    }
}
