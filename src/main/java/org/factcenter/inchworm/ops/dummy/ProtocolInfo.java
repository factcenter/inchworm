package org.factcenter.inchworm.ops.dummy;

import org.factcenter.inchworm.ops.VMProtocolPartyInfo;
import org.factcenter.inchworm.ops.common.OpInfo;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;

/**
 * Abstract class defining the common parameters and initialization of all dummy ops.
 */
public class ProtocolInfo extends VMProtocolPartyInfo implements OpInfo {
	
	/**
	 * Flag controlling whether the sharing of the results between the 
	 * players will be done using a random number or zero (for easy debugging).
	 */
	public final boolean useRandom = false;
	
	/**
	 * The {@link #runner}'s channel.
	 */
	protected Channel toPeer;

	@Override
	public void init() throws IOException, InterruptedException {
		toPeer = runner.getChannel();
	}


	/**
	 * Generates the value for sharing the results. If the useRandom flag is true
	 * a random long value is returned, otherwise zero.
	 */
	public long nextLong(){
		if (useRandom)
			return rand.nextLong();
		else
			return 0;
	}


    public BitMatrix nextRandom(int width) {
        BitMatrix b = new BitMatrix(width);
        if (useRandom)
            b.fillRandom(rand);

        return b;
    }


    @Override
    public Channel getChannel() {
        return toPeer;
    }
}
