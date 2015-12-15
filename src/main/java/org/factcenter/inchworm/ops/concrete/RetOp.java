package org.factcenter.inchworm.ops.concrete;

import org.factcenter.inchworm.MemoryArea;
import org.factcenter.inchworm.VMRunner;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;
import java.util.Random;

public class RetOp extends ConcreteCommon implements OpAction {

    AddOp stackPtrAdd;
    MuxOp frameMux;

	/*-
	 * ----------------------------------------------------------------
	 *                    Constructor(s). 
	 * ----------------------------------------------------------------
	 */
	/**
	 *  Constructs a new {@code RetOp} object.
	 */
	public RetOp() {
        super(null);
        stackPtrAdd = new AddOp(new VariableWidth() {
            @Override
            public int getWidth() {
                return state.getStackPtrSize();
            }
        });

        frameMux = new MuxOp(new VariableWidth() {
            @Override
            public int getWidth() {
                return state.getStackItemSize();
            }
        });
    }

	/*-
	 * ----------------------------------------------------------------
	 *                 Return op implementation.
	 * ----------------------------------------------------------------
	 */
    @Override
    public BitMatrix[] doOp(VMState state, BitMatrix... inputs) throws IOException {

        final int wordSize = state.getWordSize();
        final int stackPtrSize = state.getStackPtrSize();

        BitMatrix doReturnShare = inputs[0];
        BitMatrix ipShare = inputs[1];

        assert(doReturnShare.getNumCols() == 1);  // Expecting a flag
        assert(ipShare.getNumCols() == wordSize); // expecting a register value.

        long spShare = state.getSp();
        MemoryArea stackShare = state.getMemory(MemoryArea.Type.TYPE_STACK);
        BitMatrix frameShare = state.getLocalRegs();

        int frameBitSize = frameShare.getNumCols();
        int ipBitSize = state.getRomPtrSize();


        // We need to compute spShare - 1;
        // For that, we "stretch" the doReturnShare bit to generate either 0 or -1,
        // then use the secure addOp to add this to sp
        int doReturnShareExtended = (int) -doReturnShare.toInteger(1);

        BitMatrix[] newSpParts = stackPtrAdd.doOp(state, BitMatrix.valueOf(spShare, stackPtrSize),
                BitMatrix.valueOf(doReturnShareExtended, stackPtrSize));

        BitMatrix newSp = newSpParts[0];

        // This is either junk (if sp was not decremented) or a share of the previously stored frame
        BitMatrix loadedFrameShare = stackShare.loadOblivious(newSp, 1);

        BitMatrix currentFrameShare = new BitMatrix(frameBitSize + ipBitSize);
        currentFrameShare.setBits(0, frameShare);
        currentFrameShare.setBits(frameBitSize, ipShare);


        // 4) actualframe is either current frame (if doReturn = 0), or loaded Frame if (doReturn = 1).
        BitMatrix[] opResults = frameMux.doOp(state, doReturnShare, currentFrameShare, loadedFrameShare);

        BitMatrix actualFrameShare = opResults[0];

        state.setSp(newSp.toInteger(64));

        BitMatrix[] outputs = {
                actualFrameShare.getSubMatrixCols(frameBitSize, ipBitSize),
                actualFrameShare.getSubMatrixCols(0, frameBitSize)
            };

        return outputs;
    }

    /*-
	 * ----------------------------------------------------------------
	 *                 ConcreteCommon overrides.
	 * ----------------------------------------------------------------
	 */

    @Override
    public void setMoreParameters(OTExtender otExtender) {
        super.setMoreParameters(otExtender);

        stackPtrAdd.setMoreParameters(otExtender);
        frameMux.setMoreParameters(otExtender);
    }

    @Override
    public void setParameters(int playerId, final VMState state, VMRunner runner, Random rand) {
        super.setParameters(playerId, state, runner, rand);

        stackPtrAdd.setParameters(playerId, state, runner, rand);
        frameMux.setParameters(playerId, state, runner, rand);
    }

    @Override
    public void init() throws IOException, InterruptedException {
        super.init();
        stackPtrAdd.init();
        frameMux.init();
    }
}
