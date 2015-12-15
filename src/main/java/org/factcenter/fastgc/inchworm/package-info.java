/**
 * This package contains Yao's garbled circuits implementations of op circuits using
 * the FastGC framework. Each op has a dedicated circuit (usually a subclass of the 
 * {@link org.factcenter.fastgc.YaoGC.CompositeCircuit} class), and a dedicated wrapper class for using the op inside an
 * Inchworm virtual machine (a subclass of {@link org.factcenter.fastgc.inchworm.InchwormOpCommon}).
 * 
 * OpCircuit : Classes implementing complex circuit ops using simpler building blocks.
 * Implemented ops: AddOpCircuit,  AndOpCircuit,  DivOpCircuit,  MulOpCircuit, MuxOpCircuit,  OrOpCircuit, RoliOpCircuit, XorOpCircuit
 * 
 * {@link org.factcenter.fastgc.inchworm.InchwormOpCommon} - Abstract class for using both the client and server versions of the ops circuits
 * inside an Inchworm VM.
 * Direct Subclasses: {@link org.factcenter.fastgc.inchworm.AddOpInchworm}, {@link org.factcenter.fastgc.inchworm.AndOpInchworm}, {@link org.factcenter.fastgc.inchworm.DivOpInchworm}, {@link org.factcenter.fastgc.inchworm.MulOpInchworm}, {@link org.factcenter.fastgc.inchworm.MuxOpInchworm}, {@link org.factcenter.fastgc.inchworm.OrOpInchworm}, {@link org.factcenter.fastgc.inchworm.RoliOpInchworm}
 * 
 */
package org.factcenter.fastgc.inchworm;