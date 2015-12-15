package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;
import org.factcenter.fastgc.YaoGC.XOR_2_1;

/**
 * N bit data mux21 selector. The choice bit is in the highest position. If the
 * choices bit == 0 bValue is returned, if choices bit == 1 cValue is returned.
 * Input ports order is: [Player0_rValue, Player0_cShare, Player0_bShare,
 * Player0_aShare_bit] [Player1_rValue, Player1_cShare, Player1_bShare,
 * Player0_aShare_bit].
 */
public class MuxOpCircuit extends CompositeCircuit {

	private final int muxBitWidth;
	private final int selectionBitPos0;
	private final int selectionBitPos1;

	private UNSHARE unshareComponent;
	private XOR_2_1 xor21;
	private MUX_2L_L mux2L_L;
	private SHARE shareComponent;

	/**
	 * Constructs a new {@code MuxOpCircuit} object.
	 * @param globals - Global circuit parameters.
	 * @param cntBits - op word size.
	 */
	public MuxOpCircuit(CircuitGlobals globals, int cntBits) {
		super(globals, 6 * cntBits + 2, cntBits, 4, "Mux_" + cntBits + "bits");

		muxBitWidth = cntBits;
		selectionBitPos0 = 3 * muxBitWidth;
		selectionBitPos1 = 6 * muxBitWidth + 1;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {

		// Create the sub circuits.
		unshareComponent = new UNSHARE(globals, muxBitWidth);
		xor21 = new XOR_2_1(globals);
		mux2L_L = new MUX_2L_L(globals, muxBitWidth);
		shareComponent = new SHARE(globals, muxBitWidth);

		int i = 0;
		subCircuits[i++] = unshareComponent;
		subCircuits[i++] = xor21;
		subCircuits[i++] = mux2L_L;
		subCircuits[i++] = shareComponent;

	}

	protected void connectWires() {

		// Wire the inputs of the shared choice bit of aVal to the xor gate.
		inputWires[selectionBitPos0].connectTo(xor21.inputWires, 0);
		inputWires[selectionBitPos1].connectTo(xor21.inputWires, 1);

		// Wire the 4 shared inputs (bVal and cVal) to the un-share component.
		unshareComponent.connectWiresToXY(inputWires, muxBitWidth, inputWires,
				4 * muxBitWidth + 1);

		// Wire the un-share outputs to the op circuit inputs. Note that the
		// input order of bValue and cValue was reversed to return bValue when the
		// selection bit = 0.
		mux2L_L.connectWiresToXY(unshareComponent.outputWires, muxBitWidth,
				unshareComponent.outputWires, 0);
		// Wire the choice bit to the mux21L highest input bit.
		xor21.outputWires[0].connectTo(mux2L_L.inputWires, 2 * muxBitWidth);

		// Wire the op outputs and the player's random values to the share
		// component inputs.
		shareComponent.connectWiresToInputs(inputWires, 0, inputWires,
				3 * muxBitWidth + 1, mux2L_L.outputWires, 0);

	}

	protected void defineOutputWires() {
		for (int i = 0; i < muxBitWidth; i++)
			 shareComponent.outputWires[i].connectTo(outputWires, i);
	}

}
