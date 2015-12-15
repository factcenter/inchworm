package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.AND_2_1;
import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.GenericBinaryTree_L_1;

/**
 * L inputs logical and circuit.
 */
public class AND_L_1 extends GenericBinaryTree_L_1 {
    public AND_L_1(CircuitGlobals globals, int bitWidth) {
        super(globals, new NodeCircuitFactory() {
            @Override
            public Circuit createNodeCircuit(CircuitGlobals globals, boolean isForGarbling, int height) {
                return AND_2_1.newInstance(globals, isForGarbling);
            }
        }, "And", bitWidth);
    }
}
