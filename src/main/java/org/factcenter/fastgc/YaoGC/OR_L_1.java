// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC;

public class OR_L_1 extends GenericBinaryTree_L_1 {
    public OR_L_1(CircuitGlobals globals, int bitWidth) {
        super(globals, new NodeCircuitFactory() {
            @Override
            public Circuit createNodeCircuit(CircuitGlobals globals, boolean isForGarbling, int height) {
                return OR_2_1.newInstance(globals, isForGarbling);
            }
        }, "OR", bitWidth);
    }
}