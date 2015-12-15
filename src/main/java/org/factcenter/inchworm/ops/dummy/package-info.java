/**
 * 
 * Implementation of the Inchworm virtual machine dummy ops.
 *
 * These  packages contain the interfaces and  implementations of the dummy ops the
 * Inchworm virtual machine executes.  The set of ops being loaded into the virtual
 * machine is set for both players in their base constructor.
 *
 * {@link org.factcenter.inchworm.ops.dummy.DummyOPFactory} Class responsible for creating the set of ops for the Inchworm
 * Virtual Machine.
 *
 * {@link org.factcenter.inchworm.ops.VMProtocolParty} Interface defining the set of common parameters that remain constant
 * for a single party throughout the protocol execution.
 * Implementing Classes: {@link org.factcenter.inchworm.ops.dummy.ProtocolInfo}.
 *
 * {@link org.factcenter.inchworm.ops.dummy.ProtocolInfo} Abstract class defining the common parameters and initialization of all
 * dummy ops. The method used for sharing of the computation results between the player 
 * is controlled by the useRandom flag
 * Direct Subclasses:  All dummy ops (Dummy*).
 *
 */
package org.factcenter.inchworm.ops.dummy;