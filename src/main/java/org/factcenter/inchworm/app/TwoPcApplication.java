package org.factcenter.inchworm.app;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.factcenter.inchworm.MemoryFactory;
import org.factcenter.inchworm.Player;
import org.factcenter.inchworm.VMOpImplementation;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.debugger.ConsoleDebugger;
import org.factcenter.inchworm.debugger.Debugger;
import org.factcenter.inchworm.ops.concrete.ConcreteCommon;
import org.factcenter.inchworm.ops.concrete.ConcreteOPFactory;
import org.factcenter.inchworm.ops.concrete.FastMuxMemoryFactory;
import org.factcenter.inchworm.ops.dummy.DummyOPFactory;
import org.factcenter.pathORam.ops.GenericPathORAMFactory;
import org.factcenter.pathORam.ops.concrete.PathORAMFactory;
import org.factcenter.pathORam.ops.dummy.DummyPathORAMFactory;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.comm.LocalChannelFactory;
import org.factcenter.qilin.comm.LocalChannelFactory.LocalChannel;
import org.factcenter.qilin.protocols.concrete.DefaultOTExtender;
import org.factcenter.qilin.util.PRGRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Random;

import static java.util.Arrays.asList;
//import ConcreteOPFactory;
//import pathORam.PathORAM;

/**
 * The main class executing the 2PC as a single application. Should be used to
 * run the JUnit tests.
 * 
 * @author GILBZ
 * 
 */
public class TwoPcApplication {
	public static final String DEFAULT_DEBUG_OUTPUT_FORMAT = "Computation Result (Program counter = %05d) =  %d (%x)\n";

	final Logger logger = LoggerFactory.getLogger(getClass());

	/*-
	 * ----------------------------------------------------------------
	 *                      Data members 
	 * ----------------------------------------------------------------
	 */

	/**
	 * Left player running the application.
	 */
	private Player pl;

	/**
	 * Communication channels of left player.
	 */
	Channel lClientChannel;
	Channel lServerChannel;
	Channel lYaoChannel;

	/**
	 * Extender for left player
	 */
	DefaultOTExtender otExtender0;

	/**
	 * @return the left player
	 */
	public Player getLeftPlayer() {
		return pl;
	}

	/**
	 * Right player running the application.
	 */
	private Player pr;

	/**
	 * Communication channels of right player.
	 */
	Channel rClientChannel;
	Channel rServerChannel;
	Channel rYaoChannel;

	/**
	 * Extender for right player.
	 */
	DefaultOTExtender otExtender1;

	static boolean debug;
	/**
	 * @return the right player
	 */
	public Player getRightPlayer() {
		return pr;
	}

	// -- parameters for OT Extender ---
	static final int DEFAULT_k = 80;
	static final int DEFAULT_m = 800;
	static final int DEFAULT_lowWaterMark = 1;
	static final int DEFAULT_highWaterMark = DEFAULT_m;

	int k = DEFAULT_k;
	int m = DEFAULT_m;
	int lowWaterMark = DEFAULT_highWaterMark;
	int highWaterMark = DEFAULT_highWaterMark;
	boolean usePathORAM = false;

    /**
     * Random generator for left player.
     */
	Random[] extenderRand = new Random[2];

    /**
     * Random generator for right player.
     */
    Random[] playerRand = new Random[2];

	boolean useSecureOps;
	InputStream leftCode;
	InputStream leftData;
	InputStream rightCode;
	InputStream rightData;
	
	InchwormIO leftIO;
	InchwormIO rightIO;
	int maxSteps;

	boolean initialized = false;

	/*-
	 * ----------------------------------------------------------------
	 *                      Constructor(s)
	 * ----------------------------------------------------------------
	 */

	/**
	 * Constructor for running the 2PC as a single executable.
	 * 
	 * @param useSecureOps
	 *            - flag for setting the set of ops to be used (secure / not
	 *            secure).
	 * @param leftCode
	 *            - assembly source code to run for left player.
	 * @param leftData
	 *            - data file to use for left player.
	 * @param rightCode
	 *            - assembly source code to run for right player.
	 * @param rightData
	 *            - data file to use for right player.
	 * @param leftIO input/output handler for left player
	 * @param rightIO input/output handler for right player
	 * @param maxSteps maximum number of instructions to execute (-1 means unbounded)
	 */
	public TwoPcApplication(boolean useSecureOps, boolean useZeroRand, boolean usePathORAM, InputStream leftCode,
			InputStream leftData, InputStream rightCode, InputStream rightData,
			InchwormIO leftIO, InchwormIO rightIO, int maxSteps) {
		this.useSecureOps = useSecureOps;
        if (useZeroRand) {
            extenderRand[0] = extenderRand[1] = playerRand[0] = playerRand[1] = new Random() {
                public int next(int bits) { return 0; }
            };
        } else {
            for (int i = 0; i < 2; ++i) {
                extenderRand[i] = new PRGRandom();
                playerRand[i] = new PRGRandom();
            }
        }
		this.usePathORAM = usePathORAM;
		this.leftCode = leftCode;
		this.leftData = leftData;
		this.rightCode = rightCode;
		this.rightData = rightData;
		this.leftIO = leftIO;
		this.rightIO = rightIO;
		this.maxSteps = maxSteps;
	}

