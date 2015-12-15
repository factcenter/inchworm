package org.factcenter.inchworm.ops.dummy;

import org.factcenter.inchworm.MemoryArea;
import org.factcenter.inchworm.UnsupportedArgException;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;

public class DummyRet extends Common implements OpAction {

    public DummyRet(ProtocolInfo info) {
        super(info);
    }

    @Override
    public BitMatrix[] doOp(VMState state, BitMatrix... inputs) throws IOException {

        Channel toPeer = info.getChannel();

        final int wordSize = state.getWordSize();
        int doReturnShare = (int) inputs[0].toInteger(1);
        long ipShare = inputs[1].toInteger(state.getRomPtrSize());

        long spShare = state.getSp();
        MemoryArea stackShare = state.getMemory(MemoryArea.Type.TYPE_STACK);
        BitMatrix frameShare = state.getLocalRegs();

        //
        // 1) Get real values of parameters from player's shares.
        //
        toPeer.writeInt(doReturnShare);
        toPeer.writeLong(ipShare);
        toPeer.writeLong(spShare);
        toPeer.flush();

        int doReturnPeer = toPeer.readInt();
        boolean doReturn = (doReturnShare ^ doReturnPeer) != 0;
        long ipPeer = toPeer.readLong();
        long ipReal = ipShare ^ ipPeer;
        long spPeer = toPeer.readLong();
        long spReal = spShare ^ spPeer;

        //
        // 2) 'Pop' the frame from the stack (at position = spReal - 1).
        //
        int spLast = (int) spReal - 1;
        int mask = (1 << state.getStackPtrSize()) - 1;
        spLast &= mask; // We simulate a circular stack
        BitMatrix nextStackFrameShare = stackShare.load(spLast, 1);

        if (doReturn) {
            if (spReal > 0) {
                // Decrement sp.
                spReal--;
            } else {
                // For debugging -- can only happen in dummy implementation.
                throw new UnsupportedArgException("Stack is empty - Can't pop.");
            }

            frameShare = nextStackFrameShare.getSubMatrixCols(0, state.getFrameSize() * wordSize);
            ipShare = nextStackFrameShare.getBits(stackShare.getBlockSize() - state.getRomPtrSize(), state.getRomPtrSize());
        } else {
            frameShare = frameShare.clone(); // Make sure we don't step on existing matrix.
        }

        BitMatrix frameRandom;
        long ipRandom;

        // Rerandomize shares
        if (info.getPlayerId() == 0) {
            logger.debug("DummyRet({}) - ipReal = {} spReal = {}", doReturn ? "REAL" : "NOP", ipReal,
                    spReal);


            ipRandom = info.nextLong();
            long spRandom = info.nextLong();
            frameRandom = info.nextRandom(frameShare.getNumCols());

//            toPeer.writeLong(ipRandom);
//            toPeer.writeLong(spRandom);
//            toPeer.writeObject(frameRandom);
            toPeer.flush();

            spShare = spReal ^ spRandom;
        } else {
//            ipRandom = toPeer.readLong();
        	ipRandom = 0;
//            spShare = toPeer.readLong();
        	spShare = 0;
//            frameRandom = toPeer.readObject(BitMatrix.class);
        	frameRandom = frameShare;
        	
        }

        ipShare ^= ipRandom;
        frameShare.xor(frameRandom);


        // SP isn't a regular register, so it can't be an output.
        state.setSp(spShare);

        BitMatrix[] outputs = { BitMatrix.valueOf(ipShare, state.getRomPtrSize()), frameShare };
        // No output.
        return outputs;
    }
}
