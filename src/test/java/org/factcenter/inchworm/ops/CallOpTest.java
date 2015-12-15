package org.factcenter.inchworm.ops;

import org.factcenter.inchworm.MemoryArea;
import org.factcenter.qilin.util.BitMatrix;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
/**
 * Generic test for the Call Op
 */
abstract public class CallOpTest extends GenericOpTest {

    /**
     * Override and return the specific instance to test.
     * @return
     */
    abstract protected OpAction getOp(VM vm) throws Exception;

    public void testCall(BitMatrix doCallFlag, BitMatrix callAddress, BitMatrix currentIp,
                         BitMatrix currentLocalFrame) throws Exception {
        // Peer shares are zeroes
        final BitMatrix doCallFlag1 = new BitMatrix(1);
        final BitMatrix callAddress1 = new BitMatrix(romPtrSize);
        final BitMatrix currentIp1 = new BitMatrix(wordSize);
        final BitMatrix currentLocalFrame1 = new BitMatrix(wordSize * numLocalRegs);

        MemoryArea[] stacks = { vms[0].state.getMemory(MemoryArea.Type.TYPE_STACK),
                vms[1].state.getMemory(MemoryArea.Type.TYPE_STACK) };

        vms[0].state.setSp(0);
        vms[1].state.setSp(0);

        // Set current frame to a test pattern:

        Future<BitMatrix[]> peerFuture = pool.submit(new Callable<BitMatrix[]>() {
            @Override
            public BitMatrix[] call() throws Exception {
                return vms[1].op.doOp(vms[1].state, doCallFlag1, callAddress1, currentIp1, currentLocalFrame1);
            }
        });

        // Inputs to callop are address,
        BitMatrix[] callOutput = vms[0].op.doOp(vms[0].state, doCallFlag, callAddress, currentIp, currentLocalFrame);

        BitMatrix[] peerOutput = peerFuture.get();

        for (int i = 0; i < callOutput.length; ++i ) {
            callOutput[i].xor(peerOutput[i]);
        }

        BitMatrix newIp =  callOutput[0];
        long newSp = vms[0].state.getSp() ^ vms[1].state.getSp();

        BitMatrix lastStoredFrame = null;
        if (newSp > 0) {
            lastStoredFrame = stacks[0].load((int) newSp - 1, 1);
            lastStoredFrame.xor(stacks[1].load((int) newSp - 1, 1));
        }

        if (doCallFlag.getBit(0) == 1) {
            BitMatrix expectedIp = BitMatrix.valueOf(callAddress.toInteger(romPtrSize), romPtrSize);
            assertEquals("Call should have set IP", expectedIp, newIp);
            // Check stack pointer.
            assertEquals("Stack pointer should have been incremented", 1, newSp);

            assertNotNull(lastStoredFrame);
            BitMatrix expectedStoredFrame = new BitMatrix(stacks[0].getBlockSize());
            expectedStoredFrame.setBits(0, currentLocalFrame);
            expectedStoredFrame.setBits(numLocalRegs * wordSize, currentIp);
            assertEquals("Should have stored current frame", expectedStoredFrame, lastStoredFrame);
        } else {
            assertEquals("Call should not have changed IP", currentIp, newIp);
            assertEquals("Stack pointer should not have been incremented", 0, newSp);
        }
    }

    @Test
    public void test1() throws Exception {
        final BitMatrix doCallFlag = BitMatrix.valueOf(1, 1);
        final BitMatrix callAddress = BitMatrix.valueOf(0xa, romPtrSize);
        final BitMatrix currentIp = BitMatrix.valueOf(0x5, wordSize);
        final BitMatrix currentLocalFrame = BitMatrix.valueOf(0x125534aa56337822L, numLocalRegs * wordSize);

        testCall(doCallFlag, callAddress, currentIp, currentLocalFrame);
    }

}