	/**
	 * Constructor for running the 2PC as a single executable.
	 * (convenience method when we don't need input) 
	 * @param useSecureOps
	 *            - flag for setting the set of ops to be used (secure / not
	 *            secure).
	 * @param leftCode
	 *            - assembly source code to run.
	 * @param leftData
	 *            - data file to run.
	 * @param rightCode
	 *            - assembly source code to run.
	 * @param rightData
	 *            - data file to run.
	 */
	public TwoPcApplication(boolean useSecureOps, boolean useZeroRand, InputStream leftCode,
			InputStream leftData, InputStream rightCode, InputStream rightData,
			OutputStream leftOut, OutputStream rightOut) {
		this(useSecureOps, useZeroRand, false, leftCode,leftData,rightCode,rightData,
				new DefaultIOHandler(leftOut, DEFAULT_DEBUG_OUTPUT_FORMAT),
				new DefaultIOHandler(rightOut, DEFAULT_DEBUG_OUTPUT_FORMAT), -1);
	}
	
	/**
	 * Special constructor for running the ops integration tests.
	 * 
	 * @param useSecureOps
	 *            - flag for setting the set of ops.
	 * @param progFile
	 *            - assembly source code to run (left player only).
	 * @param leftDataFile
	 *            - data file to run.
	 * @param rightDataFile
	 *            - data file to run.
	 */
	public TwoPcApplication(boolean useSecureOps, boolean useZeroRand, InputStream progFile,
			InputStream leftDataFile, InputStream rightDataFile,
			OutputStream leftOut, OutputStream rightOut) throws IOException {
		this(useSecureOps, useZeroRand, progFile, leftDataFile, null, rightDataFile,
				leftOut, rightOut);
	}

	/*-
	 * ----------------------------------------------------------------
	 *                      Public Methods 
	 * ----------------------------------------------------------------
	 */

	public void init() throws IOException {

		createCommChannels();
		createOTExtender();

        MemoryFactory memFactory0, memFactory1;
        VMOpImplementation opFactory0, opFactory1;

		if (useSecureOps) {
			// Instantiate the set of concrete ops (and set OT).
			opFactory0 = new ConcreteOPFactory();
            ((ConcreteCommon)opFactory0).setMoreParameters(otExtender0);

			opFactory1 = new ConcreteOPFactory();
            ((ConcreteCommon)opFactory1).setMoreParameters(otExtender1);

            if (usePathORAM) {
                memFactory0 = new PathORAMFactory();
                ((GenericPathORAMFactory) memFactory0).setMoreParameters(otExtender0);
                memFactory1 = new PathORAMFactory();
                ((GenericPathORAMFactory) memFactory1).setMoreParameters(otExtender1);
            } else {
                memFactory0 = new FastMuxMemoryFactory();
                ((ConcreteCommon) memFactory0).setMoreParameters(otExtender0);
                memFactory1 = new FastMuxMemoryFactory();
                ((ConcreteCommon) memFactory1).setMoreParameters(otExtender1);
            }
		} else {
			// Instantiate the set of dummy ops.
            DummyOPFactory fact0 = new DummyOPFactory();
            DummyOPFactory fact1 = new DummyOPFactory();

            opFactory0 = fact0; opFactory1 = fact1;

            if (usePathORAM) {
                memFactory0 = new DummyPathORAMFactory();
                ((GenericPathORAMFactory) memFactory0).setMoreParameters(otExtender0);
                memFactory1 = new DummyPathORAMFactory();
                ((GenericPathORAMFactory) memFactory1).setMoreParameters(otExtender1);
            } else {
                memFactory0 = fact0;
                memFactory1 = fact1;
            }
		}

        VMState state0 = new VMState();
        state0.setMemory(memFactory0);

        VMState state1 = new VMState();
        state1.setMemory(memFactory1);
        
        Debugger debugger = null;
        if (debug) {
        	debugger = new ConsoleDebugger(this);
        }

        // Left player has the full state in the non-secure version,
        // so debug only the left player.
        // todo: figure out what to do with debugging in the secure version.
        this.pl = new Player(0, state0, leftCode, leftData, opFactory0,
                otExtender0, leftIO, playerRand[0], debugger);
        this.pr = new Player(1, state1, rightCode, rightData, opFactory1,
                otExtender1, rightIO, playerRand[1], null);

        this.pl.setChannel(lClientChannel);
		this.pr.setChannel(rClientChannel);

		// Set channel for doing the Yao's protocol.
		this.pl.setYaoChannel(lYaoChannel);
		this.pr.setYaoChannel(rYaoChannel);

        memFactory0.setParameters(0, state0, pl.getRunner(), playerRand[0]);
        memFactory1.setParameters(1, state1, pr.getRunner(), playerRand[1]);

        try {
            memFactory0.init();
            memFactory1.init();
        } catch (InterruptedException e) {
            logger.error("Unexpected exception: {}", e);
        }

		this.pl.init();
		this.pr.init();

		initialized = true;
	}

