package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;

/**
 * N bits ROL operator. Input ports order is: [Player0_xShare (the number),
 * Player0_yShare (the shift), Player0_rValue, Player1_xShare, Player1_yShare,
 * Player1_rValue].
 */
public class RoliOpCircuit extends CompositeCircuit {

	private final int opBitWidth;
	private final int opControlBits;

	/**
	 * Un-share component for source_value + shift_count.
	 */
	private UNSHARE unshareComponent;
	/**
	 * Mux for selecting between 2^N blocks of K bits each.
	 */
	private MUX_K_N mux_kn;
	/**
	 * Array of bitWidth {@link ROL_L_X} instances.
	 */
	private ROL_L_X[] rolComponent;
	private SHARE shareComponent;

	/**
	 * Constructs a new {@code RoliOpCircuit} object.
	 * 
	 * @param globals - Global circuit parameters.
	 * @param bitWidth - op word size.
	 */
	public RoliOpCircuit(CircuitGlobals globals, int bitWidth) {
		super(globals, 6 * bitWidth, bitWidth, 3 + bitWidth, "ROL_" + bitWidth);
		this.opBitWidth = bitWidth;
		this.opControlBits = controlBits(bitWidth);

	}

	/**
	 * Calculates power of 2.
	 * 
	 * @param n
	 *            0 <= n < 32
	 */
	@SuppressWarnings("unused")
	private int powerOf2(int n) {
		return (1 << (n - 1));
	}

	/**
	 * Calculates the number of control bits needed for the specified word size.
	 * 
	 * @param wordSize
	 * @return
	 */
	private int controlBits(int wordSize) {
		return 32 - Integer.numberOfLeadingZeros(wordSize - 1);
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {

		/*-
		 * 1 UNSHARE.
		 * bitWidth * ROL_L_X circuits of width bitWidth, incremental shift.
		 * 1 MUX_K_N (K = bitWidth, Number of blocks = bitWidth, control_bits = controlBits(bitWidth).
		 * 1 SHARE.
		 * 
		 */

		int circuitNdx = 0;

		// Create the un-share component.
		unshareComponent = new UNSHARE(globals, opBitWidth);
		subCircuits[circuitNdx++] = unshareComponent;

		// Create opBitWidth components of type ROL_L_i.
		rolComponent = new ROL_L_X[opBitWidth];
		for (int i = 0; i < opBitWidth; i++) {
			rolComponent[i] = new ROL_L_X(globals, opBitWidth, i);
			subCircuits[circuitNdx++] = rolComponent[i];
		}

		// Create the mux-kn component.
		mux_kn = new MUX_K_N(globals, opBitWidth, opBitWidth, opControlBits);
		subCircuits[circuitNdx++] = mux_kn;

		// Create the share component.
		shareComponent = new SHARE(globals, opBitWidth);
		subCircuits[circuitNdx++] = shareComponent;

	}

	@Override
	protected void connectWires() {
		
		// Wire the 4 shared inputs to the un-share component.
		unshareComponent.connectWiresToXY(inputWires, 0, inputWires,
				3 * opBitWidth);

		// Wire the un-share outputs of the number to the rol* op circuits
		// inputs.
		for (int i = 0; i < opBitWidth; ++i) {
			for (int j = 0; j < opBitWidth; j++) {
				unshareComponent.outputWires[i].connectTo(
						rolComponent[j].inputWires, i);
			}
		}

		// Wire the un-share outputs of the shift to the mux_k_n op
		// circuits control inputs.
		int crtlInputOffset = opBitWidth * opBitWidth;
		for (int i=0; i < opControlBits; i++){
			unshareComponent.outputWires[i + opBitWidth].connectTo(
					mux_kn.inputWires, crtlInputOffset + i);			
		}

		// Wire rol* op circuits outputs to the mux_k_n inputs.
		for (int i = 0; i < opBitWidth; ++i) {
			for (int j = 0; j < opBitWidth; j++) {
				rolComponent[i].outputWires[j].connectTo(mux_kn.inputWires, i
						* opBitWidth + j);
			}
		}

		// Wire the mux_k_n outputs and the player's random values to the share
		// component inputs.
		shareComponent.connectWiresToInputs(inputWires, 2 * opBitWidth,
				inputWires, 5 * opBitWidth, mux_kn.outputWires, 0);

	}

	@Override
	protected void defineOutputWires() {
		for (int i = 0; i < opBitWidth; i++)
			 shareComponent.outputWires[i].connectTo(outputWires, i);

	}

}
