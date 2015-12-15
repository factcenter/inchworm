package org.factcenter.fastgc.inchworm;

import org.factcenter.qilin.util.BitMatrix;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import test.categories.Integration;
import test.categories.Slow;

import java.math.BigInteger;
import java.util.concurrent.Future;

import static org.junit.Assert.assertArrayEquals;

@Category({Slow.class, Integration.class})
public class AesOpTest extends AesOpTestCommon {

	@Test
	public void test() throws Exception {
		
		// Init the key and data arrays (both 128 bits).
		byte[] key = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b,
				0x0c, 0x0d, 0x0e, 0x0f };
		byte[] data = { 0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99, (byte) 0xaa, (byte) 0xbb,
                (byte) 0xcc, (byte) 0xdd, (byte) 0xee, (byte) 0xff };

		// The expected encrypted text.
		int[] expected = { 0x69, 0xc4, 0xe0, 0xd8, 0x6a, 0x7b, 0x04, 0x30, 0xd8, 0xcd, 0xb7, 0x80,
				0x70, 0xb4, 0xc5, 0x5a };

		// Set the ops data.
		aesServer.setData(new BitMatrix(key, 0));
		aesClient.setData(new BitMatrix(data, 0));
		
		// Run the ops.
		Future<BigInteger> clientThread = runClient();
		BigInteger result = runServer();
		clientThread.get();
		
		// Check results.
		int[] resultArr = new int[16];
		BigInteger mask = BigInteger.valueOf(255);
		for (int i = 0; i < 16; i++) {
			int temp = result.shiftRight(i * 8).and(mask).intValue();
			System.out.print(Integer.toString(temp, 16) + " ");
			resultArr[i] = temp;
		}
		System.out.println();		
		assertArrayEquals("", expected, resultArr);
	
	}

}
