package org.factcenter.inchworm.ops.concrete;

import org.factcenter.fastgc.inchworm.AddOpInchworm;
import org.factcenter.fastgc.inchworm.InchwormOpCommon;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;

public class AddOp extends ConcreteCommon implements OpAction {
	/*-
	 * ----------------------------------------------------------------
	 *                    Constructor(s). 
	 * ----------------------------------------------------------------
	 */
    VariableWidth widthOverride;


    public AddOp() { this(null);  }

	/**
	 *  Constructs a new {@code AddOp} object.
     *
     *  widthOverride allows the caller to override the default bit width ({@link VMState#getWordSize()} with
     *  a custom width.
	 */
	public AddOp(final VariableWidth widthOverride) {
		/*-
		 * NOTE: The op circuit can be instantiated only after all the parameters
		 *       are completely known (see circuit creation in setParameters()).
		 */
        super(new CircuitWrapperFactory() {
            @Override
            public InchwormOpCommon createNewCircuitWrapper(boolean isServer, Random rand, int labelBitLength, int bitWidth, Channel yaoChannel, OTExtender otExtender) {
                return new AddOpInchworm(isServer, rand, labelBitLength,
                        widthOverride == null ? bitWidth : widthOverride.getWidth(), yaoChannel, otExtender);
            }
        });
        this.widthOverride = widthOverride;
	}

	/*-
	 * ----------------------------------------------------------------
	 *                 Add op implementation.
	 * ----------------------------------------------------------------
	 */


    @Override
    public BitMatrix[] doOp(VMState state, BitMatrix... inputs) throws IOException {
        int opWidth = widthOverride == null ? state.getWordSize() : widthOverride.getWidth();

        BitMatrix aShare = inputs[0];
        BitMatrix bShare = inputs[1];

        assert(aShare.getNumCols() == opWidth);
        assert(bShare.getNumCols() == opWidth);

        BitMatrix rValue = new BitMatrix(opWidth + 4);
        rValue.fillRandom(rand);


        BitMatrix sumShare;
        BitMatrix carryShare;
        BitMatrix zeroShare;
        BitMatrix signShare;
        BitMatrix overflowShare;
        BitMatrix flagsShare;
        

        try {
            /*-
             * 1) Set shared data into the add op and run.
             */
            circuitWrapper.setData(aShare, bShare, rValue);
            circuitWrapper.run();

            BitMatrix combinedResult;

            /*-
             * 2) Fill the results class.
             *
             * 	  Left player:  stores result^rValueLeft
             *    Right player: stores rValueRight
             */
            if (getPlayerId() == 0) {
                // Left (main) player.
                combinedResult = BitMatrix.valueOf(circuitWrapper.opOutput, opWidth + 4);
                combinedResult.xor(rValue);
            } else {
                combinedResult = rValue;
            }


            sumShare = combinedResult.getSubMatrixCols(0, opWidth);
            carryShare = combinedResult.getSubMatrixCols(opWidth, 1);
            zeroShare = combinedResult.getSubMatrixCols(opWidth + 1, 1);
            signShare = combinedResult.getSubMatrixCols(opWidth + 2, 1);
            overflowShare = combinedResult.getSubMatrixCols(opWidth + 3, 1);
            BigInteger flagsBigintShare = carryShare.toBigInteger(1);
            flagsBigintShare = flagsBigintShare.or(zeroShare.toBigInteger(1).shiftLeft(1));
            flagsBigintShare = flagsBigintShare.or(signShare.toBigInteger(1).shiftLeft(2));
            flagsBigintShare = flagsBigintShare.or(overflowShare.toBigInteger(1).shiftLeft(3));
            flagsShare = BitMatrix.valueOf(flagsBigintShare, 4);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        BitMatrix[] outputs = { sumShare, carryShare, zeroShare, signShare, overflowShare, flagsShare };

        return outputs;
    }
}