	/**
	 * runs the secure computation process for two players.
	 * 
	 */
	public void run2PC() throws IOException {
		if (!initialized) {
			init();
		}
		
		// Start running the threads of both players.
		this.pr.setMaxSteps(maxSteps);
		Thread rightPlayerThread = new Thread(this.pr, "Right");
		rightPlayerThread.start();

		
		this.pl.setMaxSteps(maxSteps);
		String oldName = Thread.currentThread().getName();
		Thread.currentThread().setName("Left");
		this.pl.run();
		Thread.currentThread().setName(oldName);
		try {
			rightPlayerThread.join();
		} catch (InterruptedException e) {
			// Ignore
		}
		printChannelStats();
	}

	private void printChannelStats() {
		logger.info("left client read={} wrote={}",
				((LocalChannel) lClientChannel).getBytesRead(),
				((LocalChannel) lClientChannel).getBytesWritten());
		logger.info("right client read={} wrote={}",
				((LocalChannel) rClientChannel).getBytesRead(),
				((LocalChannel) rClientChannel).getBytesWritten());

		logger.info("left server read={} wrote={}",
				((LocalChannel) lServerChannel).getBytesRead(),
				((LocalChannel) lServerChannel).getBytesWritten());
		logger.info("right server read={} wrote={}",
				((LocalChannel) rServerChannel).getBytesRead(),
				((LocalChannel) rServerChannel).getBytesWritten());

		logger.info("left yao read={} wrote={}",
				((LocalChannel) lYaoChannel).getBytesRead(),
				((LocalChannel) lYaoChannel).getBytesWritten());
		logger.info("right yao read={} wrote={}",
				((LocalChannel) rYaoChannel).getBytesRead(),
				((LocalChannel) rYaoChannel).getBytesWritten());
		long totalBandwith = ((LocalChannel) lClientChannel).getBytesRead()
				+ ((LocalChannel) lClientChannel).getBytesWritten()
				+ ((LocalChannel) lServerChannel).getBytesRead()
				+ ((LocalChannel) lServerChannel).getBytesWritten()
				+ ((LocalChannel) lYaoChannel).getBytesRead()
				+ ((LocalChannel) lYaoChannel).getBytesWritten();
		logger.info("Total bandwith = {}", totalBandwith);

	}

	static OptionParser createOptions() {
		OptionParser parser = new OptionParser();

		parser.acceptsAll(asList("h", "help", "?"), "print this message")
				.forHelp();

		parser.accepts("verbose", "be extra verbose").withOptionalArg()
				.describedAs("verbosity level").ofType(Integer.class)
				.defaultsTo(1);
		
		parser.accepts("debug", "use console debugger");
		
		parser.accepts("sec", "security of ops").withRequiredArg()
				.ofType(Boolean.class).defaultsTo(true);

		parser.accepts("k", "security parameter").withRequiredArg()
				.ofType(Integer.class).defaultsTo(DEFAULT_k);

		parser.accepts("m", "OT extender block size").withRequiredArg()
				.ofType(Integer.class).defaultsTo(DEFAULT_m);

		parser.accepts("low", "OT extender low water mark").withRequiredArg()
				.ofType(Integer.class).defaultsTo(DEFAULT_lowWaterMark);

		parser.accepts("high", "OT extender high water mark").withRequiredArg()
				.ofType(Integer.class).defaultsTo(DEFAULT_highWaterMark);

		parser.accepts("out", "Left result output file (defaults to stdout)")
				.withRequiredArg();

		parser.accepts("rout", "Right result output file (defaults to null)")
				.withRequiredArg();

		parser.accepts("code",
				"Inchworm assembly code file (required for player 0)")
				.withRequiredArg();

		parser.accepts("data", "Inchworm assembly data file").withRequiredArg()
				.required();

		return parser;
	}

