// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC;

/**
 * Server side of {@link OR_2_1}.
 * 
 */
class G_OR_2_1 extends OR_2_1 {
	public G_OR_2_1(CircuitGlobals globals) {
		super(globals);
	}

	protected void execYao() {
		fillTruthTable();
		encryptTruthTable();
		sendGTT();
		gtt = null;
	}
}
