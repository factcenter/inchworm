package org.factcenter.inchworm.ops.concrete;

import org.factcenter.fastgc.inchworm.InchwormOpCommon;
import org.factcenter.fastgc.inchworm.MuxOpInchworm;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;
import java.util.Random;

public class MuxOp extends ConcreteCommon implements OpAction {
	/*-
	 * ----------------------------------------------------------------
	 *                    Constructor(s). 
	 * ----------------------------------------------------------------
	 */
    VariableWidth widthOverride;

	/**
	 *  Constructs a new {@code MuxOp} object.
	 */
	public MuxOp() { this(null); }


    /**
     *  Constructs a new {@code MuxOp} object.
     *
     *  widthOverride allows the caller to override the default bit width ({@link VMState#getWordSize()} with
     *  a custom width.
     */
    public MuxOp(final VariableWidth widthOverride) {
		/*-
		 * NOTE: The op circuit can be instantiated only after all the parameters
		 *       are completely known (see circuit creation in setParameters()).
		 */
        super(new CircuitWrapperFactory() {
            @Override
            public InchwormOpCommon createNewCircuitWrapper(boolean isServer, Random rand, int labelBitLength, int bitWidth, Channel yaoChannel, OTExtender otExtender) {
                return new MuxOpInchworm(isServer, rand, labelBitLength,
                        widthOverride == null ? bitWidth : widthOverride.getWidth(), yaoChannel, otExtender);
            }
        });
        this.widthOverride = widthOverride;
    }


	/*-
	 * ----------------------------------------------------------------
	 *                 Mux op implementation.
	 * ----------------------------------------------------------------
	 */

    @Override
    public BitMatrix[] doOp(VMState state, BitMatrix... inputs) throws IOException {
        int opWidth = widthOverride == null ? state.getWordSize() : widthOverride.getWidth();

        BitMatrix aShare = inputs[0];
        BitMatrix bShare = inputs[1];
        BitMatrix cShare = inputs[2];

        assert(bShare.getNumCols() == opWidth);
        assert(cShare.getNumCols() == opWidth);

        if (aShare.getNumCols() != 1)
            aShare = aShare.getSubMatrixCols(0, 1);

        BitMatrix rValue = new BitMatrix(opWidth);
        rValue.fillRandom(rand);


        BitMatrix result;

        try {
            /*-
             * 1) Set shared data into the muxLoad op and run.
             */
            circuitWrapper.setData(rValue, cShare, bShare, aShare);
            circuitWrapper.run();

            // Left (main) player.
            if (getPlayerId() == 0) {
				/*-
				 * 2) Return the result ^ rValue to the left player.
				 */
                result = BitMatrix.valueOf(circuitWrapper.opOutput, opWidth);
                result.xor(rValue);


            } else {
				/*-
				 * 2) Return the random value to the right player.
				 */
                result = rValue;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }


        BitMatrix[] outputs = { result };

        return outputs;
    }
}
