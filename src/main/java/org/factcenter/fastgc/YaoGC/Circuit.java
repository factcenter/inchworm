// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC;

import org.factcenter.qilin.comm.SendableInput;
import org.factcenter.qilin.comm.SendableOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;

abstract public class Circuit implements TransitiveObserver {

	/**
	 * Circuit serial number for debugging.
	 */
	public final int serialNo;

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected CircuitGlobals globals;

	/**
	 * Flag denoting whether this is a "server" instance of the circuit. The
	 * server instance computes the encrypted truth tables, while the client
	 * instance is responsible for evaluating the circuit.
	 */
	// protected boolean isForGarbling;

	public Wire[] inputWires;
	public Wire[] outputWires;

	protected int inDegree, outDegree;
	protected String name;

	protected SendableOutput oos = null;
	protected SendableInput ois = null;

	/**
	 * Number of times an input wire has been updated. Used during circuit
	 * execution to keep track of when the gate should be evaluated.
	 */
	private int inputWireCount = 0;

	public int getInDegree() {
		return inDegree;
	}


    public int getOutDegree() {
        return outDegree;
    }
	
	public Circuit(CircuitGlobals globals, int inDegree, int outDegree,
			String name) {
		this.serialNo = globals.totalCircuits++;
		this.globals = globals;
		this.inDegree = inDegree;
		this.outDegree = outDegree;
		this.name = name;

		inputWires = new Wire[inDegree];
		outputWires = new Wire[outDegree];
	}

	/**
	 * Set parameters. Must call this before executing circuit.
	 * 
	 * @param ois
	 * @param oos
	 */
	public void setIOStream(SendableInput ois, SendableOutput oos) {
		this.ois = ois;
		this.oos = oos;
	}

	/**
	 * Construct any required sub-circuits, connect wires, etc.
	 */
	abstract public void build(boolean isForGarbling);

	/**
	 * Create the input wires.
	 */
	protected void createInputWires() {
		for (int i = 0; i < inDegree; i++) {
            new Wire(globals, inputWires, i);
		}
	}

	public void startExecuting(int[] vals, boolean[] invd, BigInteger[] glbs, int execSerial)
			throws IOException {
		if (vals.length != invd.length || invd.length != glbs.length
				|| glbs.length != this.inDegree)
			throw new IOException("Unmatched number of input labels.");

		for (int i = 0; i < this.inDegree; i++) {
			inputWires[i].value = vals[i];
			inputWires[i].invd = invd[i];
			inputWires[i].setLabel(glbs[i]);
			inputWires[i].setReady(execSerial);
		}
	}

	public State startExecuting(State s) {
		if (s.getWidth() != this.inDegree) {
			Exception e = new Exception("Unmatched number of input labels."
					+ s.getWidth() + " != " + inDegree);
			e.printStackTrace();
			System.exit(1);
		}

		for (int i = 0; i < this.inDegree; i++) {
			inputWires[i].value = s.wires[i].value;
			inputWires[i].invd = s.wires[i].invd;
			inputWires[i].setLabel(s.wires[i].lbl);
			inputWires[i].setReady(s.execSerial);
		}

		return State.fromWires(this.outputWires);
	}

	public BigInteger interpretOutputELabels(BigInteger[] eLabels) {
		if (eLabels.length != outDegree)
			throw new RuntimeException("Length Error.");

		BigInteger output = BigInteger.ZERO;
		for (int i = 0; i < this.outDegree; i++) {
			if (outputWires[i].value != Wire.UNKNOWN_SIG) {
				if (outputWires[i].value == 1)
					output = output.setBit(i);
			} else if (eLabels[i]
					.equals(outputWires[i].invd ? outputWires[i].lbl
							: outputWires[i].conjugate())) {
				output = output.setBit(i);
			} else if (!eLabels[i].equals(outputWires[i].invd ? outputWires[i]
					.conjugate() : outputWires[i].lbl)) {
				logger.error(
						"Bad Label encountered at ouputWire[{}] (#{}) not in (0x{}, 0x{})",
						i, outputWires[i].serialNum, outputWires[i].lbl
								.toString(16), outputWires[i].conjugate()
								.toString(16));
				throw new RuntimeException(
						"Bad Label encountered at ouputWire[" + i + "]:\n"
								+ eLabels[i] + " is neither "
								+ outputWires[i].lbl + " nor "
								+ outputWires[i].conjugate());
			}
		}

		return output;
	}

	public void update(TransitiveObservable o, Object arg) {
		inputWireCount++;
		if (inputWireCount % inDegree == 0) {
			if (logger.isTraceEnabled()) {
				StringBuffer buf = traceMsg(inputWires);
				logger.trace("subcircuit {} executing.  Input labels ({}): {}",
						serialNo, inputWires.length, buf);
			}
			execute();
			if (logger.isTraceEnabled()) {
				StringBuffer buf = traceMsg(outputWires);
				logger.trace("subcircuit {} done.  Output labels ({}): {}",
						serialNo, outputWires.length, buf);
			}
		}
	}

	/**
	 * Trace helper method.
	 * 
	 * @param ioWire
	 */
	private StringBuffer traceMsg(Wire[] ioWire) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < ioWire.length; ++i) {
			buf.append(ioWire[i].serialNum).append(":0x")
					.append(ioWire[i].lbl.toString(16));
			if (ioWire[i].value != Wire.UNKNOWN_SIG)
				buf.append("[").append(ioWire[i].value).append("]");
			buf.append(" ");
		}
		return buf;
	}

	abstract protected void compute();

	abstract protected void execute();
}
