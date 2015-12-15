package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;
import org.factcenter.fastgc.YaoGC.XOR_2L_L;

/*-
 * NOTE: XOrOpCircuit should not be used after STORE is implemented.
 */

/**
 * N bits bitwise xor operator.
 * Input ports order is: [Player0_xShare, Player0_yShare, Player0_rValue,
 * Player1_xShare, Player1_yShare, Player1_rValue].
 */
public class XorOpCircuit  extends CompositeCircuit {
	
	private final int xorBitWidth;
	
	private UNSHARE unshareComponent;
	private XOR_2L_L xor2l;
	private SHARE shareComponent;

	/**
	 * Constructs a new {@code XorOpCircuit} object.
	 * 
	 * @param globals - Global circuit parameters.
	 * @param cntBits - op word size.
	 */
	public XorOpCircuit(CircuitGlobals globals, int cntBits) {
		super(globals, 6 * cntBits, cntBits, 3, "Xor_" + cntBits + "bits");

		xorBitWidth = cntBits;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		
		// Create the sub circuits.
		unshareComponent = new UNSHARE(globals, xorBitWidth);
		xor2l = new XOR_2L_L(globals, xorBitWidth);
		shareComponent = new SHARE(globals, xorBitWidth);

		int i = 0;
		subCircuits[i++] = unshareComponent;
		subCircuits[i++] = xor2l;
		subCircuits[i++] = shareComponent;

	}

	protected void connectWires() {
		
		// Wire the 4 shared inputs to the un-share component.
		unshareComponent.connectWiresToXY(inputWires, 0, inputWires, 3 * xorBitWidth);
		
		// Wire the un-share outputs to the op circuit inputs.
		int yWireNdx = 0;
		for (int i = 0; i < xorBitWidth; ++i) {
			unshareComponent.outputWires[i].connectTo(xor2l.inputWires, i);
			yWireNdx = i + xorBitWidth;
			unshareComponent.outputWires[yWireNdx].connectTo(xor2l.inputWires, yWireNdx);
		}
		
		// Wire the op outputs and the player's random values to the share component inputs.
		shareComponent.connectWiresToInputs(inputWires, 2 * xorBitWidth, inputWires, 5 * xorBitWidth, xor2l.outputWires, 0);
	}

	protected void defineOutputWires() {
		for (int i = 0; i < xorBitWidth; i++)
				 shareComponent.outputWires[i].connectTo(outputWires, i);
	}	
	

}
