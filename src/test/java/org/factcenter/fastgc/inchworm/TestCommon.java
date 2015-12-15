package org.factcenter.fastgc.inchworm;

import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.comm.TCPChannelFactory;
import org.factcenter.qilin.protocols.concrete.DefaultOTExtender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Random;

/**
 * Abstract class for testing op circuits, running in separate client / server
 * executables.
 */
public abstract class TestCommon {

	/* ======= OT Extender parameters ======== */

	final static int k = 80;
	final static int m = 400;
	final static int highWaterMark = 400;
	final static int lowWaterMark = 1;

	/* ====== Common parameters ============ */

	protected abstract String getLoggerName();

	Logger logger = LoggerFactory.getLogger(getLoggerName());

	int partyId;

	DefaultOTExtender otExt;

	Channel toPeer;
	Channel toOTPeer;

	Random rand;

	TCPChannelFactory tcf;

	Thread tcfThread;

	TestCommon(int partyId, int localport) throws IOException {
		this.partyId = partyId;
		rand = new Random(0); // We want insecure, repeatable randomness to help
								// in debugging.

		otExt = new DefaultOTExtender(k, m, lowWaterMark, highWaterMark, partyId);

		tcf = new TCPChannelFactory(localport);
		logger.info("Starting TCP Server on {}", tcf.getLocalname());

		tcfThread = new Thread(tcf, "TCPChannelFactory");
		tcfThread.setDaemon(true);
		tcfThread.start();
	}

	/**
	 * Setup communication parameters. If peer is null, waits for a connection.
	 */
	protected void setupComm(String peer) throws IOException {
		if (peer != null) {
			logger.debug("Connecting (1) to {}", peer);
			toPeer = tcf.getChannel(peer);
			logger.debug("Connecting (2) to {}", peer);
			toOTPeer = tcf.getChannel(peer);
		} else {
			logger.debug("Waiting for connection (1) from peer");
			toPeer = tcf.getChannel();
			logger.debug("Waiting for connection (2) from peer");
			toOTPeer = tcf.getChannel();

		}
		logger.debug("Connected");
		otExt.setParameters(toPeer, rand);
		otExt.setServerParameters(toOTPeer, rand);
	}

	/**
	 * Initialize OT Extender
	 * 
	 * @throws IOException
	 */
	protected void init() throws IOException {
		otExt.init();
	}
}
