package org.factcenter.inchworm.ops.concrete;

import org.factcenter.fastgc.inchworm.AesOpInchworm;
import org.factcenter.inchworm.VMRunner;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;
import java.util.Random;
                                          
public class AesOp extends ConcreteCommon implements OpAction {

	AesOpInchworm aesOp;

	/**
	 * Number of 32-bit words comprising the Cipher Key (Nk = 4, 6, or 8).
	 */
	private int Nk;

	/*-
	 * ----------------------------------------------------------------
	 *                    Constructor(s). 
	 * ----------------------------------------------------------------
	 */

	/**
	 *  Constructs a new {@code AesOp} object.
     *  @param keyDwords determines the keylength:
     *  4 -- AES-128
     *  6 -- AES-192
     *  8 -- AES-256
	 */
	public AesOp(final int keyDwords) {
        super(null);
		/*-
		 * NOTE: The op can be instantiated only after all the parameters
		 *       are completely known (see circuit creation in setParameters()).
		 */
		Nk = keyDwords;
	}

	/*-
	 * ----------------------------------------------------------------
	 *                 AES op implementation.
	 * ----------------------------------------------------------------
	 */

    @Override
    public BitMatrix[] doOp(VMState state, BitMatrix... inputs) throws IOException {

        // Expects to receive data

        Channel toPeer = getChannel();

        BitMatrix result;

        if (getPlayerId() == 0) {
            // The server has the key.
            BitMatrix key = inputs[0].getSubMatrixCols(0, Nk * 32);
            aesOp.setData(key);
            aesOp.run();
            result = BitMatrix.valueOf(aesOp.opOutput, 128);

            // Both parties should get the encrypted output, but only the server actually
            // receives it from the garbled circuit
            toPeer.writeObject(result);
            toPeer.flush();
        } else {
            // Client has the message (always 128 bit)
            BitMatrix msg = inputs[0].getSubMatrixCols(0, 128);
            aesOp.setData(msg);
            aesOp.run();
            result = toPeer.readObject(BitMatrix.class);
        }

        BitMatrix[] outputs = { result };


        return outputs;
    }

	/*-
	 * ----------------------------------------------------------------
	 *                         Overrides 
	 * ----------------------------------------------------------------
	 */
	@Override
	public void setParameters(int playerId, VMState state, VMRunner runner, Random rand) {

		super.setParameters(playerId, state, runner, rand);

        // Now we can create the AES Op.
        aesOp = new AesOpInchworm(playerId == 0, rand, labelBitLength, Nk, runner.getYaoChannel(), otExtender);
	}


    @Override
    public void init() throws IOException, InterruptedException {
        super.init();
        aesOp.init();
    }

}
