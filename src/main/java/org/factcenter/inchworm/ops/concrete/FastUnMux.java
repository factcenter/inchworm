package org.factcenter.inchworm.ops.concrete;

import org.factcenter.inchworm.VMRunner;
import org.factcenter.qilin.protocols.BulkOT.SplitReceiver;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;
import java.util.Random;
import java.util.Stack;

public class FastUnMux {

	/**
	 * Execute the SSUNMux protocol bit after bit (MSB to LSB) for storing into the shared memory.
	 * 
	 * @param valueShare A share of the value to be stored.
	 * @param blockBitLength the size of the memory block to write in bits.
	 * @param idxShare A share of the index to write to (the index is the block number, starting from 0). 
	 * @param memShare A share of the memory being written (the memory treated as a packed sequence of blocks).
	 * @param otExtender The OT Extender used by the concrete ops.
	 * @param rand the randomness source for the ops.
	 * @param runner VMRunner of the current player.
	 * @param playerNum player ID.
//	 * @param memParts0 stack of memory lower halves.
//	 * @param memParts1	stack of memory higher halves.
	 * @throws IOException
	 */
	public static BitMatrix fastUnMuxMultiple(BitMatrix valueShare, int blockBitLength, long idxShare, BitMatrix memShare,
											  OTExtender otExtender, Random rand, VMRunner runner, int playerNum,
											  Stack<byte[]> memParts0, Stack<byte[]> memParts1) throws IOException {

        memParts0 = new Stack<>();
        memParts1 = new Stack<>();

		/*-
		 *  First split the memory using the FastMux protocol bit after bit (MSB to LSB)
		 *  for getting the share of the memory we write into.
		 */
		FastMux.fastMuxMultiple(blockBitLength, idxShare, memShare, otExtender, rand,
                runner, playerNum, memParts0, memParts1);

		// Clone valueShare into memSharePart.
		BitMatrix memSharePart = valueShare.clone();
	
		int numberOfAddressBits = FastMux.getAddressWidth(blockBitLength, memShare.getNumCols());

		int partBitLength = blockBitLength;

		/**
		 * Now write the new value into memSharePart, and use the stored halves to rebuild the
		 * memory
		 */
		for (int addressBitIndex = 0; addressBitIndex < numberOfAddressBits; addressBitIndex++) {
			int currentAddressBitValue = getBitValue((int) idxShare, addressBitIndex);
			// Combine the two parts of the shared memory.
			memSharePart = fastUnMuxSingle(memSharePart, currentAddressBitValue, partBitLength, memParts0, memParts1, otExtender, rand);
			partBitLength = 2 * partBitLength;
		}

		return memSharePart;
		
	}
	/*-
	 * ----------------------------------------------------------------
	 *                   Private Methods.
	 * ----------------------------------------------------------------
	 */

	/**
	 * fastUnMux protocol implementation.
	 * @param currMemShare part o the memory to store into.
	 * @param choiceBit current address bit value.
	 * @param partBitLength size of current memory block in bits.
	 * @throws IOException
	 */
	public static BitMatrix fastUnMuxSingle(BitMatrix currMemShare, int choiceBit, int partBitLength, Stack<byte[]> memParts0, Stack<byte[]> memParts1, OTExtender otExtender, Random rand) throws IOException {

		/*-
		 *  1) Pop memory parts and prepare a random string.
		 */
		byte[] m0 = memParts0.pop();
		byte[] m1 = memParts1.pop();
		
		// Calculate number of bytes for the new concatenated block.
		int newBlockBitLength = 2 * partBitLength;
		int newBlockNumBytes = newBlockBitLength >> 3;
		if ((newBlockBitLength % 8) != 0)
			++newBlockNumBytes;

		// Allocate a random string.
		BitMatrix rho = new BitMatrix(newBlockBitLength);
		rho.fillRandom(rand);
		
		/*-
		 *  2) Combine the memory parts based on the choice bit.
		 */
		byte[] currMemBytes = currMemShare.getPackedBits(true);
		byte[] x0;
		byte[] x1;
		if (choiceBit == 0) {
			x0 = concatenateEqualBlocks(currMemBytes, m1, partBitLength, newBlockNumBytes);
			x1 = concatenateEqualBlocks(m0, currMemBytes, partBitLength, newBlockNumBytes);
		} else {
			x0 = concatenateEqualBlocks(m0, currMemBytes, partBitLength, newBlockNumBytes);
			x1 = concatenateEqualBlocks(currMemBytes, m1, partBitLength, newBlockNumBytes);
		}

		/*-
		 *  3) Execute a 1-out-of-2 string OT.
		 */
		BitMatrix bm0 = new BitMatrix(x0, 0);
        bm0.subcolumns(0, newBlockBitLength);
		BitMatrix bm1 = new BitMatrix(x1, 0);
        bm1.subcolumns(0, newBlockBitLength);
		bm0.xor(rho);
		bm1.xor(rho);

		BitMatrix c = new BitMatrix(1);
		c.setBit(0, choiceBit);

		SplitReceiver.State state = otExtender.receiveWritingPhase(c);
		otExtender.send(bm0, bm1);
		BitMatrix newShare = otExtender.receiveReadingPhase(state);

		newShare.xor(rho);
		return newShare;
		
	}


