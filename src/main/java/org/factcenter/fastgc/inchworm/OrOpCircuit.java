package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;

/**
 * N bits bitwise or operator.
 * Input ports order is: [Player0_xShare, Player0_yShare, Player0_rValue,
 * Player1_xShare, Player1_yShare, Player1_rValue].
 */
public class OrOpCircuit extends CompositeCircuit {

	private final int orBitWidth;
	
	private UNSHARE unshareComponent;
	private OR_2L_L or;
	private SHARE shareComponent;

	/**
	 * Constructs a new {@code OrOpCircuit} object.
	 * 
	 * @param globals - Global circuit parameters.
	 * @param cntBits - op word size.
	 */
	public OrOpCircuit(CircuitGlobals globals, int cntBits) {
		super(globals, 6 * cntBits, cntBits, 3, "Or_" + cntBits + "bits");

		orBitWidth = cntBits;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		
		// Create the sub circuits.
		unshareComponent = new UNSHARE(globals, orBitWidth);
		or = new OR_2L_L(globals, orBitWidth);
		shareComponent = new SHARE(globals, orBitWidth);

		int i = 0;
		subCircuits[i++] = unshareComponent;
		subCircuits[i++] = or;
		subCircuits[i++] = shareComponent;
	}

	protected void connectWires() {
		
		// Wire the 4 shared inputs to the un-share component.
		unshareComponent.connectWiresToXY(inputWires, 0, inputWires, 3 * orBitWidth);
		
		// Wire the un-share outputs to the op circuit inputs.
		or.connectWiresToXY(unshareComponent.outputWires, 0,
				unshareComponent.outputWires, orBitWidth);
		
		// Wire the op outputs and the player's random values to the share component inputs.
		shareComponent.connectWiresToInputs(inputWires, 2 * orBitWidth, inputWires, 5 * orBitWidth, or.outputWires, 0);
	}

	protected void defineOutputWires() {
		for (int i = 0; i < orBitWidth; i++)
				 shareComponent.outputWires[i].connectTo(outputWires, i);
	}
	
}
