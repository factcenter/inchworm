package org.factcenter.inchworm.ops.concrete;

import org.factcenter.fastgc.inchworm.InchwormOpCommon;
import org.factcenter.inchworm.VMRunner;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.VMProtocolPartyInfo;
import org.factcenter.inchworm.ops.common.OpInfo;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;

import java.io.IOException;
import java.util.Random;

/**
 * Abstract class defining the common parameters and initialization of all concrete ops.
 */
public class ConcreteCommon extends VMProtocolPartyInfo implements OpInfo {
    /**
     * Label bit-length. (= OT Extender default security parameter)
     */
    final static int labelBitLength = 80;

    protected interface CircuitWrapperFactory {
        public InchwormOpCommon createNewCircuitWrapper(boolean isServer, Random rand,
                                                        int labelBitLength, int bitWidth, Channel yaoChannel,
                                                        OTExtender otExtender);
    }

    protected ConcreteCommon(CircuitWrapperFactory wrapperFactory) {
        this.circuitWrapperFactory = wrapperFactory;
    }

	/**
	 * The OT Extender used by the concrete ops.
	 */
	protected OTExtender otExtender;

    InchwormOpCommon circuitWrapper;

    CircuitWrapperFactory circuitWrapperFactory;



    /**
	 * The {@link #runner}'s channel.
	 */
    @Override
	public Channel getChannel() { return getRunner().getChannel(); }

	/**
	 * Sets the OT extension for the concrete op.
	 * @param otExtender
	 */
	public void setMoreParameters(OTExtender otExtender) {
        this.otExtender = otExtender;
	}

    @Override
    public void setParameters(int playerId, VMState state, VMRunner runner, Random rand) {
        if (getPlayerId() == playerId && this.state == state && this.runner == runner && this.rand == rand) {
            // The parameters have already been set
            return;
        } else {
            circuitWrapper = null;
        }
        super.setParameters(playerId, state, runner, rand);
    }

    @Override
    public void init() throws IOException, InterruptedException {
        otExtender.init();

        if (circuitWrapperFactory != null && circuitWrapper == null) {
            // Now we can create the circuit wrapper circuit. (at this point the memory factory and
            // pointer sizes should have been initialized).
            int bitWidth = state.getWordSize();
            circuitWrapper = circuitWrapperFactory.createNewCircuitWrapper(getPlayerId() == 0, rand, labelBitLength, bitWidth,
                    runner.getYaoChannel(), otExtender);
        }

        if (circuitWrapper != null)
            circuitWrapper.init();
    }
}
