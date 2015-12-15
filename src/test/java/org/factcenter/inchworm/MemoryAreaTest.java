package org.factcenter.inchworm;


import org.factcenter.inchworm.ops.dummy.DummyOPFactory;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.comm.LocalChannelFactory;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.protocols.generic.DummyOTExtender;
import org.factcenter.qilin.util.BitMatrix;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.runners.Parameterized.Parameters;


@RunWith(Parameterized.class)
abstract public class MemoryAreaTest  {


    public final static int NUM_PLAYERS = 2;

    protected VMState[] states = new VMState[NUM_PLAYERS];
    protected DummyOPFactory[] opFactories = new DummyOPFactory[NUM_PLAYERS];
    protected VMRunner[] runners = new VMRunner[NUM_PLAYERS];

    protected OTExtender[] otExtenders = new OTExtender[NUM_PLAYERS];

    protected MemoryFactory[] factories = new MemoryFactory[NUM_PLAYERS];
    protected MemoryArea[] rams = new MemoryArea[NUM_PLAYERS];


    // {blockSize,blockCount} parameters for testing.
    @Parameters
    public static Collection<Integer[]> memSizes() {
        return Arrays.asList(new Integer[][]{
                {8, 4}, {8, 256}, {3, 16}, {153, 64}, {87, 128},
        });
    }


    final static int TEST_COUNT = 20;

