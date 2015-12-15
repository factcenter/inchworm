// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC;

import org.factcenter.qilin.comm.SendableInput;
import org.factcenter.qilin.comm.SendableOutput;

public abstract class CompositeCircuit extends Circuit {
	protected Circuit[] subCircuits;
	protected int nSubCircuits;

	public CompositeCircuit(CircuitGlobals globals, int inDegree, int outDegree, int nSubCircuits, String name) {
		super(globals, inDegree, outDegree, name);

		this.nSubCircuits = nSubCircuits;

		subCircuits = new Circuit[nSubCircuits];
	}

	/**
	 * Recursively create sub-circuits and connect the wires.
	 */
	@Override
	public void build(boolean isForGarbling) {
		createInputWires();
		createAllSubCircuits(isForGarbling);
		for (int i = 0; i < nSubCircuits; i++) {
            assert(subCircuits[i] != null);
			subCircuits[i].setIOStream(ois, oos);
			subCircuits[i].build(isForGarbling);
		}
		connectWires();
		defineOutputWires();
		fixInternalWires();
	}
	

	@Override
	public void setIOStream (SendableInput ois, SendableOutput oos) {
		super.setIOStream(ois, oos);
		for (int i = 0; i < nSubCircuits; i++)
			if (subCircuits[i] != null) 
				subCircuits[i].setIOStream(ois, oos);
	}

	/**
	 * Create instances of the subCircuit classes.
	 * (this method should fill the array {@link #subCircuits}). 
	 * @param isForGarbling is this a server instance (servers create the garbled circuits, clients evaluate them).
	 */
	protected abstract void createAllSubCircuits(boolean isForGarbling);

	/**
	 * Connect local wires (between the circuit and it's immediate subcircuits).
	 * Note that the {@link #build(boolean)} method descends recursively and
	 * calls this method for each of the subcircuits.
	 */
	abstract protected void connectWires();
	abstract protected void defineOutputWires();
	protected void fixInternalWires() {}

	protected void compute() {}
	protected void execute() {}
}
