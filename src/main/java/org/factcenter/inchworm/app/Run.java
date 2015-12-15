package org.factcenter.inchworm.app;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.factcenter.inchworm.MemoryFactory;
import org.factcenter.inchworm.Player;
import org.factcenter.inchworm.VMOpImplementation;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.concrete.CircuitMuxMemoryFactory;
import org.factcenter.inchworm.ops.concrete.ConcreteCommon;
import org.factcenter.inchworm.ops.concrete.ConcreteOPFactory;
import org.factcenter.inchworm.ops.concrete.FastMuxMemoryFactory;
import org.factcenter.inchworm.ops.dummy.DummyOPFactory;
import org.factcenter.pathORam.ops.GenericPathORAMFactory;
import org.factcenter.pathORam.ops.concrete.PathORAMFactory;
import org.factcenter.pathORam.ops.dummy.DummyPathORAMFactory;
import org.factcenter.qilin.comm.*;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.protocols.concrete.DefaultOTExtender;
import org.factcenter.qilin.util.PRGRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Random;

import static java.util.Arrays.asList;

//import ConcreteOPFactory;
//import pathORam.PathORAM;

/**
 * Run one party in an Inchworm secure computation. Which party, and the TCP endpoint of the peer is
 * specified on the command line.
 */
public class Run {

	protected final Logger logger = LoggerFactory.getLogger(getClass());


    /**
     * Options that must be shared between parties.
     */
    public static class SharedOptions implements Sendable {

        public boolean usePathORAM = false;
        public boolean useSecureOps = true;
        public boolean useCircuitMux = false;

        /**
         * default security parameter;
         */
        public int k = 80;

        /**
         * OT Extender block size
         */
        public int m = 400;

        /**
         * OT Extender low water mark
         */
        public int lowWaterMark = 1;

        /**
         * OT Extender high water mark
         */
        public int highWaterMark = m * 2;

        @Override
        public void writeTo(SendableOutput out) throws IOException {
            out.writeBoolean(usePathORAM);
            out.writeBoolean(useSecureOps);
            out.writeBoolean(useCircuitMux);
            out.writeInt(k);
            out.writeInt(m);
            out.writeInt(lowWaterMark);
            out.writeInt(highWaterMark);
        }

        @Override
        public void readFrom(SendableInput in) throws IOException {
            usePathORAM = in.readBoolean();
            useSecureOps = in.readBoolean();
            useCircuitMux = in.readBoolean();
            k = in.readInt();
            m = in.readInt();
            lowWaterMark = in.readInt();
            highWaterMark = in.readInt();
        }

        @Override
        public String toString() {
            return String.format("mem=[%s%s] %s k=%d,m=%d,lowWM=%d,highwm=%d", usePathORAM ? "pathORAM:" : "",
                    useCircuitMux ? "circuitMux" : "fastMux",
                    useSecureOps ? "secure" : "dummy", k, m, lowWaterMark, highWaterMark);
        }
    }

    SharedOptions defaultOptions = new SharedOptions();

    protected SharedOptions sharedOptions;

    protected boolean runAsServer;
	
	protected String progName;
	protected PrintStream errOut;

	public OptionParser parser;
	public OptionSet options;
	
	public int party;
	
	public String peerName;
	public int localPort;
	
	public VMState state;
	
	public File progDataFile = null;
	public File progCodeFile = null;
	public String inFile = null;
	public String outFile = null;
	public String outFormat;
	
	public InchwormIO ioHandler;
	
	public Player player;

    protected TCPChannelFactory channelFactory;

    protected Channel toPeer;
    protected Channel toPeerYao;
    protected Channel toPeerOTExtender;

    protected OTExtender otExtender;

    protected MemoryFactory memFactory;
    protected VMOpImplementation opFactory;

	public Random extenderRand;
    public Random playerRand;
	
	public InputStream progCodeStream;
	public InputStream progDataStream;
	
	public Run(String progName, PrintStream errOut) {
		this.progName = progName;
		this.errOut = errOut;
	}

	
	public void createOptions() {
		parser = new OptionParser();

		parser.acceptsAll(asList("h","help","?"), "print this message").forHelp();
		
		parser.accepts("verbose", "be extra verbose")
		.withOptionalArg().describedAs("verbosity level").ofType(Integer.class).defaultsTo(1);
		
		parser.accepts("sec", "security of ops (false=dummy ops)")
		.withRequiredArg().ofType(Boolean.class).defaultsTo(true);

        parser.acceptsAll(asList("circuitmux", "cm"), "use CircuitMux instead of FastMux for memory");

        parser.accepts("port", "local port to listen on (0 means first available)")
		.withRequiredArg().ofType(Integer.class).defaultsTo(0);
		
		parser.accepts("k", "security parameter")
		.withRequiredArg().ofType(Integer.class).defaultsTo(defaultOptions.k);
		
		parser.accepts("m", "OT extender block size")
		.withRequiredArg().ofType(Integer.class).defaultsTo(defaultOptions.m);

		parser.accepts("low", "OT extender low water mark")
		.withRequiredArg().ofType(Integer.class).defaultsTo(defaultOptions.lowWaterMark);

		parser.accepts("high", "OT extender high water mark")
		.withRequiredArg().ofType(Integer.class).defaultsTo(defaultOptions.highWaterMark);

		parser.accepts("connect", "Remote host:port")
		.withRequiredArg().ofType(String.class);

		parser.accepts("path", "use pathORAM protocol for RAM");

        parser.accepts("server", "Run server in infinite loop");
	}
	

