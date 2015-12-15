package org.factcenter.inchworm.ops.dummy;

import org.factcenter.inchworm.MemoryArea;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;

public class DummyCall extends Common implements OpAction {

    public DummyCall(ProtocolInfo info) {
        super(info);
    }

    @Override
    public BitMatrix[] doOp(VMState state, BitMatrix... inputs) throws IOException {

        Channel toPeer = getChannel();

        int doCallShare = (int) inputs[0].toInteger(1);
        long adrShare = inputs[1].toInteger(state.getRomPtrSize());
        long ipShare = inputs[2].toInteger(state.getRomPtrSize());
        BitMatrix frameShare = inputs[3];

        long spShare = state.getSp();
        MemoryArea stackShare = state.getMemory(MemoryArea.Type.TYPE_STACK);

        //
        // 1) Get real values of parameters from player's shares.
        //
        toPeer.writeInt(doCallShare);
        toPeer.writeLong(adrShare);
        toPeer.writeLong(ipShare);
        toPeer.writeLong(spShare);
        toPeer.flush();

        int peerDoCallShare = toPeer.readInt();
        boolean isNop = (doCallShare ^ peerDoCallShare) == 0;

        long adrOther = toPeer.readLong();
        long adrReal = adrShare ^ adrOther;

        long ipOther = toPeer.readLong();
        long ipReal = ipShare ^ ipOther;

        long spOther = toPeer.readLong();
        long spReal = spShare ^ spOther;


        int wordSize = state.getWordSize();
        //
        // 2) Store the frame in the stack  (at position = spReal, but we use the "oblivious" storing strategy since
        //      this is what a  secure  implementation must do).
        //
        if (spReal == state.getStackSize()) {
            // Can only happen in dummy implementation (useful to catch bugs)
            throw new UnsupportedOperationException("Stack is full -  Can't push.");
        }

        BitMatrix itemToPush = new BitMatrix(stackShare.getBlockSize());
        itemToPush.copyBits(frameShare);
        itemToPush.setBits(state.getFrameSize() * wordSize, state.getRomPtrSize(), ipShare);

        // We always set stack[sp] to the new frame, but we advance the stack pointer
        // only if the high bit of the call address is set.
        stackShare.storeOblivious(BitMatrix.valueOf(spShare, state.getStackPtrSize()), itemToPush);


        //
        // 5) Update sp and ip.
        //
        if (!isNop) {
            // Increment sp.
            spReal++;
            // Set returned ip to adrShareLowWord
            ipReal = adrReal;
        }

        long peerShareIp;
        long peerShareSp;

        // Reshare the new SP and IP
        if (getPlayerId() == 0) {
            logger.debug("DummyCall({}) - adrReal={}, ipReal = {}, spReal = {}", isNop ? "NOP" : "REAL", adrReal,
                    ipReal, spReal);

            peerShareIp = info.nextLong();
            peerShareSp = info.nextLong();

//            toPeer.writeLong(peerShareIp);
//            toPeer.writeLong(peerShareSp);
            toPeer.flush();

            ipShare = ipReal ^ peerShareIp;
            spShare = spReal ^ peerShareSp;
        } else {
//            ipShare = toPeer.readLong();
//            spShare = toPeer.readLong();
        }

        // Store SP (it's not a regular register, so can't be the return value)
        state.setSp(spShare);

        BitMatrix[] outputs = { BitMatrix.valueOf(ipShare, state.getRomPtrSize()) };
        // No output.
        return outputs;
    }
}
