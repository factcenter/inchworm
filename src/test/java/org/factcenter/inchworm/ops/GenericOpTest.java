package org.factcenter.inchworm.ops;

import org.factcenter.inchworm.VMRunner;
import org.factcenter.inchworm.VMState;
import org.factcenter.inchworm.ops.dummy.DummyOPFactory;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.comm.LocalChannelFactory;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.protocols.generic.DummyOTExtender;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 * Generic test for the Call Op
 */
abstract public class GenericOpTest {
    final public int NUM_PLAYERS = 2;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected OTExtender[] otExtenders = { new DummyOTExtender(), new DummyOTExtender() };

    Random rand = new Random(0);

    protected final ExecutorService pool;

    protected GenericOpTest() {
        pool = Executors.newFixedThreadPool(1);
    }


    public class VM {
        public int playerId;
        public DummyOPFactory factory;
        public VMState state;
        public VMRunner runner;
        public Channel toPeer;
        public Channel toPeerYao;
        public Random rand;

        OpAction op;

        VM(int playerId, Channel toPeer, Channel toPeerYao) {
            this.playerId = playerId;
            this.toPeer = toPeer;
            this.toPeerYao = toPeerYao;
            this.rand = new Random(playerId);
            this.factory = new DummyOPFactory();
            this.state = new VMState();
            state.setMemory(factory);
            this.runner = new VMRunner(playerId, state, factory, rand, null);

            runner.setChannel(toPeer);
            runner.setYaoChannel(toPeerYao);
        }

        void init() throws Exception {

            state.setWordSize(wordSize);
            state.setRomPtrSize(romPtrSize);
            state.setRegPtrSize(regPtrSize);
            state.setFrameSize(numLocalRegs);
            state.setStackPtrSize(stackPtrSize);
            state.initMemory();

            factory.setParameters(playerId, state, runner, rand);
            factory.init();

            op = getOp(this);

        }
    }

    public int romPtrSize = 4;
    public int regPtrSize = 8;
    public int wordSize = 16;
    /**
     * Number of local registers.
     */
    public int numLocalRegs = 3;

    public int stackPtrSize = 3;


    protected VM[] vms;


    /**
     * Override and return the specific instance to test.
     * @return
     */
    abstract protected OpAction getOp(VM vm) throws Exception;

    @Before
    public void  setup() throws Exception {

        LocalChannelFactory channelFactory = new LocalChannelFactory();

        Channel[] channels = channelFactory.getChannelPair();
        Channel[] yaoChannels = channelFactory.getChannelPair();

        VM[] vms = { new VM(0, channels[0], yaoChannels[0]), new VM(1, channels[1], yaoChannels[1]) };
        this.vms = vms;

        for (int i = 0; i < otExtenders.length; ++i) {
            otExtenders[i].setParameters(channels[i], vms[i].rand);
        }

        for (int i = 0; i < vms.length; ++i) {
            vms[i].init();
        }
    }
}

