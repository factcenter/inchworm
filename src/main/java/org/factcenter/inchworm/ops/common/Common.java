package org.factcenter.inchworm.ops.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by talm on 9/2/14.
 */
public class Common {
    /**
     *  Logger.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    OpInfo info;

    Common(OpInfo info) { this.info = info; }
}
