package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;

/**
 * Mux_K_N - selects a single block out of numBlocks input blocks of K bits
 * each. The value of the control bits determines which input block the circuit
 * returns. Input ports order is: [blockSize bits each data_inputs] * numBlocks +
 * control_input_bits.
 */
public class MUX_K_N extends CompositeCircuit {

    /**
     * Size of each block (in bits).
     */
	private int blockSize;


    /**
     * Number of bocks
     */
    private int numBlocks;

    /**
     * number of control bits (numBlocks <= 2^controlBits)
     */
	private int controlBits;

	/**
	 * Array of muxBitWidth {@link MUX_K_1} instances.
	 */
	private MUX_K_1[] mux_k_1Components;

	/**
	 * Constructs a new {@code MUX_K_N} object.
	 * 
	 * @param globals - Global circuit parameters.
	 * @param blockSize - input block word bit size.
	 * @param cntControlBits - number of selection control bits.
	 */
	public MUX_K_N(CircuitGlobals globals, int blockSize, int numBlocks, int cntControlBits) {
		super(globals, blockSize * numBlocks + cntControlBits, blockSize,
				blockSize, "MUX_" + blockSize + "_" + numBlocks);
		this.blockSize = blockSize;
        this.numBlocks = numBlocks;
		this.controlBits = cntControlBits;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {

		// Create blockSize components of type MUX_K_1.
		mux_k_1Components = new MUX_K_1[blockSize];
		for (int i = 0; i < blockSize; i++) {
			mux_k_1Components[i] = new MUX_K_1(globals, numBlocks,
                    controlBits);
			subCircuits[i] = mux_k_1Components[i];
		}
	}

	@Override
	protected void connectWires() {
		// Connect each of the data input blocks to the MUX_K_1 inputs. First
		// block bits connects to bit #0 of the MUX_K_1*, second block bits to
		// bit #1, etc...
		for (int blockNum = 0; blockNum < numBlocks; blockNum++) {
			for (int j = 0; j < blockSize; j++) {
				inputWires[blockNum * blockSize + j].connectTo(
						mux_k_1Components[j].inputWires, blockNum);
			}
		}

		// Connect the control signals to the MUX_K_1 components
		int crtlInputOffset = blockSize * numBlocks;
		for (int i = 0; i < blockSize; i++) {
			for (int j = 0; j < controlBits; j++) {
				inputWires[crtlInputOffset + j].connectTo(
						mux_k_1Components[i].inputWires, numBlocks + j);
			}
		}
	}

	@Override
	protected void defineOutputWires() {
		for (int i = 0; i < blockSize; i++) {
			 mux_k_1Components[i].outputWires[0].connectTo(outputWires, i);
		}

	}

}
