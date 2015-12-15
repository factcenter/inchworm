package org.factcenter.inchworm.ops.concrete;

import org.factcenter.inchworm.VMRunner;
import org.factcenter.qilin.protocols.BulkOT.SplitReceiver;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;
import java.util.Random;
import java.util.Stack;

public class FastMux {
	/**
	 * Execute the FastMux protocol bit after bit (MSB to LSB) for loading part of the shared memory.
	 * 
	 * @param blockBitLength
	 * @param idxShare
	 * @param memShare
	 * @param otExtender
	 * @param rand
	 * @throws IOException
	 */
	public static BitMatrix fastMuxMultiple(int blockBitLength, long idxShare, BitMatrix memShare,
											OTExtender otExtender, Random rand, VMRunner runner, int playerNum,
											Stack<byte[]> memParts0, Stack<byte[]> memParts1) throws IOException {

		/*-
		 *  Execute the SSMux protocol bit after bit (MSB to LSB) for loading 'our' share of the memory.
		 */
		
		int numberOfAddressBits = getAddressWidth(blockBitLength, memShare.getNumCols());

		BitMatrix curMemShare = memShare;

		for (int addressBitIndex = numberOfAddressBits; addressBitIndex > 0; addressBitIndex--) {
			// Get the value of the current bit in the memory position.
			int currentAddressBitValue = getBitValue((int) idxShare, addressBitIndex - 1);
			// Get the a share of the 'correct half' the current memory block.
			curMemShare = fastMuxSingle(curMemShare, currentAddressBitValue, otExtender, rand, memParts0, memParts1);
		}
		return curMemShare;

	}

	/**
	 * FastMUX protocol implementation (using the BitMatrix class).
	 * 
	 * @param memShare
	 * @param choiceBit
	 * @param otExtender
	 * @param rand
	 * @throws IOException
	 */
	public static BitMatrix fastMuxSingle(BitMatrix memShare, int choiceBit, OTExtender otExtender,
                                   Random rand, Stack<byte[]> memParts0, Stack<byte[]> memParts1) throws IOException {

		int memShareHalfSize = memShare.getNumCols() / 2;

		/*-
		 *  1) Split the current memory block.
		 */
		BitMatrix M0 = memShare.getSubMatrixCols(0, memShareHalfSize);
		BitMatrix M1 = memShare.getSubMatrixCols(memShareHalfSize, memShareHalfSize);
		
		if (memParts0 != null)
			memParts0.push(M0.getPackedBits(true));
		if (memParts1 != null)
			memParts1.push(M1.getPackedBits(true));

		/*-
		 *  2) Prepare a random string for masking.
		 */
		BitMatrix rho = new BitMatrix(memShareHalfSize);
		rho.fillRandom(rand);

		BitMatrix x0 = rho.clone();
		BitMatrix x1 = rho.clone();

		if (choiceBit == 0) {
			x0.xor(M0);
			x1.xor(M1);
		} else {
			x0.xor(M1);
			x1.xor(M0);
		}

		/*-
		 *  3) Execute a 1-out-of-2 string OT.
		 */
		BitMatrix c = BitMatrix.valueOf(choiceBit, 1);

		SplitReceiver.State state = otExtender.receiveWritingPhase(c);
		otExtender.send(x0, x1);
		BitMatrix newShare = otExtender.receiveReadingPhase(state);

		newShare.xor(rho);
		return newShare;
	}



	/**
	 * Returns the address width (in bits) of the shared memory block.
	 *
	 * @param blockBitLength Length of block in bits.
	 * @param memBitLength Length of memory in bits
	 */
	final static int getAddressWidth(int blockBitLength, int memBitLength) {
		int numBlocks = memBitLength / blockBitLength;
		return 32 - Integer.numberOfLeadingZeros(numBlocks - 1);
	}

	/*-
	 * ----------------------------------------------------------------
	 *                   Private Methods
	 * ----------------------------------------------------------------
	 */

    /**
     * Get the value of a specific bit in the passed integer.
     *
     * @param value
     *            - integer value to check.
     * @param zeroBasedPosition
     *            - bit position in passed integer (LSB = bit #0).
     */
    private static int getBitValue(int value, int zeroBasedPosition) {

        if ((value & (1L << zeroBasedPosition)) != 0) {
            // The bit was set.
            return 1;
        }
        return 0;
    }
}