	/**
	 * Concatenates two equal parts of memory block.
	 * @param memDataLow lower memory part
	 * @param memDataHigh higher memory part
	 * @param cntPartBits number of bits in each part
	 * @param totalBytes size of new memory block in bytes.
	 * @return the concatenated memory.
	 */
	static byte[] concatenateEqualBlocks(byte[] memDataLow, byte[] memDataHigh, int cntPartBits, int totalBytes) {
		assert memDataHigh.length >= (cntPartBits + 7) / 8;
        assert memDataLow.length >= (cntPartBits + 7) / 8;

		int cntPartBytes = memDataLow.length;
		byte[] newMem = new byte[totalBytes];

		// Copy low part into new memory block.
		System.arraycopy(memDataLow, 0, newMem, 0, cntPartBytes);
		// Now copy the high part of the memory into the block.
		writeBitsToBlockUpperPart(cntPartBits, memDataHigh, cntPartBits, newMem);

		return newMem;
	}
	
	/**
	 * Concatenates two parts into a new memory block.
	 * 
	 * @param dataLow
	 *            lower block part
	 * @param dataHigh
	 *            higher block part
	 * @param cntDataLowBits
	 *            number of bits in low part
	 * @param cntDataHighBits
	 *            number of bits in high part
	 * @return the concatenated memory.
	 */
	public static byte[] concatenateBlocks(byte[] dataLow, byte[] dataHigh, int cntDataLowBits,
			int cntDataHighBits) {

		int cntLowPartBytes = cntDataLowBits >> 3;
		if ((cntDataLowBits % 8) != 0)
			++cntLowPartBytes;
		
		int totalBits = cntDataLowBits + cntDataHighBits;
		int totalBytes = totalBits >> 3;
		if ((totalBits % 8) != 0)
			++totalBytes;
		
		byte[] newMem = new byte[totalBytes];

		// Copy low part into new memory block.
		System.arraycopy(dataLow, 0, newMem, 0, cntLowPartBytes);
		// Now copy the high part of the memory into the new block.
		FastUnMux.writeBitsToBlockUpperPart(cntDataHighBits, dataHigh, cntDataLowBits,
                newMem);

		return newMem;
	}
	
	/*-
	 * ----------------------------------------------------------------
	 *                   Private Methods.
	 * ----------------------------------------------------------------
	 */

	/**
	 * Writes a specified number of bits from a source byte array to the upper half of the
	 * shared memory block array being reconstructed.
	 * This isn't always a simple arraycopy because the bits might not be byte-aligned.
	 * 
	 * @param blockBitLength
	 *            - number of bits to save.
	 * @param sourceArr
	 *            - source of data to save.
	 * @param startBitPos
	 *            - first bit position in shared memory block (end of lower part).
	 * @param blockShare
	 *            - shared memory block to write to.
	 */
	private static void writeBitsToBlockUpperPart(int blockBitLength, byte[] sourceArr, int startBitPos,
			byte[] blockShare) {

        // Make sure the source array has enough bits to copy out of.
        assert sourceArr.length >= (blockBitLength + 7) / 8;
        // Make sure we have enough room to copy into.
        assert blockShare.length >= (startBitPos + blockBitLength + 7) / 8;

		// High / low bits of non-full byte.
		int highPartBits = startBitPos % 8;
		int lowPartBits = 8 - highPartBits;

		int sourceByteNdx = 0;
		int currByteValue = sourceArr[0];
		int memShareByteNdx = startBitPos / 8;

        // Check if bits are byte-aligned
		if (highPartBits != 0) {
            // Not byte aligned, we have to shift while copying.
			if (blockBitLength < lowPartBits) {
				//
				// The block we write fits the first byte of the new memory shared block. Prepare a
				// mask for 'shading' the bits inside the byte being written to.
				//
				int bitMask = (1 << blockBitLength) - 1;
				blockShare[memShareByteNdx] &= ~(bitMask << highPartBits);
				blockShare[memShareByteNdx] |= (currByteValue & bitMask) << highPartBits;
				return;
			}
			/*
			 * Put LSB of current byte value in MSBits of non-full byte at lower memory part, and
			 * the MSB bits in the LSB bits of the next memShare byte.
			 */
			byte bitMask = (byte) ((1 << highPartBits) - 1);
			blockShare[memShareByteNdx] &= bitMask;
			while (blockBitLength > 7) {
				blockShare[memShareByteNdx] |= (currByteValue << highPartBits);
				memShareByteNdx++;
				blockShare[memShareByteNdx] |= ((currByteValue & 0xff) >>> lowPartBits);
				blockBitLength -= 8;
				sourceByteNdx++;
				currByteValue = sourceArr[sourceByteNdx];
			}
			if (blockBitLength > 0) {
				blockShare[memShareByteNdx] |= (currByteValue << highPartBits);
				blockBitLength -= lowPartBits;
				if (blockBitLength > 0) {
					memShareByteNdx++;
					blockShare[memShareByteNdx] |= ((currByteValue & 0xff) >>> lowPartBits);
				}
			}
		} else {
			// Byte-aligned: Just append the sourceArr to the new shared memory block.
			int cntBytes = sourceArr.length;
			System.arraycopy(sourceArr, 0, blockShare, memShareByteNdx, cntBytes);
		}
	}

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
