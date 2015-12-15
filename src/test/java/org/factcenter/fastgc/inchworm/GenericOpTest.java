package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.Wire;
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

/**
 * Abstract class for testing op circuits (client / server implementation).
 */
public abstract class GenericOpTest {

	final Logger logger = LoggerFactory.getLogger(getClass());

    ExecutorService pool = Executors.newFixedThreadPool(1);

	/* ======= OT Extender parameters ======== */

	final static int k = 80;
	final static int m = 160;
	final static int highWaterMark = 80;
	final static int lowWaterMark = 1;

	/* ====== Common parameters ============ */

	OTExtender otExt0, otExt1;

	LocalChannelFactory lcf;
	Channel chan0;
	Channel chan1;

	// Insecure, repeatable randomness to help in debugging.
	final static Random rand = new Random(0);

	GenericOpServer server;
	GenericOpClient client;

	/**
	 * Return a new instance of the circuit to be tested.
	 */
	abstract Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode);


    /**
     * Override this to support dynamically constructed circuits (that depend on the paramter passed to runClient/runServer)
     */
    Circuit getTestCircuit(CircuitGlobals globals, boolean serverMode, Object param) {
        return getTestCircuit(globals, serverMode);
    }

	/**
	 * Return the number of client / server input bits.
	 */
	abstract int getNumberOfInputs();

    /**
     * Override this to support dynamically constructed circuits (that depend on the paramter passed to runClient/runServer)
     */
    int getNumberOfInputs(Object param) { return getNumberOfInputs(); }


    class GenericOpServer extends InchwormOpCommon {
		public GenericOpServer(Object param) {
			super(GenericOpTest.rand, k);
			// Instantiate and build the circuit.
			serverMode = true;
			ccs = new Circuit[1];
			ccs[0] = getTestCircuit(globals, true, param);
		    ccs[0].build(serverMode);
			
			otNumOfPairs = getNumberOfInputs(param);
			generateLabelPairs();
			logger.debug("Server conjugation value (R << 1 | 1): 0x{}", Wire
					.conjugate(globals.R, BigInteger.ZERO).toString(16));
		}

		/**
		 * Sets the server data inputs to the circuit.
		 */
		public void setData(BigInteger y) {
			choices = y;
		}

	}

	class GenericOpClient extends InchwormOpCommon {
		public GenericOpClient(Object param) {
			super(GenericOpTest.rand, k);
			// Instantiate and build the circuit.
			serverMode = false;
			ccs = new Circuit[1];
			ccs[0] = getTestCircuit(globals, false, param);
		    ccs[0].build(serverMode);
			otNumOfPairs = getNumberOfInputs(param);
		}

		/**
		 * Sets the client data inputs into the adder circuit.
		 * 
		 * @param x
		 */
		public void setData(BigInteger x) {
			choices = x;
		}

	}

	@Before
	public void setup() throws IOException {
		logger.debug("setup()");
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

	}

	/**
	 * Run the client in a separate thread.
	 * 
	 * @param x
	 *            the input bits for the client
	 * @throws IOException
	 */
	public Future<BigInteger> runClient(final BigInteger x, Object param) throws IOException {

        client = new GenericOpClient(param);
        client.setParameters(chan1, otExt1);

        Future<BigInteger> clientFuture = pool.submit(new Callable<BigInteger>() {
            @Override
            public BigInteger call() throws Exception {
                logger.debug("client init()");
                client.init();

                client.setData(x);

                logger.debug("client run()");
                client.run();
                return client.opOutput;
            }
        });

        return clientFuture;
	}

    public Future<BigInteger> runClient(final BigInteger x)  throws IOException {
        return runClient(x, null);
    }

	/**
	 * Run the server in the main thread.
	 * 
	 * @param y
	 *            the input bits to the server
	 * @return the output results.
	 * @throws IOException
	 */
	public BigInteger runServer(BigInteger y, Object param) throws IOException {
        server = new GenericOpServer(param);
        server.setParameters(chan0, otExt0);

		logger.debug("server init()");
		server.init();

		server.setData(y);

		logger.debug("server run()");
		server.run();

		logger.debug("result={}", server.opOutput);
		return server.opOutput;
	}

    public BigInteger runServer(BigInteger y) throws IOException {
        return runServer(y, null);
    }

}