	public void addFileOptions() {
		parser.accepts("code", "Inchworm assembly code file (required for player 0)")
			.withRequiredArg();
		
		parser.accepts("data", "Inchworm assembly data file")
			.withRequiredArg();
		
		parser.accepts("out", "Result output file (defaults to stdout)")
			.withRequiredArg();
		
		parser.accepts("outFormat", "Output file format (in String.format style, with args pc and value)")
		.withRequiredArg().ofType(String.class).defaultsTo("Output (pc=%d): %d\n");

		parser.accepts("in", "Input file (Use '-' for stdin; defaults to null)")
			.withRequiredArg();
	
	}

	
	public void printHelpAndExit(String msg, int retval)  {
		try {
			printHelp(msg, retval);
		} catch (IOException e) {
			
		}
		System.exit(retval);
	}

	void printHelp(String msg, int retval) 
			throws IOException {
		if (msg != null) {
			errOut.println(msg);
		}
		errOut.println(progName + " [Options]");
		errOut.println();
		errOut.println("If --connect is specified, runs as player 0.");
		parser.printHelpOn(errOut);
	}


    /**
     * Override if a subclass adds shared options.
     */
    protected void createSharedOptions() { sharedOptions =  new SharedOptions(); }

	public void parse(String[] args) {
        createSharedOptions();

		options = parser.parse(args);
		
		if (options.has("help")) {
			printHelpAndExit(null, 0);
		}

		localPort = (Integer) options.valueOf("port");
		
		// Party 0 (Left party) must specify the remote (server:port) information.
		if (options.has("connect")) {
			peerName = ((String)options.valueOf("connect"));
            party = 0;
		} else {
			party = 1;
		}

        sharedOptions.k = (Integer) options.valueOf("k");
        sharedOptions.m = (Integer) options.valueOf("m");
        sharedOptions.lowWaterMark = (Integer) options.valueOf("low");
        sharedOptions.highWaterMark = (Integer) options.valueOf("high");
		
        // Set the ops.
        sharedOptions.useSecureOps = (Boolean) options.valueOf("sec");

        sharedOptions.useCircuitMux = options.has("circuitmux");

        sharedOptions.usePathORAM = options.has("path");

        runAsServer = options.has("server");
    }
	
	public void parseFileOptions() {
		if (options.has("data")) {
			progDataFile = new File((String) options.valueOf("data"));

			if (!progDataFile.exists()) {
				printHelpAndExit("Data file " + progDataFile + " doesn't exist!", -1);
			}
		} else {
			progDataFile = null;
		}
		
		if (options.has("code")) {
			progCodeFile = new File((String) options.valueOf("code"));

			if (!progCodeFile.exists()) {
				printHelpAndExit("Code file " + progCodeFile + " doesn't exist!", -1);
			}
		} else {
            progCodeFile = null;
        }

		outFormat = (String) options.valueOf("outFormat");

		if (options.has("in")) {
			inFile = (String) options.valueOf("in");
		}

		if (options.has("out")) {
			outFile =  (String) options.valueOf("out");
		}
	}
		
	
	public void createIOHandler() {
		OutputStream out = System.out;
		if (outFile != null)
			try {
				out = new FileOutputStream(outFile);
			} catch (IOException e) {
				printHelpAndExit("Couldn't create output file " + outFile + ": " + e.getMessage(), -1);
			}
		
		InputStream in = null;
	
		if (inFile != null) {
			if (inFile.equals("-"))
				in = System.in;
			else {
				try {
					in = new FileInputStream(inFile);
				} catch (IOException e) {
					printHelpAndExit("Couldn't open input file " + inFile + ": " + e.getMessage(),  -1);
				}
			}
		}

		if (in != null) {
			ioHandler = new InputStreamIOHandler(in, out, outFormat);
		} else {
			ioHandler = new DefaultIOHandler(out, outFormat);
		}
	}
		

	public void openCodeStreams() throws IOException {
		if (progCodeFile != null)
			progCodeStream = new FileInputStream(progCodeFile);
		
		if (progDataFile != null)
			progDataStream = new FileInputStream(progDataFile);
	}

    public void createOpAndMemoryFactories() {
        if (sharedOptions.useSecureOps) {
            MemoryFactory baseMemory;
            if (sharedOptions.useCircuitMux)
                baseMemory = new CircuitMuxMemoryFactory();
            else
                baseMemory = new FastMuxMemoryFactory();

            // Instantiate the set of concrete ops (and set OT).
            opFactory = new ConcreteOPFactory();
            ((ConcreteCommon)opFactory).setMoreParameters(otExtender);

            if (sharedOptions.usePathORAM) {
                memFactory = new PathORAMFactory(baseMemory);
                ((GenericPathORAMFactory) memFactory).setMoreParameters(otExtender);
            } else {
                memFactory = baseMemory;
                ((ConcreteCommon) memFactory).setMoreParameters(otExtender);
            }
        } else {
            // Instantiate the set of dummy ops.
            DummyOPFactory dummyFactory = new DummyOPFactory();

            opFactory = dummyFactory;

            if (sharedOptions.usePathORAM) {
                memFactory = new DummyPathORAMFactory();
                ((GenericPathORAMFactory) memFactory).setMoreParameters(otExtender);
            } else {
                memFactory = dummyFactory;
            }
        }
    }


