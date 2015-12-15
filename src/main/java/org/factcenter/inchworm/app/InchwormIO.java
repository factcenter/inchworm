package org.factcenter.inchworm.app;

import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;

/**
 * 
 * Handle input/output from the secure computation from/to a player. 
 *
 */
public interface InchwormIO {

	/**
	 * Output the secure computation result to the specified stream.
	 * @param stream - output stream number
	 * @param pc - program counter
	 * @param value - register value
	 * @throws IOException 
	 */
	public void output(long stream, long pc, BitMatrix value) throws IOException;
	
	/**
	 * Retrieve input from the player.
	 * The input value will be XORed with the value input by the other player.
	 * @param pc
	 * @return
	 * @throws IOException
	 */
	public BitMatrix input(long pc) throws IOException;

}
