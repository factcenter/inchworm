package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;

import java.util.Random;

/**
 * Wraps an Op circuit with the appropriate unshare/reshare circuits.
 */
public class InchwormOpWrapper extends InchwormOpCommon {

    public interface CircuitFactory {
        Circuit createCircuit(CircuitGlobals globals);
    }

    Circuit baseCircuit;


    /**
     * Constructor (perform initialization before the instantiation of a subclass).
     *
     * @param rand           - a secure random number generator.
     * @param labelBitLength
     */
    public InchwormOpWrapper(boolean serverOp, Random rand, int labelBitLength, Channel toPeer, OTExtender otExtender,
                             CircuitFactory circuitFactory) {
        super(rand, labelBitLength);

        baseCircuit = circuitFactory.createCircuit(globals);

        this.serverMode = serverOp;

        try {
            ccs = new Circuit[1];

            ccs[0] = new UnshareOpReshareCircuit(globals, baseCircuit);

            // Have to set otNumOfPairs before calling setParameters
            this.otNumOfPairs = ccs[0].getInDegree() / 2;

            setParameters(toPeer, otExtender);

            if (serverMode)
                generateLabelPairs();

            super.init();
            createCircuits(serverMode);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
