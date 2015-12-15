package test;

import org.factcenter.inchworm.MemoryArea;
import org.factcenter.inchworm.app.Run;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.comm.LocalChannelFactory;
import org.factcenter.qilin.comm.SendableInput;
import org.factcenter.qilin.comm.SendableOutput;
import org.factcenter.qilin.protocols.concrete.DefaultOTExtender;
import org.factcenter.qilin.protocols.generic.DummyOTExtender;
import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;

import static java.util.Arrays.asList;

/**
 * Created by talm on 9/11/14.
 */
public class MemoryAreaTimer extends Run {

    public static class SharedOptions extends Run.SharedOptions {
        int blockSize = 8;

        int blockCount = 256;

        int repeat = 20;


        @Override
        public void writeTo(SendableOutput out) throws IOException {
            super.writeTo(out);
            out.writeInt(blockSize);
            out.writeInt(blockCount);
            out.writeInt(repeat);
        }

        @Override
        public void readFrom(SendableInput in) throws IOException {
            super.readFrom(in);
            blockSize = in.readInt();
            blockCount = in.readInt();
            repeat = in.readInt();
        }

        @Override
        public String toString() {
            return super.toString() + String.format(" blocksize=%d,blockcount=%d,repeat=%d", blockSize, blockCount, repeat);
        }
    }


    MemoryAreaTimer localPeer;

    boolean local = false;

    boolean useLocalChannel = true;

    boolean useDummyOTExtender = true;

    SharedOptions defaultOptions = new SharedOptions();

    SharedOptions memSharedOptions;

    @Override
    protected void createSharedOptions() {
        sharedOptions = memSharedOptions = new SharedOptions();
    }

    public MemoryAreaTimer() {
        super("MemoryTimer", System.err);
    }


    @Override
    public void createOptions() {
        super.createOptions();


        parser.acceptsAll(asList("blocksize", "bs"), "block size for memory area")
                .withRequiredArg().ofType(Integer.class).defaultsTo(defaultOptions.blockSize);

        parser.acceptsAll(asList("blockcount", "bc"), "number of blocks in memory area")
                .withRequiredArg().ofType(Integer.class).defaultsTo(defaultOptions.blockCount);

        parser.accepts("repeat", "How many times to repeat test for computing average time")
                .withRequiredArg().ofType(Integer.class).defaultsTo(defaultOptions.repeat);

        parser.accepts("local", "Run two threads internally rather than between two parties");

    }

    @Override
    public void parse(String[] args) {
        super.parse(args);

        memSharedOptions.blockSize =  (Integer) options.valueOf("blocksize");
        memSharedOptions.blockCount = (Integer) options.valueOf("blockcount");
        memSharedOptions.repeat = (Integer) options.valueOf("repeat");
        local = options.has("local");
    }


    /**
     * Run the test and return average load and average store timings in ms
     * @return
     * @throws IOException
     */
    public double[] runTest() throws IOException {
        logger.info("Running with parameters: {}", memSharedOptions);
        logger.info("Initializing memory");
        MemoryArea area = memFactory.createNewMemoryArea(MemoryArea.Type.TYPE_RAM);
        area.init(memSharedOptions.blockSize, memSharedOptions.blockCount);

        int logCount = 32 - Integer.numberOfLeadingZeros(memSharedOptions.blockCount) - 1;
        BitMatrix zeroIdx = new BitMatrix(logCount);
        BitMatrix zeroBlock = new BitMatrix(memSharedOptions.blockSize);

        long loadSum = 0;

        {
            // First load might do extra initialization, we don't include it in average.
            logger.debug("Running initial load");
            long startTime = System.currentTimeMillis();
            area.loadOblivious(zeroIdx, 1);
            long stopTime = System.currentTimeMillis();
            long timing = stopTime - startTime;
            logger.debug("Initial load Test took {}ms", timing);
        }

        logger.info("Starting loadOblivious test");
        for (int i = 0; i < memSharedOptions.repeat; ++i) {
            logger.debug("Load Test {} starting", i);
            long startTime = System.currentTimeMillis();
            area.loadOblivious(zeroIdx, 1);
            long stopTime = System.currentTimeMillis();
            long timing = stopTime - startTime;
            logger.debug("Load Test {} took {}ms", i, timing);
            loadSum += timing;
        }

        double loadAve = ((double) loadSum) / memSharedOptions.repeat;

        logger.info("Average load time: {}ms", loadAve);

        long storeSum = 0;

        logger.info("Starting storeOblivious test");
        for (int i = 0; i < memSharedOptions.repeat; ++i) {
            logger.debug("Store Test {} starting", i);
            long startTime = System.currentTimeMillis();
            area.storeOblivious(zeroIdx, zeroBlock);
            long stopTime = System.currentTimeMillis();
            long timing = stopTime - startTime;
            logger.debug("Store Test {} took {}ms", i, timing);
            storeSum += timing;
        }

        double storeAve = ((double) storeSum) / memSharedOptions.repeat;

        logger.info("Average store time: {}ms", storeAve);

        double[] aves = { loadAve, storeAve };

        return aves;
    }

    @Override
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
    }

    public void setupLocal() throws IOException {

        localPeer = new MemoryAreaTimer();
        MemoryAreaTimer[] run = { this, localPeer };
        run[1].sharedOptions = run[1].memSharedOptions = run[0].memSharedOptions;


        if (useLocalChannel) {
            LocalChannelFactory lcf = new LocalChannelFactory();

            Channel[] peers = lcf.getChannelPair();
            Channel[] yaoPeers = lcf.getChannelPair();
            Channel[] extendPeers = lcf.getChannelPair();

            for (int i = 0; i < run.length; ++i) {
                run[i].toPeer = peers[i];
                run[i].toPeerYao = yaoPeers[i];
                run[i].toPeerOTExtender = extendPeers[i];
            }
        } else {
            run[0].setupTCPServer();
            run[1].setupTCPServer();

            run[0].peerName = "localhost:" + run[1].channelFactory.getLocalPort();
            run[0].setupTCPChannels();
            run[1].setupTCPChannels();
        }

        for (int i = 0; i < run.length; ++i) {

            if (useDummyOTExtender) {
                DummyOTExtender[] otExtenders = {new DummyOTExtender(), new DummyOTExtender()};
                run[i].otExtender = otExtenders[i];
                otExtenders[i].setParameters(run[i].toPeer, run[i].playerRand);
            } else {
                run[i].setupOTExtenders();
            }

            run[i].createIOHandler();
            run[i].createOpAndMemoryFactories();
            run[i].setupStateAndPlayer();
        }
    }


    public void runLocal() throws IOException {
        Thread localPeerThread = new Thread("localPeer") {
            @Override
            public void run()  {
                try {
                    localPeer.init();
                    localPeer.runTest();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };
        localPeerThread.setDaemon(true);
        localPeerThread.start();
        init();
        double[] aves = runTest();
        logger.info("Load ave: {}ms, Store ave: {}", aves[0], aves[1]);
    }


        /**
         * @param args
         */
    public static void main(String[] args) {

        MemoryAreaTimer run = new MemoryAreaTimer();

        run.createOptions();

        run.parse(args);

        try {
            if (run.local) {
                run.setupLocal();
                run.runLocal();
                return;
            }

            run.setupTCPServer();

            do {
                if (run.runAsServer) {
                    run.logger.info("Waiting for player {} to connect", 1 - run.party);
                }
                run.setupInchworm();

                // Run
                run.runTest();

                // We stop the current server and start another one.
                ((DefaultOTExtender)run.otExtender).stopServer();
            } while (run.runAsServer);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
