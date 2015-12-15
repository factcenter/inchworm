package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;
import org.factcenter.fastgc.YaoGC.XOR_2L_L;

/**
 * Select 1 out of N blocks, each of size K. The randomness bits are in the highest position, followed by control bits
 * and then data bits (MSB contains highest-order block).
 * Input ports order is: [Player0_dataBitsShare, Player0_ctrlBitsShare, Player0_randomness]
 *  [Player1_dataBitsShare, Player1_ctrlBitsShare, Player1_randomness],
 * Length of randomness is blockSize (it's used to reshare output).
 */
public class UnshareOpReshareCircuit extends CompositeCircuit {
    int subCircuitInDegree;
    int subCircuitOutDegree;

    int playerInputWidth;

	private XOR_2L_L unshareComponent;
	private Circuit opCircuit;
	private XOR_2L_L reshareComponent;

	/**
	 * Constructs a new {@code MuxOpCircuit} object.
	 * @param globals - Global circuit parameters.
	 */
	public UnshareOpReshareCircuit(CircuitGlobals globals, Circuit opCircuit) {
		super(globals, (opCircuit.getInDegree() + opCircuit.getOutDegree()) * 2, opCircuit.getOutDegree(), 3,
                "Unshare_Reshare_" + opCircuit.getClass().getName());

        this.opCircuit = opCircuit;
        this.subCircuitInDegree = opCircuit.getInDegree();
        this.subCircuitOutDegree = opCircuit.getOutDegree();

        playerInputWidth = subCircuitInDegree + subCircuitOutDegree;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {

		// Create the sub circuits.
		unshareComponent = new XOR_2L_L(globals, playerInputWidth);
    	reshareComponent = new XOR_2L_L(globals, subCircuitOutDegree);

		int i = 0;
		subCircuits[i++] = unshareComponent;
		subCircuits[i++] = opCircuit;
		subCircuits[i++] = reshareComponent;

	}

	protected void connectWires() {
        // Wire the inputs to the unshare component.
        for (int i = 0; i < playerInputWidth; ++i) {
            inputWires[i].connectTo(unshareComponent.inputWires, i);
            inputWires[i + playerInputWidth].connectTo(unshareComponent.inputWires, i + playerInputWidth);
        }

        // Wire the unshared op inputs (data, new block and control)
        for (int i = 0; i <  subCircuitInDegree; ++i) {
            unshareComponent.outputWires[i].connectTo(opCircuit.inputWires, i);
        }

        // Wire the output of the op and the unshared randomness to the reshare (XOR) component.
        for (int i = 0; i < subCircuitOutDegree; ++i) {
            opCircuit.outputWires[i].connectTo(reshareComponent.inputWires, i);
            unshareComponent.outputWires[i + subCircuitInDegree].connectTo(reshareComponent.inputWires,
                    i + subCircuitOutDegree);
        }
	}

	protected void defineOutputWires() {
		for (int i = 0; i < subCircuitOutDegree; i++)
			 reshareComponent.outputWires[i].connectTo(outputWires, i);
	}

}
