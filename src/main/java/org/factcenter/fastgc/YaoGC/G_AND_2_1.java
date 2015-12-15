// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC;

/**
 * Server side of {@link AND_2_1}.
 * 
 */
class G_AND_2_1 extends AND_2_1 {
	public G_AND_2_1(CircuitGlobals globals) {
		super(globals);
	}

	protected void execYao() {
		fillTruthTable();
		encryptTruthTable();
		sendGTT();
		gtt = null;
	}
}
