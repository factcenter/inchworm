package test;

import org.factcenter.inchworm.MemoryArea;
import org.factcenter.inchworm.app.DefaultIOHandler;
import org.factcenter.inchworm.app.InchwormIO;
import org.factcenter.inchworm.app.TwoPcApplication;
import org.factcenter.qilin.util.BitMatrix;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class TestCommon {
	protected final Logger logger = LoggerFactory.getLogger(getClass());


	public static final String DEFAULT_OUTPUT_FORMAT = "Computation Result (Program counter = %05d) =  %d\n";
	
	public static class CodeDescription {
		InputStream leftCode;
		InputStream rightCode;
		InputStream leftData;
		InputStream rightData;
		InputStream referenceResult;

		public CodeDescription(InputStream leftCode, InputStream rightCode,
				InputStream leftData, InputStream rightData, InputStream referenceResult) {
			this.leftCode = leftCode;
			this.rightCode = rightCode;
			this.leftData = leftData;
			this.rightData = rightData;
			this.referenceResult = referenceResult;
		}
		

		public CodeDescription(String leftCode, String rightCode,
				String leftData, String rightData, String referenceResult) {
			this.leftCode = leftCode != null ? getClass().getResourceAsStream(leftCode) : null;
			this.rightCode = rightCode != null ? getClass().getResourceAsStream(rightCode) : null;
			this.leftData = leftData != null ? getClass().getResourceAsStream(leftData) : null;
			this.rightData = rightData != null ? getClass().getResourceAsStream(rightData) : null;
			this.referenceResult = referenceResult != null ? getClass().getResourceAsStream(referenceResult) : null;
		}
	}
	
	/**
	 * Return a stream containing the left player's code share. 
	 */
	protected abstract CodeDescription getCodeDescription();

	protected boolean useSecureOps() { return true; }

    protected boolean useZeroRand() { return false; }
	
	protected boolean usePathORAM() { return false; }
	
	/**
	 * Initialize RAM.
	 */
	protected BitMatrix getRAMContents() { return null; }
	
	protected int getMaxSteps() { return -1; }
	
	protected void justBeforeRunning() throws Exception { }
	
	protected void justAfterRunning() throws Exception { }
	
	InchwormIO ioLeft;
	
	
	
	protected File outFile;
	protected CodeDescription desc;
	protected OutputStream outStream = null;
	protected TwoPcApplication tpc;

	protected InchwormIO getLeftIO() throws IOException {
		outStream = new FileOutputStream(outFile);
		return new DefaultIOHandler(outStream, DEFAULT_OUTPUT_FORMAT);
	}
	
	protected InchwormIO getRightIO() throws IOException {
		return new DefaultIOHandler(null, "");
	}

	
	@Before
	public void setUp() throws IOException {
		outFile = File.createTempFile(getClass().getSimpleName() + "-output-", ".txt");
		desc = getCodeDescription();
		
		assertTrue("At least one code resource must be given!",  (desc.leftCode != null) || (desc.rightCode != null));
	}
	
	@Test
	public void testExecution() throws Exception {

		FileOutputStream outStream = new FileOutputStream(outFile);
		
		// Create two players.
		tpc = new TwoPcApplication(useSecureOps(), useZeroRand(), usePathORAM(), desc.leftCode, desc.leftData, desc.rightCode, desc.rightData,
				getLeftIO(), getRightIO(), getMaxSteps());
		// Run the secure computation.
		tpc.init();
		
		BitMatrix ramContents = getRAMContents();
		
		if (ramContents != null) {
			tpc.getLeftPlayer().getState().getMemory(MemoryArea.Type.TYPE_RAM).store(0, ramContents);
		}

		justBeforeRunning();
		
		tpc.run2PC();
		
		justAfterRunning();
		
		outStream.close();

		
		verifyResults();
	}

	/**
	 * Determines if the contents of two files are equal.
	 * 
	 */
	protected void verifyResults() throws IOException {
		FileInputStream testResults = new FileInputStream(outFile);
		compareStreams(desc.referenceResult, testResults);
		testResults.close();
	}

	/**
	 * Determines if the contents of two input streams are equal.
	 * 
	 * @param refFileStream
	 * @param newFileStream
	 * @return true if the content of the streams are equal, false otherwise.
	 * @throws IOException
	 */
	private boolean compareStreams(InputStream refFileStream,
			InputStream newFileStream) throws IOException {

		// Buffer the two input streams.
		if (!(refFileStream instanceof BufferedInputStream)) {
			refFileStream = new BufferedInputStream(refFileStream);
		}
		if (!(newFileStream instanceof BufferedInputStream)) {
			newFileStream = new BufferedInputStream(newFileStream);
		}
        int lineNum = 1;
        int charNum = 1;
		// Loop and compare streams byte after byte.
		int refChar = refFileStream.read();
		int newChar;
		while (refChar != -1) {
			if (refChar == '\n')
			{
				lineNum++;
				charNum = 1;
			}
			newChar = newFileStream.read();
			assertEquals("Result comparison error - mismatch found in line " + lineNum + ", column " + charNum,
					refChar, newChar); 
			refChar = refFileStream.read();
			charNum++;
		}
		// Check the last byte.
		newChar = newFileStream.read();
		return (newChar == -1);
	}

}
