package org.factcenter.inchworm.ops;

import org.factcenter.inchworm.MemoryArea;
import org.factcenter.qilin.util.BitMatrix;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;


/**
 * Generic test for the Call Op
 */
abstract public class RetOpTest extends GenericOpTest {

    /**
     * Override and return the specific instance to test.
     * @return
     */
    abstract protected OpAction getOp(VM vm) throws Exception;

    public void testReturn(final BitMatrix doReturnFlag, BitMatrix currentIp,
                           int origSp, BitMatrix storedStackFrame, BitMatrix currentLocalFrame) throws Exception {
        // Peer shares are zeroes
        final BitMatrix doReturnFlag1 = new BitMatrix(1);
        final BitMatrix currentIp1 = new BitMatrix(wordSize);

        MemoryArea[] stacks = { vms[0].state.getMemory(MemoryArea.Type.TYPE_STACK),
                vms[1].state.getMemory(MemoryArea.Type.TYPE_STACK) };

        int localFrameSize = numLocalRegs * wordSize;
        int stackItemSize = stacks[0].getBlockSize();

        vms[0].state.setSp(origSp);
        vms[1].state.setSp(0); // Second share is 0

        // Set current frame to a test pattern:
        vms[0].state.setLocalRegs(currentLocalFrame);
        vms[1].state.setLocalRegs(BitMatrix.valueOf(0, localFrameSize));

        stacks[0].store(origSp - 1, storedStackFrame);
        stacks[1].store(origSp - 1, BitMatrix.valueOf(0, stackItemSize));

        Future<BitMatrix[]> peerFuture = pool.submit(new Callable<BitMatrix[]>() {
            @Override
            public BitMatrix[] call() throws Exception {
                return vms[1].op.doOp(vms[1].state, doReturnFlag1, currentIp1);
            }
        });

        // Inputs to callop are address,
        BitMatrix[] callOutput = vms[0].op.doOp(vms[0].state, doReturnFlag, currentIp);

        BitMatrix[] peerOutput = peerFuture.get();

        for (int i = 0; i < callOutput.length; ++i ) {
            callOutput[i].xor(peerOutput[i]);
        }

        BitMatrix newIp = callOutput[0];
        BitMatrix newLocalFrame = callOutput[1];
        long newSp = vms[0].state.getSp() ^ vms[1].state.getSp();


        BitMatrix storedLocalFrame = storedStackFrame.getSubMatrixCols(0, localFrameSize);
        BitMatrix retAddr = storedStackFrame.getSubMatrixCols(localFrameSize, romPtrSize);


        if (doReturnFlag.getBit(0) == 1) {
            BitMatrix expectedIp = retAddr;
            assertEquals("Call should have set IP", expectedIp, newIp);
            // Check stack pointer.
            assertEquals("Stack pointer should have been decremented", origSp - 1, newSp);

            assertEquals("Should have restored current frame", storedLocalFrame, newLocalFrame);
        } else {
            assertEquals("Call should not have changed IP", currentIp.getSubMatrixCols(0, romPtrSize), newIp);
            assertEquals("Stack pointer should not have been decremented", origSp, newSp);

            assertEquals("Current frame should not have been changed", currentLocalFrame, newLocalFrame);
        }
    }

    @Test
    public void test1() throws Exception {
        int localFrameSize = numLocalRegs * wordSize;

        final BitMatrix doReturnFlag = BitMatrix.valueOf(1, 1);
        final BitMatrix retAddress = BitMatrix.valueOf(0xa, romPtrSize);
        final BitMatrix storedLocalFrame = BitMatrix.valueOf(0xfe99dc88ba779866L, localFrameSize);

        BitMatrix storedStackFrame = new BitMatrix(vms[0].state.getMemory(MemoryArea.Type.TYPE_STACK).getBlockSize());
        storedStackFrame.setBits(0, storedLocalFrame);
        storedStackFrame.setBits(localFrameSize, retAddress);

        final BitMatrix currentLocalFrame = BitMatrix.valueOf(0x125534aa56337822L, localFrameSize);

        final BitMatrix currentIp = BitMatrix.valueOf(0x5, wordSize);

        testReturn(doReturnFlag, currentIp, 3, storedStackFrame, currentLocalFrame);
    }

}

