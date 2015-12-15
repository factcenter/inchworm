package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;

/**
 * Accept as input a sequence of "existing" data bits, divided into N blocks of size K bits each,
 * a "new block" of K bits and a binary index i (of log N bits).
 *
 * The output is N blocks of K bits, with the i^th original block replaced by the new block.
 */
public class UNMUX_K_N extends CompositeCircuit {
    int blockSize;

    int numBlocks;

    int indexSize;

    MUX_2L_L[] muxes;
    UNARY_CTR_K unaryCtr;


    public UNMUX_K_N(CircuitGlobals globals, int blockSize, int numBlocks, int indexSize) {
        super(globals, blockSize * numBlocks + indexSize + blockSize, blockSize * numBlocks, numBlocks + 1,
                "UNMUX_" + blockSize + "_" + numBlocks);

        this.blockSize = blockSize;
        this.numBlocks = numBlocks;
        this.indexSize = indexSize;
    }

    @Override
    protected void createAllSubCircuits(boolean isForGarbling) {
        int n = 0;

        subCircuits[n++] = unaryCtr = new UNARY_CTR_K(globals, indexSize, numBlocks);

        muxes = new MUX_2L_L[numBlocks];
        for (int i = 0; i < numBlocks; ++i)
            subCircuits[n++] = muxes[i] = new MUX_2L_L(globals, blockSize);
    }

    @Override
    protected void connectWires() {
        for (int i = 0; i < indexSize; ++i) {
            inputWires[numBlocks * blockSize + blockSize + i].connectTo(unaryCtr.inputWires, i);
        }
        for (int i = 0; i < numBlocks; ++i) {
            for (int j = 0; j < blockSize; ++j) {
                // Connect the i^th original block to the "0" position of the i^th mux
                inputWires[i * blockSize + j].connectTo(muxes[i].inputWires, j + blockSize);
                // Connect the "new block" to the "1" position of the i^th mux
                inputWires[numBlocks * blockSize + j].connectTo(muxes[i].inputWires, j);
            }
            // Connect the i^th unary counter bit to the control bit of the i^th mux.
            unaryCtr.outputWires[i].connectTo(muxes[i].inputWires, blockSize * 2);
        }
    }

    @Override
    protected void defineOutputWires() {
        for (int i = 0; i < numBlocks; ++i) {
            for (int j = 0; j < blockSize; ++j) {
                 muxes[i].outputWires[j].connectTo(outputWires, i * blockSize + j);
            }
        }
    }
}