    public void setupTCPServer() throws IOException {
        channelFactory = new TCPChannelFactory(localPort);

        Thread tcpThread = new Thread(channelFactory, "TCPChannelFactory");
        tcpThread.setDaemon(true);
        tcpThread.start();
        logger.info("Running TCP Server on port {}", channelFactory.getLocalPort());
    }

    public void setupTCPChannels() throws IOException {
        if (peerName != null) {
            try {
                logger.warn("Party {}, connecting to {}", party, peerName);
                toPeer = channelFactory.getChannel(peerName);

                logger.debug("Party {}, connecting to {} (OT extender)", party, peerName);
                toPeerOTExtender = channelFactory.getChannel(peerName);
                toPeerYao = channelFactory.getChannel(peerName);
            } catch (IOException e) {
                throw new IOException("Error connecting to " + peerName + " (" + e.getMessage() +")");
            }
        } else {
            logger.warn("Party {}, waiting for connections on {}", party, channelFactory.getLocalname());
            toPeer = channelFactory.getChannel();
            logger.debug("Party {}, waiting for OT extender connection on {}", party, channelFactory.getLocalname());
            toPeerOTExtender = channelFactory.getChannel();
            toPeerYao = channelFactory.getChannel();
        }
    }

    public void setupOTExtenders() throws IOException {
        logger.debug("k={}, m={}, lowWaterMark={}, highWaterMark={}", sharedOptions.k, sharedOptions.m,
                sharedOptions.lowWaterMark, sharedOptions.highWaterMark);

        extenderRand = new PRGRandom();

        // ------ Start an DefaultOTExtender ------
        DefaultOTExtender defaultExtender = new DefaultOTExtender(sharedOptions.k, sharedOptions.m,
                sharedOptions.lowWaterMark, sharedOptions.highWaterMark, party);
        otExtender = defaultExtender;

        otExtender.setParameters(toPeer, playerRand);
        // Server runs in a separate thread, so needs a separate RNG instance.
        defaultExtender.setServerParameters(toPeerOTExtender, extenderRand);

    }


    public void setupStateAndPlayer() throws IOException {

        state = new VMState();

        state.setMemory(memFactory);

        playerRand = new PRGRandom();

        player = new Player(party, state, progCodeStream, progDataStream, opFactory, otExtender, ioHandler, playerRand, null);

        player.setChannel(toPeer);
        player.setYaoChannel(toPeerYao);

        memFactory.setParameters(party, state, player.getRunner(), playerRand);
    }

    public void init() throws IOException {

        logger.debug("Initializing OT Extender");
        otExtender.init();

        try {
            logger.debug("Initializing Memory Factory");
            memFactory.init();
        } catch (InterruptedException e) {
            logger.error("Unexpected exception: {}", e);
        }

        logger.debug("Initializing Player");
        player.init();
    }


    public void readSharedOptionsFromPeer() throws IOException {
        sharedOptions.readFrom(toPeer);
    }

    public void writeSharedOptionsToPeer() throws IOException {
        toPeer.writeObject(sharedOptions);
    }

	public void setupInchworm() throws IOException {
		setupTCPChannels();

        if (party == 0)
            writeSharedOptionsToPeer();
        else
            readSharedOptionsFromPeer();

        setupOTExtenders();

        createOpAndMemoryFactories();

        setupStateAndPlayer();

        init();
	}
	
	public void run(int maxSteps) throws IOException {
		if (maxSteps > 0)
			player.setMaxSteps(maxSteps);
		
		player.run();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Run run = new Run(Run.class.getCanonicalName(), System.out);
		/*-
		 *  To run the GCD sample using secure ops the following command line parameters
         *  for the left player can be used:
         *  --code ./examples/gcd.txt --sec true --port 10000 --data ./examples/gcd-left-data.txt
         *  for the right player:
         *  --port 11000 --data ./examples/gcd-right-data.txt --sec true HOST_NAME:10000
		 */
		
		run.createOptions();
		run.addFileOptions();
		
		run.parse(args);
		run.parseFileOptions();
		
		run.createIOHandler();

		try {
			run.openCodeStreams();
            run.setupTCPServer();

            do {
                if (run.runAsServer) {
                    run.logger.info("Waiting for player {} to connect", 1 - run.party);
                }
                run.setupInchworm();

	    		//logger.debug("Running party {}", party);
    			run.run(0);

                // We stop the current server and start another one.
                ((DefaultOTExtender)run.otExtender).stopServer();
            } while (run.runAsServer);

        } catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		} 
	}

}