	static void printHelpAndExit(String msg, OptionParser parser, int retval) {
		if (msg != null) {
			System.out.println(msg);
		}
		System.out.println(TwoPcApplication.class.getSimpleName()
				+ " [Options] code-file left-data right-data");
		System.out.println();
		System.out.println("If code file is specified, runs as player 0 (left player).");
		try {
			parser.printHelpOn(System.out);
		} catch (IOException e) {
			// ignore
		}
		System.exit(retval);
	}

	/**
	 * Program entry point.
	 * 
	 * @param args
	 *            - command line parameters (left player source file, left and
	 *            right data files).
	 */
	public static void main(String args[]) {
		OptionParser parser = createOptions();

		OptionSpec<File> nonOptionSpec = parser.nonOptions().ofType(File.class);
		OptionSet options = parser.parse(args);
		List<File> files = options.valuesOf(nonOptionSpec);

		if (options.has("help")) {
			printHelpAndExit(null, parser, 0);
		}

		if (files.size() < 3) {
			printHelpAndExit(
					"You need to specify a code file and two data files!",
					parser, -1);
		}

		// Get source code to run.
		File progCodeFile = files.get(0);

		// Get the players data files.
		File leftPlayerDataFile = files.get(1);

		File rightPlayerDataFile = files.get(2);

		OutputStream out = System.out;
		OutputStream rout = null;

		boolean secureOps = (Boolean) options.valueOf("sec");

		try {
			// Create two players for running a secure computation session.

			if (options.has("out")) {
				out = new FileOutputStream((String) options.valueOf("out"));
			}

			if (options.has("rout")) {
				rout = new FileOutputStream((String) options.valueOf("rout"));
			}
			
			if (options.has("debug")) {
				debug = true;
			}
			
			TwoPcApplication tpc = new TwoPcApplication(secureOps, false,
					new FileInputStream(progCodeFile), new FileInputStream(
							leftPlayerDataFile), null, new FileInputStream(
							rightPlayerDataFile), out, rout);

			tpc.k = (Integer) options.valueOf("k");
			tpc.m = (Integer) options.valueOf("m");
			tpc.lowWaterMark = (Integer) options.valueOf("low");
			tpc.highWaterMark = (Integer) options.valueOf("high");

			tpc.init();

			// Run the secure computation.
			tpc.run2PC();
		} catch (IOException io) {
			io.printStackTrace();
			System.exit(-1);
		}
	}

	/*-
	 * ----------------------------------------------------------------
	 *                      Private Methods 
	 * ----------------------------------------------------------------
	 */

	/**
	 * Create the and initialize the communication channels of the application.
	 */
	void createCommChannels() {
		// Use local channel -- this allows tests to ignore communication
		// problems.
		LocalChannelFactory channelFactory = new LocalChannelFactory();

		Channel[] channelPair = channelFactory
				.getChannelPair("ClientChannel");
		lClientChannel = channelPair[0];
		rClientChannel = channelPair[1];

		Channel[] channelPairServer = channelFactory
				.getChannelPair("ServerChannel");
		lServerChannel = channelPairServer[0];
		rServerChannel = channelPairServer[1];

		Channel[]channelPairYao = channelFactory
				.getChannelPair("YaoChannel");
		lYaoChannel = channelPairYao[0];
		rYaoChannel = channelPairYao[1];
	}

	/**
	 * Create and initialize the OT extender.
	 */
	void createOTExtender() throws IOException {

		// Start DefaultOTExtenders for both players.
		otExtender0 = new DefaultOTExtender(k, m, lowWaterMark, highWaterMark,
				0);
		otExtender1 = new DefaultOTExtender(k, m, lowWaterMark, highWaterMark,
				1);

		otExtender0.setParameters(lClientChannel, playerRand[0]);
		otExtender1.setParameters(rClientChannel, playerRand[1]);

		otExtender0.setServerParameters(lServerChannel, extenderRand[0]);
		otExtender1.setServerParameters(rServerChannel, extenderRand[1]);

		otExtender0.init();
		otExtender1.init();
	}

}
