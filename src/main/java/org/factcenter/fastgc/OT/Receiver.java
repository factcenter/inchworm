// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.OT;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;

public abstract class Receiver {
	protected BigInteger choices;
	protected int numOfChoices;
	protected ObjectInputStream ois;
	protected ObjectOutputStream oos;

	protected BigInteger[] data = null;

	public Receiver(int numOfChoices, ObjectInputStream in, ObjectOutputStream out) {
		this.numOfChoices = numOfChoices;
		ois = in;
		oos = out;
	}

	public void execProtocol(BigInteger choices) throws IOException {
		this.choices = choices;
	}

	public BigInteger[] getData() {
		return data;
	}
}
