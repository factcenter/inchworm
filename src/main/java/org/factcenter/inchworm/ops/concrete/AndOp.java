package org.factcenter.inchworm.ops.concrete;

import org.factcenter.fastgc.inchworm.AndOpInchworm;
import org.factcenter.fastgc.inchworm.InchwormOpCommon;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;

import java.util.Random;

public class AndOp extends GenericOp {
	public AndOp() {
        super(new CircuitWrapperFactory() {
            @Override
            public InchwormOpCommon createNewCircuitWrapper(boolean isServer, Random rand, int labelBitLength, int bitWidth, Channel yaoChannel, OTExtender otExtender) {
                return new AndOpInchworm(isServer, rand, labelBitLength, bitWidth, yaoChannel, otExtender);
            }
        });
	}
}
