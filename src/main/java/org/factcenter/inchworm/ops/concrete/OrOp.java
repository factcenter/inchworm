package org.factcenter.inchworm.ops.concrete;

import org.factcenter.fastgc.inchworm.InchwormOpCommon;
import org.factcenter.fastgc.inchworm.OrOpInchworm;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;

import java.util.Random;

public class OrOp extends GenericOp {
	/*-
	 * ----------------------------------------------------------------
	 *                    Constructor(s). 
	 * ----------------------------------------------------------------
	 */
	/**
	 *  Constructs a new {@code OrOp} object.
	 */
	public OrOp() {
		/*-
		 * NOTE: The op circuit can be instantiated only after all the parameters
		 *       are completely known (see circuit creation in setParameters()).
		 */
        super(new CircuitWrapperFactory() {
            @Override
            public InchwormOpCommon createNewCircuitWrapper(boolean isServer, Random rand, int labelBitLength, int bitWidth, Channel yaoChannel, OTExtender otExtender) {
                return new OrOpInchworm(isServer, rand, labelBitLength, bitWidth, yaoChannel, otExtender);
            }
        });
	}
}
