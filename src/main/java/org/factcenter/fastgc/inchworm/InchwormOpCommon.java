package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.Utils.Utils;
import org.factcenter.fastgc.YaoGC.Circuit;
import org.factcenter.fastgc.YaoGC.CircuitGlobals;
import org.factcenter.fastgc.YaoGC.State;
import org.factcenter.fastgc.YaoGC.Wire;
import org.factcenter.qilin.comm.Channel;
import org.factcenter.qilin.protocols.OTExtender;
import org.factcenter.qilin.util.BitMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;

/**
 * Abstract class for using both the client and server versions of the ops circuits
 * inside an Inchworm VM.
 */
public abstract class InchwormOpCommon {

	final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Flag indicating client or server mode of operation.
	 */
	protected boolean serverMode;

	/**
	 * Input to client / server op. Bit representation of all input wires (LSB to MSB - share of X
	 * input + share of Y input + random value).
	 */
	BigInteger choices;

	/**
	 * Global circuit parameters
	 */
	protected CircuitGlobals globals;

	/**
	 * A secure random number generator.
	 */
	protected Random rand;

	/**
	 * OT Extender.
	 */
	protected OTExtender otExtender;

	/**
	 * Communication channel
	 */
	protected Channel toPeer;

	/**
	 * Number of OT pairs needed for the op inputs. This should be equal to the number of input bits
	 * held by the client / server. (we assume they have the same number of inputs)
	 */
	protected int otNumOfPairs;


    /**
     * Execution serial number.
     * Incremented before every execution.
     */
    protected int execSerial = 0;

	/**
	 * Circuit implementing the op.
	 */
	public Circuit[] ccs;

	/**
	 * Server side inputs
	 */
	public BigInteger[] serverLabels;
	/**
	 * Client side inputs
	 */
	public BigInteger[] clientLabels;

	/**
	 * State holding the computation result.
	 */
	public State outState;

	/**
	 * BigInt holding the decoded computation result.
	 */
	public BigInteger opOutput;

	/**
	 * The receiver side of the OT extender server/client pair.
	 */
	protected OnlineOTReceiver rcver;

	/**
	 * The sender side of the OT extender server/client pair.
	 */
	protected OnlineOTSender snder;

	/**
	 * Length in bits of a single OT message.
	 */
	protected int otMsgBitLength; // = globals.labelBitLength;

	/**
	 * Label pairs (client and server).
	 */
	protected BigInteger[][] sBitslps, cBitslps;

	/**
	 * Must call this before calling {@link #init()} (or using).
     * Must set {@link #otNumOfPairs} before calling this.
	 * 
	 * @param toPeer
	 *            - communication channel for Yao's circuits.
	 * @param otExtender
	 *            - OT Extender.
	 */
	public void setParameters(Channel toPeer, OTExtender otExtender) {
		this.otExtender = otExtender;
		this.toPeer = toPeer;
		if (serverMode) {
			snder = new OnlineOTSender(otNumOfPairs, otMsgBitLength);
			snder.setParameters(otExtender);
		} else {
			rcver = new OnlineOTReceiver(otNumOfPairs);
			rcver.setParameters(otExtender);
		}
	}

	/**
	 * Constructor (perform initialization before the instantiation of a subclass).
	 * 
	 * @param rand
	 *            - a secure random number generator.
	 * @param labelBitLength
	 *            - Length in bits of a single label.
	 */
	public InchwormOpCommon(Random rand, int labelBitLength) {
		this.rand = rand;
		globals = new CircuitGlobals(rand, labelBitLength);
		otMsgBitLength = globals.labelBitLength;
	}

	/**
	 * One-time Initialization (can involve communication with peer). Must call this after
	 * {@link #setParameters(Channel, OTExtender)} and before making any use of the class.
	 * 
	 * @throws IOException
	 */
	public void init() throws IOException {
		for (int i = 0; i < ccs.length; ++i) {
			ccs[i].setIOStream(toPeer, toPeer);
		}
	}

	/**
	 * Method for running the ops (client and server).
	 * 
	 * @throws IOException
	 */
	public void run() throws IOException {

		execute();

	}

    /**
     * Set the input data.
     * Inputs are concatenated into one BigInteger, with the first being the least significant.
     * @param inputs
     */
    public void setData(BitMatrix... inputs) {
        choices = BigInteger.ZERO;
        for (int i = inputs.length - 1; i >= 0; --i)
            choices = choices.shiftLeft(inputs[i].getNumCols()).or(inputs[i].toBigInteger());
    }


	/**
	 * Send server inputs data to the client side (fills the client side inputs
	 * label) using an OT protocol.
	 * @throws IOException
	 */
	protected void execTransfer() throws IOException {

		if (serverMode) {
			/*-
			 * Send server inputs data to the client side (fills the client side inputs label).
			 */
			int bytelength = (globals.labelBitLength - 1) / 8 + 1;
			for (int i = 0; i < otNumOfPairs; i++) {
				int idx = choices.testBit(i) ? 1 : 0;
				Utils.writeBigInteger(sBitslps[i][idx], bytelength, toPeer);
			}
			toPeer.flush();

			// Set the OT data.
			snder.execProtocol(cBitslps);
		} else {
			// Get the server side inputs (rightData + leftData).
			serverLabels = new BigInteger[otNumOfPairs];
			int bytelength = (globals.labelBitLength - 1) / 8 + 1;
			for (int i = 0; i < otNumOfPairs; i++) {
				serverLabels[i] = Utils.readBigInteger(bytelength, toPeer);
			}

//			clientLabels = new BigInteger[otNumOfPairs];
			// Set the OT choices (using our input bits).
			rcver.execProtocol(choices);
			// Get the data
			clientLabels = rcver.getData();
		}

	}

