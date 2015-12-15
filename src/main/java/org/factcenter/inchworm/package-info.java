/**
 * 
 * This package contains the implementation  of the Inchworm virtual machine a virtual 
 * PC using secure computation. 
 * The virtual machine is implemented using  two classes: {@link org.factcenter.inchworm.VMState} which contains the
 * current state of the virtual-machine, and {@link org.factcenter.inchworm.VMRunner} which is responsible for running
 * the secure computation programs.
 *
 * {@link org.factcenter.inchworm.VMState} - This class contains the loaded program, the data, and a snapshot of the current
 * state of the virtual-machine (the current instruction, program counter, and memory).  
 * This class provides many utility methods for manipulating (retrieve / write) the state.
 *
 * {@link org.factcenter.inchworm.VMRunner} This class contains utility methods for  executing  the loaded program (the
 * series of instructions, stored in the VMState program memory)  in the virtual-machine,
 * using the pre-defined set of op implementations.
 *
 * {@link org.factcenter.inchworm.VMOpImplementation} Interface defining the current set of op implementations the VM executes.
 * Direct Subclasses: {@link org.factcenter.inchworm.ops.concrete.ConcreteOPFactory}, {@link org.factcenter.inchworm.ops.dummy.DummyOPFactory}.
 *
 */
package org.factcenter.inchworm;