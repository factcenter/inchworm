package org.factcenter.inchworm.app;

import org.factcenter.qilin.util.BitMatrix;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class InputStreamIOHandler extends DefaultIOHandler {
	InputStream in;
	boolean closed;
	
	public InputStreamIOHandler(InputStream in, OutputStream outStream, String outFormat) {
		super(outStream, outFormat);
		this.in = in;
		closed = false;
	}
	
	@Override 
	public BitMatrix input(long pc) throws IOException {
		if (closed)
			return BitMatrix.valueOf(0, 1);
		long val = in.read();
		if (val < 0) {
			closed = true;
            return BitMatrix.valueOf(0, 1);
		}
		return BitMatrix.valueOf(val, 64);
	}
}