    /**
     * Set up all the common VM components for both parties.
     * @throws Exception
     */
    protected void setupVM() throws Exception {

        LocalChannelFactory channelFactory = new LocalChannelFactory();

        Channel[] channelPair = channelFactory.getChannelPair();

        for (int i = 0; i < NUM_PLAYERS; ++i) {
            opFactories[i] = new DummyOPFactory();

            states[i] = new VMState();

            runners[i] = new VMRunner(i, states[i], opFactories[i], cryptoRand, null);
            runners[i].setChannel(channelPair[i]);

            otExtenders[i] = new DummyOTExtender();
            otExtenders[i].setParameters(channelPair[i], cryptoRand);

            opFactories[i].setParameters(i, states[i], runners[i], cryptoRand);

            factories[i] = createAndSetupMemoryFactory(i);

            factories[i].setParameters(i, states[i], runners[i], cryptoRand);

            rams[i] = factories[i].createNewMemoryArea(MemoryArea.Type.TYPE_RAM);

        }

        Future<Void> initRes = pool.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                factories[1].init();
                opFactories[1].init();
                rams[1].init(blockSize, blockCount);
                return null;
            }
        });
        factories[0].init();
        opFactories[0].init();
        rams[0].init(blockSize, blockCount);
        initRes.get();
    }



    /**
     * Randomness used for crypto operations (as opposed to test instances).
     * Can override this with {@link #zeroRandom} to make debugging easier.
     */
    protected Random cryptoRand = new Random(1);

    /**
     * Randomness used for testing instances.
     */
    protected Random testRand = new Random(0);

    /**
     * A random instance that always returns zeroes.
     */
    public static Random zeroRandom = new Random() {
        @Override
        protected int next(int bits) { return 0; }
    };

    /**
     * Subclasses should implement this method to create and set up a memory factory for player <i>i</i>.
     * When this method is called, the {@link #states}, {@link #runners}, {@link #opFactories} and {@link #otExtenders}
     * fields are already set up.
     * @return the new memory factory
     */
    protected abstract MemoryFactory createAndSetupMemoryFactory(int playerId) throws Exception;

    /**
     *
     * @return the MemoryArea to be tested
     */
    protected MemoryArea getMemoryArea() { return rams[0]; }

    /**
     * Load a memory share from the peer at a publicly-known location
     * @param idx Index (in blocks) of the share to return
     * @param num Number of (consecutive) blocks to return
     * @return A {@link Callable} that will actually return the share of memory held by the peer.
     * @throws Exception
     */
    protected Callable<BitMatrix> peerLoad(final int idx, final int num)  throws Exception  {
        return new Callable<BitMatrix>() {
            @Override
            public BitMatrix call() throws Exception {
                return rams[1].load(idx, num);
            }
        };
    }

    /**
     * Store a memory share to the peer at a publicly-known location
     * @param idx  Index (in blocks) at which the share should be stored
     * @param blockShare The share of blocks to store (may be multiple blocks)
     * @return A {@link Callable}  that will actually store the share to the peer.
     * @throws Exception
     */
    protected Callable<Void> peerStore(final int idx, final BitMatrix blockShare)  throws Exception  {
        return new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                rams[1].store(idx, blockShare);
                return null;
            }
        };
    }


    /**
     * Load a memory share from the peer at a location whose address is secret-shared between the parties.
     * @param idxShare The peer's share of the index from which to load
     * @param num The number of blocks to load
     * @return A {@link Callable} that performs that actual load
     * @throws Exception
     */
    protected Callable<BitMatrix> peerLoadOblivious(final BitMatrix idxShare, final int num)  throws Exception  {
        return new Callable<BitMatrix>() {
            @Override
            public BitMatrix call() throws Exception {
                return rams[1].loadOblivious(idxShare, num);
            }
        };
    }

    /**
     * Store a memory share to the peer at a location whose address is secret-shared between the parties.
     * @param idxShare The peer's share of the index from which to load
     * @param blockShare The share of blocks to store (may be multiple blocks)
     * @return A {@link Callable}  that will actually store the share to the peer.
     * @throws Exception
     */
    protected Callable<Void>  peerStoreOblivious(final BitMatrix idxShare, final BitMatrix blockShare)  throws Exception  {
        return new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                rams[1].storeOblivious(idxShare, blockShare);
                return null;
            }
        };
    }


    protected final ExecutorService pool;

    MemoryArea mem;

    int blockSize;
    protected int blockCount;

    BitMatrix memContents;

    protected MemoryAreaTest(int blockSize, int blockCount) {
        this.blockSize = blockSize;
        this.blockCount = blockCount;

        pool = Executors.newFixedThreadPool(1);
    }


    protected int[] getRandomIndexAndNum() {
        int idx = testRand.nextInt(blockCount);
        int num;
        if (idx < blockCount - 1)
            num = testRand.nextInt(blockCount - idx - 1) + 1;
        else
            num = 1;

        int[] pair = {idx, num};
        return pair;
    }

    @Before
    public void setup() throws Exception {
        setupVM();

        mem = getMemoryArea();

        BitMatrix zeroes = new BitMatrix(blockSize * blockCount);
        memContents = zeroes.clone();

        // Make sure the peer shares are 0.
        Callable<Void> peerTask = peerStore(0, zeroes);

        // Fill contents with counter (makes it easier to see what went wrong
        // during debugging
        byte[] contentBackingArray = memContents.getBackingArray();

        for (int i = 0; i < contentBackingArray.length; ++i) {
            contentBackingArray[i] = (byte) i;
        }

        memContents.zeroPad();

        pool.submit(peerTask);
        mem.store(0, memContents);
    }


    @Test
    public void testLoad() throws Exception {
        for (int i = 0; i < TEST_COUNT; ++i) {
            int[] idxNum = getRandomIndexAndNum();
            int idx = idxNum[0];
            int num = idxNum[1];

            Future<BitMatrix> peerResultPending = pool.submit(peerLoad(idx, num));
            BitMatrix val = mem.load(idx, num);

            BitMatrix peerVal = peerResultPending.get();
            val.xor(peerVal);

            BitMatrix expected = memContents.getSubMatrixCols(idx * blockSize, num * blockSize);
            assertEquals(expected, val);
        }
    }


    @Test
    public void testStore() throws Exception {
        for (int i = 0; i < TEST_COUNT; ++i) {
            int[] idxNum = getRandomIndexAndNum();
            int idx = idxNum[0];
            int num = idxNum[1];

            BitMatrix val = new BitMatrix(num * blockSize);
            val.fillRandom(cryptoRand);

            BitMatrix peerShare = new BitMatrix(num * blockSize);
            peerShare.fillRandom(cryptoRand);

            BitMatrix myShare = val.clone();
            myShare.xor(peerShare);

            Future<Void> peerResultPending = pool.submit(peerStore(idx, peerShare));

            mem.store(idx, myShare);
            peerResultPending.get();

            memContents.setBits(idx * blockSize, val);
        }

        Future<BitMatrix> peerResultPending = pool.submit(peerLoad(0, blockCount));
        BitMatrix result = mem.load(0, blockCount);
        BitMatrix peerResult = peerResultPending.get();
        result.xor(peerResult);

        assertEquals(memContents, result);
    }

    @Test
    public void testLoadOblivious() throws Exception {

        for (int i = 0; i < TEST_COUNT; ++i) {
            int[] idxNum = getRandomIndexAndNum();
            int idx = idxNum[0];
            int num = idxNum[1];

            int idxPeer = cryptoRand.nextInt();
            int idxShare = idx ^ idxPeer;

            Future<BitMatrix> peerResultPending = pool.submit(peerLoadOblivious(BitMatrix.valueOf(idxPeer, 32), num));
            BitMatrix val = mem.loadOblivious(BitMatrix.valueOf(idxShare, 32), num);
            BitMatrix peerVal = peerResultPending.get();

            val.xor(peerVal);

            BitMatrix expected = memContents.getSubMatrixCols(idx * blockSize, num * blockSize);
            assertEquals(String.format("Failed in set %d (idx=%d, num=%d, blockSize=%d, blockCount=%d)",
                    i, idx, num, blockSize, blockCount), expected, val);
        }
    }

    @Test
    public void testStoreOblivious() throws Exception {
        for (int i = 0; i < TEST_COUNT; ++i) {
            int[] idxNum = getRandomIndexAndNum();
            int idx = idxNum[0];
            int num = idxNum[1];

            int idxPeer = cryptoRand.nextInt();
            int idxShare = idx ^ idxPeer;

            BitMatrix val = new BitMatrix(num * blockSize);
            val.fillRandom(cryptoRand);

            BitMatrix peerShare = new BitMatrix(num * blockSize);
            peerShare.fillRandom(cryptoRand);

            BitMatrix myShare = val.clone();
            myShare.xor(peerShare);

            Future<Void> peerResultPending = pool.submit(peerStoreOblivious(BitMatrix.valueOf(idxPeer, 32), peerShare));

            mem.storeOblivious(BitMatrix.valueOf(idxShare, 32), myShare);
            peerResultPending.get();

            memContents.setBits(idx * blockSize, val);
        }

        Future<BitMatrix> peerResultPending = pool.submit(peerLoad(0, blockCount));
        BitMatrix result = mem.load(0, blockCount);
        BitMatrix peerResult = peerResultPending.get();
        result.xor(peerResult);

        assertEquals(memContents, result);
    }
}