package org.factcenter.inchworm.ops.concrete;

import org.factcenter.inchworm.MemoryArea;
import org.factcenter.inchworm.VMRunner;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;
import java.util.Random;

public class CallOp extends ConcreteCommon implements OpAction {
    AddOp stackPtrAdd;
    MuxOp romPtrMux;

	/*-
	 * ----------------------------------------------------------------
	 *                    Constructor(s). 
	 * ----------------------------------------------------------------
	 */

	/**
	 *  Constructs a new {@code CallOp} object.
	 */
	public CallOp() {
        super(null);

        stackPtrAdd = new AddOp(new VariableWidth() {
            @Override
            public int getWidth() {
                return state.getStackPtrSize();
            }
        });

        romPtrMux = new MuxOp(new VariableWidth() {
            @Override
            public int getWidth() {
                return state.getRomPtrSize();
            }
        });
	}

	/*-
	 * ----------------------------------------------------------------
	 *                 Call op implementation.
	 * ----------------------------------------------------------------
	 */

    @Override
    public BitMatrix[] doOp(VMState state, BitMatrix... inputs) throws IOException {
        int wordSize = state.getWordSize();

        BitMatrix doCallFlag = inputs[0];
        BitMatrix adrShare = inputs[1];
        BitMatrix ipShare = inputs[2];
        BitMatrix frameShare = inputs[3];



        BitMatrix newIpShare;

        MemoryArea stack = state.getMemory(MemoryArea.Type.TYPE_STACK);
        int stackItemSize = stack.getBlockSize();

        assert(doCallFlag.getNumCols() == 1);
        assert(adrShare.getNumCols() == state.getRomPtrSize());
        assert(ipShare.getNumCols() == wordSize);
        assert(frameShare.getNumCols() == state.getFrameSize() * wordSize);

        // Concatenate frame and ip shares into a new BitMatrix.
        BitMatrix itemToPush = new BitMatrix(stackItemSize);
        itemToPush.copyBits(frameShare);
        itemToPush.setBits(state.getFrameSize() * wordSize, wordSize, ipShare);

        BitMatrix spShare = BitMatrix.valueOf(state.getSp(), state.getStackPtrSize());

        // We always store the current frame (whether or not the call is a "nop")
        stack.storeOblivious(spShare, itemToPush);

        // 3) Run sp'  <-  Add(sp, doCallFlag)
        // This increments sp iff doCallFlag==1
        BitMatrix doCallFlagExtended = new BitMatrix(spShare.getNumCols());
        doCallFlagExtended.setBits(0, doCallFlag);
        BitMatrix[] opResults = stackPtrAdd.doOp(state, spShare, doCallFlagExtended);
        spShare = opResults[0];
        state.setSp(spShare.toInteger(state.getStackPtrSize()));

        // 4) ip' = doCallFlag == 0 ? ip : adr
        opResults = romPtrMux.doOp(state, doCallFlag, ipShare.getSubMatrixCols(0, state.getRomPtrSize()), adrShare);

        newIpShare = opResults[0];

        BitMatrix[] outputs = { newIpShare };

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
        romPtrMux.setMoreParameters(otExtender);

    }

	@Override
	public void setParameters(int playerId, final VMState state, VMRunner runner, Random rand) {
		super.setParameters(playerId, state, runner, rand);

        stackPtrAdd.setParameters(playerId, state, runner, rand);
        romPtrMux.setParameters(playerId, state, runner, rand);
    }

	@Override
	public void init() throws IOException, InterruptedException {
		super.init();
        stackPtrAdd.init();
        romPtrMux.init();
	}

}
