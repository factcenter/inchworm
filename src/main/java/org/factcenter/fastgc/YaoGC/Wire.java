// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;

/**
 * Models a wire in a circuit.
 * Note that due to the fastGC construction, wires must "remember" who holds references to them. So never assign a wire
 * into an array of Wires directly, use {@link #connectTo(Wire[], int)} instead (this updates the references correctly).
 */
public class Wire extends TransitiveObservable {
	public static final int UNKNOWN_SIG = -1;

	/**
	 * Global parameters (shared by all wires in the circuit).
	 */
	CircuitGlobals globals;

	/**
	 * Pointer to {@link CircuitGlobals#R}
	 */
	final BigInteger R;

	/**
	 * Serial number of this wire. Serial numbers are used in the construction
	 * of garbled truth tables (so that two gates with the same input wires will
	 * still generate different garblings).
	 */
	public int serialNum;

	/**
	 * This wire's integer value. Can be 0, 1 or {@link #UNKNOWN_SIG} (meaning
	 * it hasn't been evaluated/decrypted yet).
	 */
	public int value = UNKNOWN_SIG;

	/**
	 * This wire's current label.
	 */
	public BigInteger lbl;

	/**
	 * This wire is inverted. (a 0 value on the wire should give a 1 output, and
	 * vice versa).
	 */
	public boolean invd = false;

    /**
     * Holds a reference to a wire.
     */
    public static class Reference {
        public Wire[] ws;
        public int idx;

        public Reference(Wire[] ws, int idx) {
            this.ws = ws;
            this.idx = idx;
        }

        @Override
        public int hashCode() {
            return ws.hashCode() + idx * 0x1234567;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof  Reference))
                return false;
            Reference other = (Reference) obj;

            return ws == other.ws && idx == other.idx;
        }
    }

    Collection<Reference> references = new HashSet<>();

    /**
     * When replacing an existing wire, update references to that wire
     * with references to this one.
     * @param w wire that is being replaced.
     */
    protected void updateReferences(Wire w) {
        for (Reference ref : w.references) {
            ref.ws[ref.idx] = this;
            references.add(ref);
        }
    }

	public Wire(CircuitGlobals globals, Wire[] ws, int idx) {
		this.globals = globals;
		this.R = globals.R;
		this.serialNum = globals.totalWires++;
		lbl = new BigInteger(globals.labelBitLength, globals.rand);

        if (ws != null) {
            if (ws[idx] != null) {
                updateReferences(ws[idx]);
            }
            references.add(new Reference(ws, idx));
            ws[idx] = this;
        }
	}

	public BigInteger[] newLabelPair() {
		BigInteger[] res = new BigInteger[2];
		res[0] = new BigInteger(globals.labelBitLength, globals.rand);
		res[1] = conjugate(R, res[0]);
		return res;
	}

	/**
	 * Compute the label corresponding to the negation of this wire's value.
	 * This method uses the "free XOR" technique to compute the value.
	 * 
	 * @param label
	 */
	public static BigInteger conjugate(BigInteger R, BigInteger label) {
		if (label == null)
			return null;

		return label.xor(R.shiftLeft(1).setBit(0));
	}

	public BigInteger conjugate() {
		return conjugate(R, lbl);
	}

	public void setLabel(BigInteger label) {
		lbl = label;
	}

	/**
	 * Notify observers that the wire's label has been set to a new value.
	 */
	public void setReady(int execSerial) {
		setChanged(execSerial);
		notifyObservers();
	}

    private void handleFixedWire(Circuit c) {
        c.inDegree--;
        if (c.inDegree == 0) {
            c.compute();
            for (int j = 0; j < c.outDegree; j++)
                c.outputWires[j].fixWire(c.outputWires[j].value);
        }
    }

	/**
	 * Connect a driving source (current circuit input wires or another circuit
	 * output) to an input port.
	 * 
	 * @param ws  - input wires of a (sub)circuit.
	 * @param idx - index of input to connect to.
	 */
	public void connectTo(Wire[] ws, int idx) {
		Wire w = ws[idx];

        if (w != null) {
            for (int i = 0; i < w.observers.size(); i++) {
                TransitiveObserver ob = w.observers.get(i);
                TransitiveObservable.Socket s = w.exports.get(i);
                this.addObserver(ob, s);
                s.updateSocket(this);
                if (value != UNKNOWN_SIG) {
                    // The wire is fixed,

                    // Fixed wires should have called fixWire()
                    assert (execSerial == Integer.MAX_VALUE);
                    Circuit c = (Circuit) ob;
                    handleFixedWire(c);
                }
            }

            w.deleteObservers();

            updateReferences(w);
            w.references.clear();
        }

        references.add(new Reference(ws, idx));
		ws[idx] = this;
	}

	public void fixWire(int v) {
		this.value = v;
        // This doesn't need to percolate...
        this.execSerial = Integer.MAX_VALUE;

		for (int i = 0; i < this.observers.size(); i++) {
			Circuit c = (Circuit) this.observers.get(i);
			handleFixedWire(c);
		}
	}

	protected static int getLSB(BigInteger lp) {
		return lp.testBit(0) ? 1 : 0;
	}
}
