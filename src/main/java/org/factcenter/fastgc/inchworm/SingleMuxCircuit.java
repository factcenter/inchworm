package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.*;
import java.math.BigInteger;


/**
 * 
 * @author mikegarts
 * 
 *         INPUT: [Player0_idx, Player0_idx', Player0_data, Player0_valid, Player0_rand,
 *         Player1_idx, Player1_idx', Player1_data, Player1_valid ,Player1_rand] 
 *         OUTPUT: {@link SingleChooseCircuit} or diagram for exact output definition
 */
public class SingleMuxCircuit extends CompositeCircuit {

	private final int indexLenBits;
	private final int dataLenBits;

	// OFFSETS in input bits
	private final int IDX0;
	private final int IDX1;
	private final int IDX_TAG0;
	private final int IDX_TAG1;
	private final int DATA0;
	private final int DATA1;
	private final int VALID0;
	private final int VALID1;
	private final int RAND0;
	private final int RAND1;

	private UNSHARE unshare1;

	private XOR_2L_L compareStep1; // next two xors compare the calculated (prev
									// step) index with the given one
	private OR_L_1 compareStep2; // in fact the implement comparator circuit

	private XOR_2L_L xorDataBeforeMux; // XOR the data inputs
	private MUX_2L_L chooseXoredDataOrZero; // mux to choose between data or
											// zeros

	private XOR_2L_L xorToCreateZeros;

	private SHARE reShareData; // re-share the output value
	private SHARE reShareValid; // re-share the output validBit
	private XOR_2_1 unshareValid; //unshare valid bit
	private AND_2_1 andCmp2WithValid; // and unshared valid with cmp bit
	private INVERTER negateCompareStep2;
	private BigInteger DATA_MASK;
	
	

	static int NUM_SUBCIRCUITS = 11;

	public static int calcInDegree(int dataLenBits, int indexLenBits){
		// (data{L,R}+dataRand{L,R}) + (oldIndex{L,R} + newIndex{L,R}) + valid{L,R} + RandValid{L,R} 
		return 4 * dataLenBits + 4 * indexLenBits + 2 + 2;
	}
	
	public static int calcOutDegree(int dataLenBits){
		return dataLenBits + 1;
	}
	
	public SingleMuxCircuit(CircuitGlobals globals, int indexLenBits,
							int dataLenBits) {
		super(globals, calcInDegree(dataLenBits, indexLenBits), // in bits
				calcOutDegree(dataLenBits), // out bits
				NUM_SUBCIRCUITS, // sub circuits because somebody was too lazy
									// to calculate with reflection
				"Single_mux_" + indexLenBits + "_" + dataLenBits);
		this.indexLenBits = indexLenBits;
		this.dataLenBits = dataLenBits;

		// calculate offsets
		int randBitsLen = calcOutDegree(dataLenBits); //dataRandBits + validRandBit
		
		DATA_MASK = BigInteger.ONE.shiftLeft(dataLenBits).subtract(BigInteger.ONE);
		
		IDX0 = 0;
		IDX_TAG0 = IDX0 + indexLenBits;
		DATA0 = IDX_TAG0 + indexLenBits;
		VALID0 = DATA0 + dataLenBits;
		RAND0 = VALID0 + 1;
		IDX1 = RAND0 + randBitsLen; 
		IDX_TAG1 = IDX1 + indexLenBits;
		DATA1 = IDX_TAG1 + indexLenBits;
		VALID1 = DATA1 + dataLenBits;
		RAND1 = VALID1 + 1;

	}

	@Override
	protected void createAllSubCircuits(boolean isForGarbling) {
		unshare1 = new UNSHARE(globals, indexLenBits);

		compareStep1 = new XOR_2L_L(globals, indexLenBits);
		compareStep2 = new OR_L_1(globals, indexLenBits);
		
		xorDataBeforeMux = new XOR_2L_L(globals, dataLenBits);
		chooseXoredDataOrZero = new MUX_2L_L(globals, dataLenBits);

		reShareData = new SHARE(globals, dataLenBits);
		reShareValid = new SHARE(globals, 1);

		xorToCreateZeros = new XOR_2L_L(globals, dataLenBits);
		
		unshareValid = new XOR_2_1(globals);
		andCmp2WithValid = AND_2_1.newInstance(globals, isForGarbling);
		
		negateCompareStep2 = new INVERTER(globals);

		int i = 0;
		subCircuits[i++] = unshare1;
		subCircuits[i++] = compareStep1;
		subCircuits[i++] = compareStep2;
		subCircuits[i++] = xorDataBeforeMux;
		subCircuits[i++] = chooseXoredDataOrZero;
		subCircuits[i++] = reShareData;
		subCircuits[i++] = xorToCreateZeros;
		subCircuits[i++] = unshareValid;
		subCircuits[i++] = andCmp2WithValid;
		subCircuits[i++] = negateCompareStep2;
		subCircuits[i++] = reShareValid;
		
	}

