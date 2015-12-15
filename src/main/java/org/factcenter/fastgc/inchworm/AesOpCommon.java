package org.factcenter.fastgc.inchworm;

import org.factcenter.fastgc.Utils.Utils;
import org.factcenter.fastgc.YaoGC.AESComponents.AddRoundKey;
import org.factcenter.fastgc.YaoGC.AESComponents.MixColumns;
import org.factcenter.fastgc.YaoGC.AESComponents.SBox;
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
 * Abstract class for using both the client and server versions of the AES
 * op inside an Inchworm VM. Client side has the text to encrypt, server side
 * has the encryption key.
 */
public abstract class AesOpCommon {

	final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Flag indicating client or server mode of operation.
	 */
	protected boolean serverMode;

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
	 * Number of OT pairs needed for the op inputs.
	 */
	protected int otNumOfPairs;

	/**
	 * Array containing the circuits implementing the op.
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
     * Serial number of execution.
     * Incremented before every execution.
     */
    protected int execSerial = 0;

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
	protected int otMsgBitLength;

	/**
	 * Label pairs (client and server).
	 */
	protected BigInteger[][] sBitslps, cBitslps;
	
	/**
	 * AES processing circuits.
	 */
	protected AddRoundKey ccARK;
	protected SBox ccSBX;
	protected MixColumns ccMXC;
	
	/**
	 * Number of columns (32-bit words) comprising the AES State.
	 */
	final static int Nb = 4;

	/**
	 * Number of 32-bit words comprising the Cipher Key (Nk = 4, 6, or 8).
     * (used in AES-128, AES-192 and AES-256, respectively)
	 */
	protected int Nk;

	/**
	 * Number of rounds (a function of Nk and Nb, for this standard, Nr = 10, 12, or 14).
	 */
	protected int Nr;

	/**
	 * The expanded AES key (server side).
     * (we use short rather than byte to prevent sign issues.)
	 */
	protected short[] keyExp;

	/**
	 * The message to encrypt (client side, length = 128 bits)
	 */
	// protected short[] msg;
    protected BitMatrix msg;

