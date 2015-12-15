// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC;


public class XOR_L_1 extends GenericBinaryTree_L_1 {
    public XOR_L_1(CircuitGlobals globals, int bitWidth) {
        super(globals, new GenericBinaryTree_L_1.NodeCircuitFactory() {
            @Override
            public Circuit createNodeCircuit(CircuitGlobals globals, boolean isForGarbling, int height) {
                return new XOR_2_1(globals);
            }
        }, "XOR", bitWidth);
    }

}