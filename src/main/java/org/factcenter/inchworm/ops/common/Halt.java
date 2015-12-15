package org.factcenter.inchworm.ops.common;

import org.factcenter.inchworm.Constants;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;

public class Halt extends Common implements OpAction {
    public Halt(OpInfo info) { super(info); }

	/*-
	 *  The implementation of this command should output the contents of the halt register
	 *	to both parties.
	 *  L sends the left share of %halt to R, and R sends the right share of %halt to left.
	 *  If the result is non-zero, both parties should halt. 	
	 *  
	 */

    @Override
    public BitMatrix[] doOp(VMState state, BitMatrix... inputs) throws IOException {
        Channel toPeer = info.getChannel();

        long resultMine;
        long resultOther;
        long haltRegValue;

        // Left (main) player.
        if (info.getPlayerId() == 0) {
			/*-
			 * 1) Get the value from players shares.
			 */
            resultMine = state.getNamedReg(Constants.NamedReg.R_CTRL).toInteger(64);
            toPeer.writeLong(resultMine);
            toPeer.flush();
            resultOther = toPeer.readLong();
        } else {
			/*-
			 * 1) Get the value from players shares.
			 */
            resultMine = state.getNamedReg(Constants.NamedReg.R_CTRL).toInteger(64);
            toPeer.writeLong(resultMine);
            toPeer.flush();
            resultOther = toPeer.readLong();
        }

        haltRegValue = resultMine ^ resultOther;
        // Test (and report) ending condition.
        logger.trace("Halt - Register Value = {}", haltRegValue);

        int wordSize = state.getWordSize();

        // Check high bit to see if halt was requested.
        if ((haltRegValue & (1 << (wordSize - 1))) != 0) {
            logger.debug("Computation requested halt");
            state.setHalt(true);
        }
        return null; // No output
    }
}