	/**
	 * Must call this before calling {@link #init()} (or using).
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
	 * @param serverOp
	 *            - true if server, false otherwise.
	 * @param rand
	 *            - a secure random number generator.
	 * @param labelBitLength
	 *            - Length in bits of a single label.
     * @param Nk
	 *            - Number of 32-bit words comprising the Cipher Key (Nk = 4, 6, or 8).
	 */
	public AesOpCommon(boolean serverOp, Random rand, int labelBitLength, int Nk) {
		
		this.serverMode = serverOp;
		this.rand = rand;
		otMsgBitLength = labelBitLength;
		globals = new CircuitGlobals(rand, labelBitLength);
		
		// Create the AES sub-circuits.
		ccs = new Circuit[3];
		ccs[0] = ccARK = new AddRoundKey(globals);
		ccs[1] = ccSBX = new SBox(globals);
		ccs[2] = ccMXC = new MixColumns(globals);
		
		// Allocate the key expansion array.
		this.Nk = Nk;
		Nr = Nk + 6;
		keyExp = new short[4 * Nb * (Nr + 1)];
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
	 * Method for running the AES op (client and server).
	 * 
	 * @throws IOException
	 */
	public void run() throws IOException {
		
		execTransfer();
		execCircuit();
		interpretResult();

	}
	
	/**
	 * Server sends inputs data to the client side, client fills the input
	 * labels using an OT protocol.
	 * @throws IOException
	 */
	protected void execTransfer() throws IOException {

		if (serverMode) {
			/*-
			 * Send server inputs data to the client side (fills the client side labels).
			 */
			int bytelength = (globals.labelBitLength - 1) / 8 + 1;
	        for (int i = 0; i < sBitslps.length; i++) {
				int idx = testBit(keyExp, i);
				Utils.writeBigInteger(sBitslps[i][idx], bytelength, toPeer);
			}
			toPeer.flush();

			// Set the OT data.
			snder.execProtocol(cBitslps);
		
		} else {
			// Get the server side inputs.
			serverLabels = new BigInteger[(Nr + 1) * 128];
			int bytelength = (globals.labelBitLength - 1) / 8 + 1;
			for (int i = 0; i < serverLabels.length; i++) {
				serverLabels[i] = Utils.readBigInteger(bytelength, toPeer);
			}

			// Compose the OT choices from the client message.
			BigInteger choices = msg.toBigInteger();

			// Run the OT (using our input message bits).
			rcver.execProtocol(choices);
			// Get the data
			clientLabels = rcver.getData();

		}

	}
	
	/**
	 * Builds pairs of server / client data (for filling input labels and doing OT).
	 */
	protected void generateLabelPairs() {

		sBitslps = new BigInteger[(Nr + 1) * 128][2];
		cBitslps = new BigInteger[Nb * 32][2];

		for (int i = 0; i < sBitslps.length; i++) {
			BigInteger glb0 = new BigInteger(globals.labelBitLength, rand);
			BigInteger glb1 = Wire.conjugate(globals.R, glb0);			
			sBitslps[i][0] = glb0;
			sBitslps[i][1] = glb1;
		}

		for (int i = 0; i < cBitslps.length; i++) {
			BigInteger glb0 = new BigInteger(globals.labelBitLength, rand);
			BigInteger glb1 = Wire.conjugate(globals.R, glb0);			
			cBitslps[i][0] = glb0;
			cBitslps[i][1] = glb1;
		}		
	
	}

	/**
	 * Run the op circuit. Unlike the other op circuits (that are constructed of a single instance of a
	 * CompositeCircuit object) the AES op has a special Cipher method that runs the three processing
	 * circuits several times.
	 */
	protected void execCircuit() throws IOException {

        ++execSerial;
		if (serverMode) {

			// Fill the server side inputs (leftData + rightData).			
			serverLabels = new BigInteger[(Nr + 1) * 128];
			clientLabels = new BigInteger[Nb * 32];

			for (int i = 0; i < serverLabels.length; i++)
				serverLabels[i] = sBitslps[i][0];

			for (int i = 0; i < clientLabels.length; i++)
				clientLabels[i] = cBitslps[i][0];
		}
		// Run the Cipher.
		outState = Cipher(State.fromLabels(serverLabels, execSerial), State.fromLabels(clientLabels, execSerial));

	}

	/**
	 * Extracts the results from the output labels.
	 * 
	 * @throws IOException
	 */
	protected void interpretResult() throws IOException {

		if (serverMode) {
			// Get the outState.toLabels() sent by the client.
			opOutput = BigInteger.ZERO;
			BigInteger[] outLabels = (BigInteger[]) toPeer.readObject(BigInteger[].class);

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
			// Write the client side result to the op output stream.
			BigInteger[] outLabels = outState.toLabels();
			toPeer.writeObject(outLabels);
			toPeer.flush();
		}
	}

	/**
	 * Recursively create the circuits and sub-circuits implementing the AES op.
	 * @param isForGarbling
	 *                     - True for a server instance (servers create the garbled circuits, clients evaluate them)
	 * @throws IOException
	 */
	protected void createCircuits(boolean isForGarbling) throws IOException {
		for (int i = 0; i < ccs.length; i++) {
			ccs[i].build(isForGarbling);
		}
	}	
	
	/**
	 * Performs the required initialization for setting the server key.
	 * @param key
	 */
	protected void initServer(BitMatrix key) {
	    // length of expanded key depends on Nr.
		keyExpansion(key);
	}
	
	
	/*-
	 * ----------------------------------------------------------------
	 *                      Private Methods 
	 * ----------------------------------------------------------------
	 */
	
	/**
	 * Cipher: The actual AES encryption
	 * @param key - the AES key
	 * @param msg - the text to encrypt.
	 * @return a State containing the encrypted text.
	 */
	private State Cipher(State key, State msg) {
		State[] arrS;

		State state = AddRoundKey(key, msg, 0);
		for (int round = 1; round < Nr; round++) {
			arrS = SubBytes(state);
			arrS = ShiftRows(arrS);
			state = MixColumns(arrS);
			state = AddRoundKey(key, state, round);
		}

		arrS = SubBytes(state);
		arrS = ShiftRows(arrS);
		state = AddRoundKey(key, arrS, Nr);

		return state;
	}	
	
	/**
	 * KeyExpansion. This is the AES key schedule.
     * The key should be 128/192/256 bits long (corresponding to Nk*32)
	 */
	private void keyExpansion(BitMatrix key) {

        assert(key.getNumCols() == Nk * 32);
		short[] temp = new short[4];


		// first just copy key to w
		int j = 0;
		while (j < 4 * Nk) {
			keyExp[j] = (short) key.getBits(j * 8, 8);
		}

		// here j == 4*Nk;
		int i;
		while (j < 4 * Nb * (Nr + 1)) {
			i = j / 4; // j is always multiple of 4 here

			// handle everything word-at-a time, 4 bytes at a time
			for (int iTemp = 0; iTemp < 4; iTemp++)
				temp[iTemp] = keyExp[j - 4 + iTemp];
			if (i % Nk == 0) {
				short ttemp, tRcon;
				short oldtemp0 = temp[0];
				for (int iTemp = 0; iTemp < 4; iTemp++) {
					if (iTemp == 3)
						ttemp = oldtemp0;
					else
						ttemp = temp[iTemp + 1];
					if (iTemp == 0)
						tRcon = Rcon[i / Nk - 1];
					else
						tRcon = 0;
					temp[iTemp] = (short) (SBox[ttemp & 0xff] ^ tRcon);
				}
			} else if (Nk > 6 && (i % Nk) == 4) {
				for (int iTemp = 0; iTemp < 4; iTemp++)
					temp[iTemp] = SBox[temp[iTemp] & 0xff];
			}
			for (int iTemp = 0; iTemp < 4; iTemp++)
				keyExp[j + iTemp] = (short) (keyExp[j - 4 * Nk + iTemp] ^ temp[iTemp]);
			j = j + 4;
		}
	}
	
	// SubBytes: The substitute byte transformation.
	private State[] SubBytes(State state) {
		State[] res = new State[16];
		for (int i = 0; i < 16; i++) {
			res[i] = ccSBX.startExecuting(State.extractState(state, i * 8, i * 8 + 8));
		}
		return res;
	}

	// ShiftRows: simple circular shift of rows 1, 2, 3 by 1, 2, 3
	private State[] ShiftRows(State[] state) {
		State[] res = new State[16];
		short[] c = new short[] { 0, 5, 10, 15, 4, 9, 14, 3, 8, 13, 2, 7, 12, 1, 6, 11 };
		for (int i = 0; i < 16; i++) {
			res[i] = state[c[i]];
		}

		return res;
	}

	// MixColumns: complex and sophisticated mixing of columns.
	private State MixColumns(State[] s) {
		return ccMXC.startExecuting(s);
	}

	// AddRoundKey: xor a portion of expanded key with state
	private State AddRoundKey(State key, State state, int round) {
		return ccARK.startExecuting(key, round * 128, state);
	}

	private State AddRoundKey(State key, State[] arrS, int round) {
		return ccARK.startExecuting(key, round * 128, State.flattenStateArray(arrS));
	}

	private int testBit(short[] w, int n) {
		int i = n / 8;
		int j = n % 8;

		int res = ((w[i] & (1 << j)) == 0) ? 0 : 1;
		return res;
	}	
	
	
	/**
	 * SubBytes table
	 */
	private final short[] SBox = { 0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f, 0xc5, 0x30, 0x01,
			0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76, 0xca, 0x82, 0xc9, 0x7d, 0xfa, 0x59, 0x47, 0xf0,
			0xad, 0xd4, 0xa2, 0xaf, 0x9c, 0xa4, 0x72, 0xc0, 0xb7, 0xfd, 0x93, 0x26, 0x36, 0x3f,
			0xf7, 0xcc, 0x34, 0xa5, 0xe5, 0xf1, 0x71, 0xd8, 0x31, 0x15, 0x04, 0xc7, 0x23, 0xc3,
			0x18, 0x96, 0x05, 0x9a, 0x07, 0x12, 0x80, 0xe2, 0xeb, 0x27, 0xb2, 0x75, 0x09, 0x83,
			0x2c, 0x1a, 0x1b, 0x6e, 0x5a, 0xa0, 0x52, 0x3b, 0xd6, 0xb3, 0x29, 0xe3, 0x2f, 0x84,
			0x53, 0xd1, 0x00, 0xed, 0x20, 0xfc, 0xb1, 0x5b, 0x6a, 0xcb, 0xbe, 0x39, 0x4a, 0x4c,
			0x58, 0xcf, 0xd0, 0xef, 0xaa, 0xfb, 0x43, 0x4d, 0x33, 0x85, 0x45, 0xf9, 0x02, 0x7f,
			0x50, 0x3c, 0x9f, 0xa8, 0x51, 0xa3, 0x40, 0x8f, 0x92, 0x9d, 0x38, 0xf5, 0xbc, 0xb6,
			0xda, 0x21, 0x10, 0xff, 0xf3, 0xd2, 0xcd, 0x0c, 0x13, 0xec, 0x5f, 0x97, 0x44, 0x17,
			0xc4, 0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73, 0x60, 0x81, 0x4f, 0xdc, 0x22, 0x2a,
			0x90, 0x88, 0x46, 0xee, 0xb8, 0x14, 0xde, 0x5e, 0x0b, 0xdb, 0xe0, 0x32, 0x3a, 0x0a,
			0x49, 0x06, 0x24, 0x5c, 0xc2, 0xd3, 0xac, 0x62, 0x91, 0x95, 0xe4, 0x79, 0xe7, 0xc8,
			0x37, 0x6d, 0x8d, 0xd5, 0x4e, 0xa9, 0x6c, 0x56, 0xf4, 0xea, 0x65, 0x7a, 0xae, 0x08,
			0xba, 0x78, 0x25, 0x2e, 0x1c, 0xa6, 0xb4, 0xc6, 0xe8, 0xdd, 0x74, 0x1f, 0x4b, 0xbd,
			0x8b, 0x8a, 0x70, 0x3e, 0xb5, 0x66, 0x48, 0x03, 0xf6, 0x0e, 0x61, 0x35, 0x57, 0xb9,
			0x86, 0xc1, 0x1d, 0x9e, 0xe1, 0xf8, 0x98, 0x11, 0x69, 0xd9, 0x8e, 0x94, 0x9b, 0x1e,
			0x87, 0xe9, 0xce, 0x55, 0x28, 0xdf, 0x8c, 0xa1, 0x89, 0x0d, 0xbf, 0xe6, 0x42, 0x68,
			0x41, 0x99, 0x2d, 0x0f, 0xb0, 0x54, 0xbb, 0x16 };

	private final short[] Rcon = { 1, 2, 4, 8, 16, 32, 64, 128, 27, 54, 108, 216, 171, 77, 154,
			47, 94, 188, 99, 198, 151, 53, 106, 212, 179, 125, 250, 239, 197, 145 };
	
}
