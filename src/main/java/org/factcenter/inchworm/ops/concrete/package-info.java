/**
 * 
 * Implementation of the Inchworm virtual machine concrete ops.
 *
 * These  packages contain the interfaces and  implementations of the concrete ops the
 * Inchworm virtual machine executes. The set of ops being loaded into the virtual
 * machine is set for both players in their base constructor.
 *
 * {@link org.factcenter.inchworm.ops.concrete.ConcreteOPFactory} Class responsible for creating the set of ops for the Inchworm
 * Virtual Machine.
 *
 * {@link org.factcenter.inchworm.ops.VMProtocolParty} Interface defining the set of common parameters that remain constant
 * for a single party throughout the protocol execution.
 * Implementing Classes: {@link org.factcenter.inchworm.ops.concrete.ConcreteCommon}.
 *
 * {@link org.factcenter.inchworm.ops.concrete.ConcreteCommon} Abstract class defining the common parameters and initialization of
 * all concrete ops.
 * Direct Subclasses:  All concrete ops (*Op, {@link org.factcenter.inchworm.ops.concrete.FastMux}, {@link org.factcenter.inchworm.ops.concrete.FastUnMux}).
 * 
 */
package org.factcenter.inchworm.ops.concrete;