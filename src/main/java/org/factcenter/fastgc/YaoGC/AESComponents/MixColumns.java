// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package org.factcenter.fastgc.YaoGC.AESComponents;

import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;
import org.factcenter.fastgc.YaoGC.State;

public class MixColumns extends CompositeCircuit {

	public MixColumns(CircuitGlobals globals) {
		super(globals, 128, 128, 4, "MixColumns");
	}

	public State startExecuting(State[] arrS) {
		for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 8; j++) {
                inputWires[i * 8 + j].value = arrS[i].wires[j].value;
                inputWires[i * 8 + j].invd = arrS[i].wires[j].invd;
                inputWires[i * 8 + j].setLabel(arrS[i].wires[j].lbl);
                inputWires[i * 8 + j].setReady(arrS[i].execSerial);
            }
        }

		return State.fromWires(outputWires);
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		for (int i = 0; i < 4; i++) {
			subCircuits[i] = new MixOneColumn(globals);
		}
	}

	protected void connectWires() {
		for (int i = 0; i < 4; i++)
			for (int j = 0; j < 32; j++)
				inputWires[i * 32 + j].connectTo(subCircuits[i].inputWires, j);
	}

	protected void defineOutputWires() {
		for (int i = 0; i < 4; i++) {
			System.arraycopy(subCircuits[i].outputWires, 0, outputWires,
					i * 32, 32);
		}
	}
}