package org.factcenter.inchworm.ops.concrete;

import org.factcenter.fastgc.inchworm.DivOpInchworm;
import org.factcenter.fastgc.inchworm.InchwormOpCommon;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;
import java.util.Random;

public class DivOp extends GenericOp implements OpAction {

	/*-
	 * ----------------------------------------------------------------
	 *                    Constructor(s). 
	 * ----------------------------------------------------------------
	 */
	/**
	 *  Constructs a new {@code DivOp} object.
	 */
	public DivOp() {
        super(2, new CircuitWrapperFactory() {
            @Override
            public InchwormOpCommon createNewCircuitWrapper(boolean isServer, Random rand, int labelBitLength, int bitWidth, Channel yaoChannel, OTExtender otExtender) {
                return new DivOpInchworm(isServer, rand, labelBitLength, bitWidth, yaoChannel, otExtender);
            }
        });
	}


    /**
     * We override the generic version because div outputs an extra bit to player 0
     * indicating division by zero.
     */
    @Override
    public BitMatrix[] doOp(VMState state, BitMatrix... inputs) throws IOException {
        BitMatrix[] outputs = super.doOp(state, inputs);

        boolean divByZero;
        if (getPlayerId() == 0) {
            logger.debug("Div output was 0x{}", circuitWrapper.opOutput.toString(16));
            divByZero = circuitWrapper.opOutput.testBit(state.getWordSize() * 2);
            getChannel().writeBoolean(divByZero);
            getChannel().flush();
        } else {
            divByZero = getChannel().readBoolean();
        }
        if (divByZero) {
            BitMatrix[] zeroOutputs = { BitMatrix.valueOf(0, state.getWordSize() * 2)};
            outputs = zeroOutputs;
        }

        return outputs;
    }

//	/*-
//	 * ----------------------------------------------------------------
//	 *                 Div op implementation.
//	 * ----------------------------------------------------------------
//	 */
//
//
//    @Override
//    public BitMatrix[] doOp(VMState state, BitMatrix... inputs) throws IOException {
//        int opWidth = state.getWordSize();
//        Channel toPeer = getChannel();
//
//        BitMatrix aShare = inputs[0];
//        BitMatrix bShare = inputs[1];
//        BitMatrix rValue = new BitMatrix(opWidth * 2);
//        rValue.fillRandom(rand);
//
//
//
//        BitMatrix quotientShare, remainderShare;
//
//        try {
//            /*-
//             * 1) Set shared data into the op and run.
//             */
//            circuitWrapper.setData(aShare, bShare, rValue);
//            //divOp.setData(rValue, bShare, aShare);
//            circuitWrapper.run();
//
//            BitMatrix combinedResult;
//            boolean divideByZero;
//
//
//            // Left (main) player.
//            if (getPlayerId() == 0) {
//
//                BitMatrix resultWithDivByZero = BitMatrix.valueOf(circuitWrapper.opOutput, 2 * opWidth + 1);
//                BitMatrix divideByZeroVal = resultWithDivByZero.getSubMatrixCols(2 * opWidth, 1);
//                divideByZero = divideByZeroVal.getBit(0) == 1;
//                toPeer.writeBoolean(divideByZero);
//                toPeer.flush();
//                combinedResult = resultWithDivByZero.getSubMatrixCols(0, opWidth * 2);
//
//                combinedResult.xor(rValue);
//            } else {
//                combinedResult = rValue;
//                divideByZero = toPeer.readBoolean();
//            }
//
//            /*-
//             * 2) Check the zero-divisor bit and fill the results
//             *
//             * 	  Left player:  stores result^rValueLeft
//             *    Right player: stores rValueRight
//             */
//
//            if (divideByZero) {
//                 // The divide circuit explicitly outputs if there was a divide-by-zero.
//                 // TODO: Replace this with an oblivious "output zero" strategy.
//
//                 combinedResult.reset();
//            }
//            quotientShare = combinedResult.getSubMatrixCols(0, opWidth);
//            remainderShare = combinedResult.getSubMatrixCols(opWidth, opWidth);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//
//
//        BitMatrix[] outputs = { quotientShare, remainderShare };
//
//        return outputs;
//    }
}
