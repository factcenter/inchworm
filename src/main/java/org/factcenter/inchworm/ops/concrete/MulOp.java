package org.factcenter.inchworm.ops.concrete;

import org.factcenter.fastgc.inchworm.InchwormOpCommon;
import org.factcenter.fastgc.inchworm.MulOpInchworm;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;
import java.util.Random;

public class MulOp extends ConcreteCommon implements OpAction {
	/*-
	 * ----------------------------------------------------------------
	 *                    Constructor(s). 
	 * ----------------------------------------------------------------
	 */
	/**
	 *  Constructs a new {@code MulOp} object.
	 */
	public MulOp() {
   		/*-
		 * NOTE: The op circuit can be instantiated only after all the parameters
		 *       are completely known (see circuit creation in setParameters()).
		 */
        super(new CircuitWrapperFactory() {
            @Override
            public InchwormOpCommon createNewCircuitWrapper(boolean isServer, Random rand, int labelBitLength, int bitWidth, Channel yaoChannel, OTExtender otExtender) {
                return new MulOpInchworm(isServer, rand, labelBitLength, bitWidth, yaoChannel, otExtender);
            }
        });
	}

	/*-
	 * ----------------------------------------------------------------
	 *                 Mul op implementation.
	 * ----------------------------------------------------------------
	 */

    @Override
    public BitMatrix[] doOp(VMState state, BitMatrix... inputs) throws IOException {
        int opWidth = state.getWordSize();

        BitMatrix aShare = inputs[0];
        BitMatrix bShare = inputs[1];
        BitMatrix rValue = new BitMatrix(opWidth * 2);
        rValue.fillRandom(rand);

        BitMatrix productShare;


        try {
            /*-
             * 1) Set shared data into the op and run.
             */

            circuitWrapper.setData(aShare, bShare, rValue);
            circuitWrapper.run();

            // Left (main) player.
            if (getPlayerId() == 0) {

				/*-
				 * 2) Fill the results class.
				 *
				 * 	  Left player:  stores result^rValueLeft
				 *    Right player: stores rValueRight
				 */
                productShare = BitMatrix.valueOf(circuitWrapper.opOutput, opWidth * 2);
                productShare.xor(rValue);
            } else {
                productShare = rValue;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }


        BitMatrix[] outputs = { productShare };

        return outputs;
    }
	
	/*-
	 * ----------------------------------------------------------------
	 *                         Overrides 
	 * ----------------------------------------------------------------
	 */
	

}
