/**
 * 
 * This package contains the setup needed for using the Inchworm virtual machine:
 * classes implementing players and applications for running and testing the VM 
 * secure computation.
 *
 * {@link org.factcenter.inchworm.Player} Class defining the common code of all players, assuming players
 * functionality are not symmetrical. Communication is also asymmetrical: The right player is a 
 * TCP client while the left is the TCP server.
 * 
 * {@link org.factcenter.inchworm.app.Run} - The main class for running one party in an Inchworm secure computation.
 * Which party, and the TCP endpoint of the peer is specified on the command line.
 *
 * {@link org.factcenter.inchworm.app.TwoPcApplication} - The main class for running a secure computation session from
 * a single executable.
 *
 */
package org.factcenter.inchworm.app;