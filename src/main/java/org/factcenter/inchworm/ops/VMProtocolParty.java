package org.factcenter.inchworm.ops;

import org.factcenter.inchworm.VMRunner;
import org.factcenter.inchworm.VMState;

import java.io.IOException;
import java.util.Random;

/**
 * Interface defining the set of common parameters that remain constant for a 
 * single party throughout the protocol execution.
 * 
 */
public interface VMProtocolParty {
	/**
	 * Return the id of the associated party.
	 * 
	 * @return One party should return 0, the other should return 1.
	 */
	public int getPlayerId();

	/**
	 * Set parameters that remain constant for a single party throughout the protocol execution.
	 * 
	 * @param playerId
	 *            the index of the party in the secure computation (e.g., if there are two parties,
	 *            one will call this method with parameter 0, and the other with parameter 1).
	 * @param state
	 *            State parameters (the state should be considered read-only!)
	 * @param runner
	 *            Runner with channel to peer and utilities in secure-computation protocol.
	 * @param rand
	 *            randomness source for the protocol.
	 */
	public void setParameters(int playerId, VMState state, VMRunner runner, Random rand);

	/**
	 * Perform any required initialization. This might involve communication with peers. This method
	 * is meant for setup that is performed once; "setup" that is performed for every protocol
	 * execution should be done in the protocol itself.
	 * 
	 * Call this after calling {@link #setParameters(int, VMState, VMRunner, Random)}.
     *
     * Instantiations should deal gracefully with multiple init calls.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void init() throws IOException, InterruptedException;

}
