package org.factcenter.inchworm.ops.concrete;

import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;

/**
 * A Generic op implementation for Ops that have two word-size inputs and one output.
 */
public class GenericOp extends ConcreteCommon implements OpAction {
    int outWidth;

    public GenericOp(int outWidth, CircuitWrapperFactory wrapperFactory) {
        super(wrapperFactory);
        this.outWidth = outWidth;
    }


    public GenericOp(CircuitWrapperFactory wrapperFactory) {
        this(1, wrapperFactory);
    }

    /*-
	 * ----------------------------------------------------------------
	 *                 Generic op implementation.
	 * ----------------------------------------------------------------
	 */
    @Override
    public BitMatrix[] doOp(VMState state, BitMatrix... inputs) throws IOException {
        int opWidth = state.getWordSize();

        BitMatrix[] sharesWithRandom = new BitMatrix[inputs.length + 1];

        for (int i = 0; i < inputs.length; ++i) {
            assert(inputs[i].getNumCols() == opWidth);
            sharesWithRandom[i] = inputs[i];
        }

        BitMatrix rValue = new BitMatrix(outWidth * opWidth);
        rValue.fillRandom(rand);

        sharesWithRandom[inputs.length] = rValue;

        BitMatrix outShare;


        try {
            /*-
             * 1) Set shared data into the and op and run.
             */
            circuitWrapper.setData(sharesWithRandom);
            circuitWrapper.run();


            // Left (main) player.
            if (getPlayerId() == 0) {

				/*-
				/*-
				 * 2) Return the result ^ rValue to the left player.
				 */
                outShare = BitMatrix.valueOf(circuitWrapper.opOutput, outWidth * opWidth);
                outShare.xor(rValue);

            } else {

				/*-
				 * 2) Return the random value to the right player.
				 */
                outShare = rValue;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        BitMatrix[] outputs = { outShare };

        return outputs;
    }
}
