package org.factcenter.fastgc.inchworm;

import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.comm.LocalChannelFactory;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.protocols.generic.DummyOTExtender;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public abstract class AesOpTestCommon {

	final Logger logger = LoggerFactory.getLogger(getClass());

    ExecutorService pool = Executors.newFixedThreadPool(1);

    // labelBitLength.
	final static int k = 80;

	// Insecure, repeatable randomness to help in debugging.
	final static Random rand = new Random(0);

	// Communication + OT extender members.
	OTExtender otExt0, otExt1;
	LocalChannelFactory lcf;
	Channel chan0;
	Channel chan1;

	// AES ops.
	AesOpServer aesServer;
	AesOpClient aesClient;

	// Test only 128 bits key (Nk = 4).
	final static int keyDwords = 4;

	/**
	 * Represents the server side of the tested AES op.
	 */
	class AesOpServer extends AesOpInchworm {
		public AesOpServer(Channel toPeer, OTExtender otExtender) {
			// Instantiate and build the server op.
			super(true, AesOpTestCommon.rand, k, keyDwords, toPeer, otExtender);
		}
	}

	/**
	 * Represents the client side of the tested AES op.
	 */
	class AesOpClient extends AesOpInchworm {
		public AesOpClient(Channel toPeer, OTExtender otExtender) {
			// Instantiate and build the client op.
			super(false, AesOpTestCommon.rand, k, keyDwords, toPeer, otExtender);
		}
	}

	@Before
	public void setup() throws IOException {
		logger.debug("setup()");

		// Init communication and OT extender.
		lcf = new LocalChannelFactory();
		otExt0 = new DummyOTExtender();
		otExt1 = new DummyOTExtender();

		Channel[] clientComm = lcf.getChannelPair("Client");

		chan0 = clientComm[0];
		chan1 = clientComm[1];

		otExt0.setParameters(chan0, rand);
		otExt1.setParameters(chan1, rand);

		otExt0.init();
		otExt1.init();

		// Create the client / server ops.
		aesServer = new AesOpServer(chan0, otExt0);
		aesClient = new AesOpClient(chan1, otExt1);

	}

	/**
	 * Run the client in a separate thread.
	 * 
	 * @throws IOException
	 */
	public Future<BigInteger>  runClient() throws IOException {
		Future<BigInteger> clientThread = pool.submit(new Callable<BigInteger>() {
            @Override
            public BigInteger call() throws Exception {
                logger.debug("client run()");
                aesClient.run();
                return aesClient.opOutput;
            }
        });
		return clientThread;
	}

	/**
	 * Run the server in the main thread.
	 * 
	 * @return the output results.
	 * @throws IOException
	 */
	public BigInteger runServer() throws IOException {

		logger.debug("server run()");
		aesServer.run();

		logger.debug("result={}", aesServer.opOutput);
		return aesServer.opOutput;
	}

}
