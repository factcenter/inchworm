// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC;


/**
 * Create a binary tree of gates.
 */
public class GenericBinaryTree_L_1 extends CompositeCircuit {
	private final int bitWidth;

    GenericBinaryTree_L_1 treeLeft;
    GenericBinaryTree_L_1 treeRight;
    Circuit nodeCircuit;

    NodeCircuitFactory nodeCircuitFactory;

    public interface NodeCircuitFactory {
        /**
         * Return a new circuit for a node at a specific height. The circuit should have two inputs and one output.
         * @param height height in the tree (0 means the node's children are input wires)
         */
        public Circuit createNodeCircuit(CircuitGlobals globals, boolean isForGarbling, int height);
    }


	public GenericBinaryTree_L_1(CircuitGlobals globals, NodeCircuitFactory nodeCircuitFactory, String name, int bitWidth) {
		super(globals, bitWidth, 1, (bitWidth > 2) ? 3 : (bitWidth > 1) ? 1 : 0, name + "_" + bitWidth + "_1");
		this.bitWidth = bitWidth;
        this.nodeCircuitFactory = nodeCircuitFactory;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
        int height = 32 - Integer.numberOfLeadingZeros(bitWidth < 2 ? 2 : bitWidth) - 1;

        int n = 0;
        if (bitWidth == 1)
            return;
        subCircuits[n++] = nodeCircuit = nodeCircuitFactory.createNodeCircuit(globals, isForGarbling, height);
        assert(nodeCircuit.getInDegree() == 2 && nodeCircuit.getOutDegree() == 1);

        if (bitWidth > 2) {
            subCircuits[n++] = treeLeft = new GenericBinaryTree_L_1(globals, nodeCircuitFactory, name,  bitWidth / 2);

            // The right subtree gets the extra input if they're not even.
            subCircuits[n++] = treeRight = new GenericBinaryTree_L_1(globals, nodeCircuitFactory, name, bitWidth / 2 + (bitWidth & 1));
        }
	}

	protected void connectWires() {
        if (bitWidth == 1)
            return;

        if (bitWidth <= 2) {
            inputWires[0].connectTo(nodeCircuit.inputWires, 0);
            if (bitWidth == 2)
                inputWires[1].connectTo(nodeCircuit.inputWires, 1);
            else
                inputWires[1].fixWire(1);
        } else {
            int leftDegree = treeLeft.getInDegree();
            int rightDegree = treeRight.getInDegree();
            for (int i = 0; i < leftDegree; ++i) {
                inputWires[i].connectTo(treeLeft.inputWires, i);
            }

            for (int i = 0; i < rightDegree; ++i) {
                inputWires[i + leftDegree].connectTo(treeRight.inputWires, i);
            }

            treeLeft.outputWires[0].connectTo(nodeCircuit.inputWires, 0);
            treeRight.outputWires[0].connectTo(nodeCircuit.inputWires, 1);

        }
	}

	protected void defineOutputWires() {
        if (bitWidth == 1) {
            inputWires[0].connectTo(outputWires, 0);
        } else {
            nodeCircuit.outputWires[0].connectTo(outputWires, 0);
        }
	}

	/**
	 * Connect srcInputs[srcStartPos...srcStartPos + bitWidth] to the inputs
	 * of the or circuit.
	 */
	public void connectWiresToInputs(Wire[] srcInputs, int srcStartPos) {

		for (int i = 0; i < bitWidth; i++) {
			srcInputs[i + srcStartPos].connectTo(inputWires, i);
		}
	}

}