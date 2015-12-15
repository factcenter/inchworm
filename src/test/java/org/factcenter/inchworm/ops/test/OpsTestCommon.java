package org.factcenter.inchworm.ops.test;

import org.factcenter.inchworm.Player;
import org.factcenter.inchworm.app.TwoPcApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public abstract class OpsTestCommon {

	final Logger logger = LoggerFactory.getLogger(getClass());
	

	/**
	 * Source and data streams.
	 */
	ByteArrayInputStream srcFile;
	ByteArrayInputStream dataFileLeft;
	ByteArrayInputStream dataFileRight;
	
	/**
	 * Left + right players.
	 */
	Player lp;
	Player rp;


    /**
     * Override this to change default secure ops to false
     * @return
     */
    public boolean getUseSecureOps() { return false; }


    /**
     * Override this to change default zero randomness to true
     * @return
     */
    public boolean getUseZeroRand() { return false; }

    /**
	 * Executes the passed testCode inside the Inchworm VM.
	 * @param useSecureOps - flag for setting the set of ops.
	 * @param testCode assembly source code to run (left player only).
	 * @param dataLeft data file to run.
	 * @param dataRight data file to run.
	 * @throws IOException
	 */
	public void runTest(boolean useSecureOps, boolean useZeroRand, String testCode, String dataLeft,
			String dataRight) throws IOException {

		// Create input streams.
		srcFile = new ByteArrayInputStream(testCode.getBytes());
		dataFileLeft = new ByteArrayInputStream(dataLeft.getBytes());
		dataFileRight = new ByteArrayInputStream(dataRight.getBytes());

		// Create two players.
		TwoPcApplication tpc = new TwoPcApplication(useSecureOps, useZeroRand, srcFile, dataFileLeft,
				dataFileRight, null, null);

		// Run the secure computation.
		tpc.run2PC();

		// Get the players.
		lp = tpc.getLeftPlayer();
		rp = tpc.getRightPlayer();
	}

}
