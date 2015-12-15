package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.CompositeCircuit;
import org.factcenter.fastgc.YaoGC.Wire;
import org.factcenter.fastgc.YaoGC.XOR_L_1;

/**
 * 
 * input: [NUM0|NUM1|..|NUMM]
 * output NUM0 xor NUM1 xor .. xor NUMM
 * Each number is dataWidth bits.
 * There are xorInputWidth numbers
 */
public class XOR_ML_L extends CompositeCircuit {

	private int xorInputWidth;
	private int dataWidth;

	public XOR_ML_L(CircuitGlobals globals, int numCount, int numWidth) {
		super(globals, numCount * numWidth, numWidth, numWidth, "XOR_" + numCount + "_" + numWidth);
		this.xorInputWidth = numCount;
		this.dataWidth = numWidth;
	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		for (int i = 0; i < dataWidth; i++) 
			subCircuits[i] = new XOR_L_1(globals, xorInputWidth);
	}

	protected void connectWires() {
		for (int inputIndex = 0 ; inputIndex < xorInputWidth*dataWidth ; inputIndex++){
			inputWires[inputIndex].connectTo(subCircuits[inputIndex % dataWidth].inputWires, inputIndex / dataWidth);
		}
	}

	protected void defineOutputWires() {
		for (int i = 0; i < dataWidth; i++){
			 subCircuits[i].outputWires[0].connectTo(outputWires, i);
		}
	}
	
	public void setInputLine(Wire[] wires, int offsetInWires, int lineIndex){
		int lineOffset = lineIndex * dataWidth;
		for (int i = 0 ; i < dataWidth ; i++){
			//System.out.println("Setting input number " + (lineOffset+i));
			wires[i + offsetInWires].connectTo(inputWires, lineOffset + i);
		}
	}

}