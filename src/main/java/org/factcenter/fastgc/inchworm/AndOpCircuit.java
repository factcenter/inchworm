package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;

/**
 * N bit bitwise and operator.
 * Input ports order is: [Player0_xShare, Player0_yShare, Player0_rValue,
 * Player1_xShare, Player1_yShare, Player1_rValue].
 */
public class AndOpCircuit extends CompositeCircuit {

	private final int andBitWidth;

	private UNSHARE unshareComponent;
	private AND_2L_L and;
	private SHARE shareComponent;

	/**
	 * Constructs a new {@code AndOpCircuit} object.
	 * @param globals - Global circuit parameters.
	 * @param cntBits - op word size.
	 */
	public AndOpCircuit(CircuitGlobals globals, int cntBits) {
		super(globals, 6 * cntBits, cntBits, 3, "And_" + cntBits + "bits");

		andBitWidth = cntBits;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		// Create the sub circuits.
		unshareComponent = new UNSHARE(globals, andBitWidth);
		and = new AND_2L_L(globals, andBitWidth);
		shareComponent = new SHARE(globals, andBitWidth);

		int i = 0;
		subCircuits[i++] = unshareComponent;
		subCircuits[i++] = and;
		subCircuits[i++] = shareComponent;
	}

	protected void connectWires() {
		
		// Wire the 4 shared inputs to the un-share component.
		unshareComponent.connectWiresToXY(inputWires, 0, inputWires, 3 * andBitWidth);
		
		// Wire the un-share outputs to the op circuit inputs.
		and.connectWiresToXY(unshareComponent.outputWires, 0,
				unshareComponent.outputWires, andBitWidth);
		
		// Wire the op outputs and the player's random values to the share component inputs.
		shareComponent.connectWiresToInputs(inputWires, 2 * andBitWidth, inputWires, 5 * andBitWidth, and.outputWires, 0);
		
	}

	protected void defineOutputWires() {
		for (int i = 0; i < andBitWidth; i++)
            shareComponent.outputWires[i].connectTo(outputWires, i);
	}
	
}
