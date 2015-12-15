package org.factcenter.inchworm.ops.dummy;

import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.OpAction;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;
import java.math.BigInteger;

/**
 * A base class for dummy arithmetic operations.
 */
abstract public class GenericArithmeticAction extends Common implements OpAction {

    public GenericArithmeticAction(ProtocolInfo info) {
        super(info);
    }


    /**
     * Override if results have non-standard width  (return value is in bits)
     */
    protected int getResultWidth(int wordSize, int resultIndex) { return wordSize; }

    abstract protected BigInteger[] doArithmetic(int wordSize, BigInteger... inputs);

    /**
     * Party 0 will do the computation, party 1 just sends it current shares and receives new ones.
     * @param state
     * @param inputs
     * @return
     */
    @Override
    public BitMatrix[] doOp(VMState state, BitMatrix... inputs) throws IOException {

        Channel toPeer = getChannel();
        int wordSize = state.getWordSize();

        BigInteger[] unsharedInputs = new BigInteger[inputs.length];


        if (getPlayerId() == 0) {
            BitMatrix[] peerShares = toPeer.readObject(BitMatrix[].class);
            assert(peerShares.length == inputs.length);

            for (int i = 0; i < inputs.length; ++i) {
                peerShares[i].xor(inputs[i]);
                unsharedInputs[i] = peerShares[i].toBigInteger();
            }
        } else {
            toPeer.writeObject(inputs);
            toPeer.flush();
        }

        BitMatrix outputs[];
        if (getPlayerId() == 0) {
            BigInteger[] results = doArithmetic(wordSize, unsharedInputs);

            outputs = new BitMatrix[results.length];
            BitMatrix[] peerShares = new BitMatrix[results.length];

            for (int i = 0; i < results.length; ++i) {
                outputs[i] = BitMatrix.valueOf(results[i], getResultWidth(wordSize, i));
                peerShares[i] = info.nextRandom(outputs[i].getNumCols());

                outputs[i].xor(peerShares[i]);
            }

            toPeer.writeObject(peerShares);
            toPeer.flush();
        } else {
            outputs = toPeer.readObject(BitMatrix[].class);
        }

        return outputs;
    }
}