	/**
	 * Performs the steps involved in running the op.
	 * 
	 * @throws IOException
	 */
	protected void execute() throws IOException {
		execTransfer();

		execCircuit();

		interpretResult();
	}

	/**
	 * Builds pairs of server / client data (for filling input labels and doing OT).
	 */
	protected void generateLabelPairs() {

		sBitslps = new BigInteger[otNumOfPairs][2];
		cBitslps = new BigInteger[otNumOfPairs][2];

		for (int i = 0; i < otNumOfPairs; i++) {
			BigInteger glb0 = new BigInteger(globals.labelBitLength, rand);
			BigInteger glb1 = Wire.conjugate(globals.R, glb0);
			sBitslps[i][0] = glb0;
			sBitslps[i][1] = glb1;

			glb0 = new BigInteger(globals.labelBitLength, rand);
			glb1 = Wire.conjugate(globals.R, glb0);
			cBitslps[i][0] = glb0;
			cBitslps[i][1] = glb1;
		}

	}

	/**
	 * Run the op circuit. All op circuits are constructed of a single instance of a
	 * CompositeCircuit object.
	 */
	protected void execCircuit() throws IOException {

        ++execSerial;
		if (serverMode) {
			// Fill the server side inputs (leftData + rightData).
			serverLabels = new BigInteger[otNumOfPairs];
			clientLabels = new BigInteger[otNumOfPairs];

			for (int i = 0; i < sBitslps.length; i++)
				serverLabels[i] = sBitslps[i][0];

			for (int i = 0; i < cBitslps.length; i++)
				clientLabels[i] = cBitslps[i][0];
		}

		int serverBitCount = serverLabels.length;
		int clientBitCount = clientLabels.length;
		BigInteger[] lbs = new BigInteger[serverBitCount + clientBitCount];
		System.arraycopy(serverLabels, 0, lbs, 0, serverBitCount);
		System.arraycopy(clientLabels, 0, lbs, serverBitCount, clientBitCount);
		State in = State.fromLabels(lbs, execSerial);
		outState = ccs[0].startExecuting(in);
        if (outState.execSerial < execSerial)
            logger.error("Execution inputs have not percolated (current serial: {}, percolated serial: {})!",
                    execSerial, outState.execSerial);
	}

	/**
	 * Extracts the results from the output labels.
	 * 
	 * @throws IOException
	 */
	protected void interpretResult() throws IOException {

		if (serverMode) {
			// Reset decoded result.
			opOutput = BigInteger.ZERO;
			// Get the outState.toLabels() sent by the client.
			BigInteger[] outLabels = (BigInteger[]) toPeer.readObject(BigInteger[].class);
			// FOR DEBUG ONLY - rem when not needed.
			if (logger.isTraceEnabled()) {
				StringBuffer hexLabels = new StringBuffer("0x");
				hexLabels.append(outLabels[0].toString(16));
				for (int i = 1; i < outLabels.length; ++i) {
					hexLabels.append(", 0x").append(outLabels[i].toString(16));
				}
				logger.trace("Received garbled output labels ({}): {}", outLabels.length, hexLabels);
			}

			for (int i = 0; i < outLabels.length; i++) {
				if (outState.wires[i].value != Wire.UNKNOWN_SIG) {
					if (outState.wires[i].value == 1)
						opOutput = opOutput.setBit(i);
					continue;
				} else if (outLabels[i].equals(outState.wires[i].invd ? outState.wires[i].lbl
						: Wire.conjugate(globals.R, outState.wires[i].lbl))) {
					opOutput = opOutput.setBit(i);
				} else if (!outLabels[i].equals(outState.wires[i].invd ? Wire.conjugate(globals.R,
						outState.wires[i].lbl) : outState.wires[i].lbl)) {
					logger.error("Bad label encountered: (i={}; wire={}) 0x{} not in (0x{}, 0x{})",
							i, outState.wires[i].serialNum, outLabels[i].toString(16),
							outState.wires[i].lbl.toString(16),
							Wire.conjugate(globals.R, outState.wires[i].lbl).toString(16));
					throw new IOException(String.format(
							"Bad label encountered: (i=%d) 0x%s not in (0x%s, 0x%s)", i,
							outLabels[i].toString(16), outState.wires[i].lbl.toString(16), Wire
									.conjugate(globals.R, outState.wires[i].lbl).toString(16)));
				}
			}

		} else {
			//
			BigInteger[] outLabels = outState.toLabels();
			// FOR DEBUG ONLY - rem when not needed.
			if (logger.isTraceEnabled()) {
				StringBuffer buf = new StringBuffer().append(outState.wires[0].serialNum).append(
						":0x");
				buf.append(outLabels[0].toString(16));
				for (int i = 1; i < outLabels.length; ++i) {
					buf.append(", ").append(outState.wires[i].serialNum).append(":0x")
							.append(outLabels[i].toString(16));
				}
				logger.trace("Sending output labels ({}): {}", outLabels.length, buf);
			}
			// Write the client side result to the op output stream.
			toPeer.writeObject(outLabels);
			toPeer.flush();
		}
	}

	protected void createCircuits(boolean isForGarbling) throws IOException {
		for (int i = 0; i < ccs.length; i++) {
			ccs[i].build(isForGarbling);
		}
	}

}
