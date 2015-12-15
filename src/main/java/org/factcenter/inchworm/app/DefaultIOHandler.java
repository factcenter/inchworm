package org.factcenter.inchworm.app;

import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Debug implementation for the InchwormIO interface that formats output in a human-readable way 
 * 
 */
public class DefaultIOHandler implements InchwormIO {

	/**
	 * Default output stream for the result of the secure computation.
	 */
	OutputStream outStream;
	
	String outFormat;

	/**
	 * Constructs a new {@code DefaultIOHandler} object with a default output stream.
	 * @param outStream this is the stream to which outputs will be sent.
	 * @param outFormat - The output will be formatted using String.format(outFormat, pc, value), where pc and value
	 *  are the parameters passed to {@link InchwormIO#output(long, long, BitMatrix)}.
	 */
	public DefaultIOHandler(OutputStream outStream, String outFormat) {
		this.outStream = outStream;
		this.outFormat = outFormat;
	}

	/**
	 * Output the secure computation result to the specified stream.
	 */
	@Override
	public void output(long stream, long pc, BitMatrix value) throws IOException {
		if (outStream == null)
			return;
		if (stream == 1){
			String temp = String.format(outFormat,	pc, value.toInteger(64), value.toInteger(64));
			outStream.write(temp.getBytes());
		}
		
	}

	/**
	 * Input. Subclass and override to give meaningful input.
	 */
	@Override
	public BitMatrix input(long pc) throws IOException {
		return BitMatrix.valueOf(0, 64);
	}

}