	@Override
	protected void connectWires() {

		unshare1.connectWiresToXY(inputWires, IDX0, inputWires, IDX1);

		// unshare VALID bit
		inputWires[VALID0].connectTo(unshareValid.inputWires, 0);
		inputWires[VALID1].connectTo(unshareValid.inputWires, 1);
		
		// COMPARE idx==index. result is in the compareStep2.outputWires
		// (control of mux)
		for (int i = 0; i < indexLenBits; ++i) {
			unshare1.outputWires[i].connectTo(compareStep1.inputWires,
					compareStep1.X(i));
			unshare1.outputWires[i + indexLenBits].connectTo(
					compareStep1.inputWires, compareStep1.Y(i));
		}

		for (int i = 0; i < indexLenBits; ++i) {
			compareStep1.outputWires[i].connectTo(compareStep2.inputWires, i);
		}
		
		// negate compate 
		compareStep2.outputWires[0].connectTo(negateCompareStep2.inputWires, 0);
		
		// calc the AND of valid and compare
		negateCompareStep2.outputWires[0].connectTo(andCmp2WithValid.inputWires, 0);
		unshareValid.outputWires[0].connectTo(andCmp2WithValid.inputWires, 1);
		
		// COMPUTE XOR of the data
		for (int i = 0; i < dataLenBits; ++i) {
			inputWires[i + DATA0].connectTo(xorDataBeforeMux.inputWires,
					xorDataBeforeMux.X(i));
			inputWires[i + DATA1].connectTo(xorDataBeforeMux.inputWires,
					xorDataBeforeMux.Y(i));

			xorDataBeforeMux.outputWires[i].connectTo(
					xorToCreateZeros.inputWires, xorToCreateZeros.X(i));
			xorDataBeforeMux.outputWires[i].connectTo(
					xorToCreateZeros.inputWires, xorToCreateZeros.Y(i));

		}

		// MUX between zeros or data
		for (int i = 0; i < dataLenBits; i++) {
			xorDataBeforeMux.outputWires[i].connectTo(
					chooseXoredDataOrZero.inputWires, i );
			xorToCreateZeros.outputWires[i].connectTo(
					chooseXoredDataOrZero.inputWires, i + dataLenBits);
		}
		
		// CONTROL BIT
		andCmp2WithValid.outputWires[0].connectTo(chooseXoredDataOrZero.inputWires, dataLenBits * 2);

		// RESHARE data
		reShareData.connectWiresToInputs(inputWires, RAND0, inputWires,
				RAND1, chooseXoredDataOrZero.outputWires, 0);
		
		// RESHARE valid
		reShareValid.connectWiresToInputs(inputWires, RAND0 + dataLenBits, inputWires, RAND1 + dataLenBits, andCmp2WithValid.outputWires, 0);
	}

	@Override
	protected void defineOutputWires() {
		for (int i = 0; i < dataLenBits; ++i) {
			 reShareData.outputWires[i].connectTo(outputWires, i);
		}
		 reShareValid.outputWires[0].connectTo(outputWires, dataLenBits);
	}

	public BigInteger packData(BigInteger idx, BigInteger indexTag, BigInteger data, BigInteger validBit,
			BigInteger r) {
		return r
				.shiftLeft(1).or(validBit)
				.shiftLeft(dataLenBits).or(data)
				.shiftLeft(indexLenBits).or(indexTag)
				.shiftLeft(indexLenBits).or(idx);
	}
	
	public static class Result{
		public BigInteger data;
		public boolean validBit;
		public Result(BigInteger data, boolean validBit){
			this.data = data;
			this.validBit = validBit;
		}
		@Override
		public String toString() {
			return "Data = " + data.toString(16) + " valid = " + validBit;
		}
	}
	
	public int getRandBitsLength(){
		return calcOutDegree(dataLenBits);
	}
	
	public Result parseResult(BigInteger resultBits){
		BigInteger data = resultBits.and(DATA_MASK);
		boolean validBit = (resultBits.shiftRight(dataLenBits).longValue() & 0x1) == 0 ? false : true;
		return new Result(data, validBit);
	}
	
}
