package org.factcenter.inchworm.ops.common;

import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;

public class Out extends Common implements OpAction {
    public Out(OpInfo info) {
        super(info);
    }
	
	/*-
	 *  L sends %out1L to R;                        R sends %out2R to L;        
	 *  L reconstructs %out2 = %out2L xor %out2R    R reconstructs %out1 = %out1L xor %out1R
	 *  
	 */

    @Override
    public BitMatrix[] doOp(VMState state, BitMatrix... inputs) throws IOException {

        Channel toPeer = info.getChannel();

        final int wordSize = state.getWordSize();

        // We only care about the least significant byte of the control register.
        int ctrlShare = (int) inputs[1].toInteger(8);

        toPeer.writeByte(ctrlShare);
        toPeer.flush();

        int ctrl = (toPeer.readByte() ^ ctrlShare) & 0xff;


        // Both parties have now recovered the control share.
        if (ctrl == 0)
            // Do nothing if control is 0
            return null;

        // Note that OUT1 and OUT2 appear reversed because the offsets are from the *end* of the
        // register file.
        BitMatrix outRegShares[] =  {
                inputs[0].getSubMatrixCols(wordSize, wordSize),    // OUT1
                inputs[0].getSubMatrixCols(0, wordSize)            // OUT2
        };


        BitMatrix result;

        toPeer.writeObject(outRegShares[info.getPlayerId()]);
        toPeer.flush();

        result = toPeer.readObject(BitMatrix.class);
        result.xor(outRegShares[1- info.getPlayerId()]);

        info.getRunner().outputToPlayer(ctrl, result);

        return null;
    }
}
