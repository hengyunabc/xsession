package io.github.xsession.util;

import java.security.SecureRandom;

/**
 * part from tomcat source code.
 * 
 * @author hengyunabc
 * 
 */
public class SessionIdGenerator {

	/*
	 * The random number generator used by this class to create random based
	 * UUIDs. In a holder class to defer initialization until needed.
	 */
	private static class Holder {
		static final SecureRandom random = new SecureRandom();
	}

	private static void getRandomBytes(byte bytes[]) {
		SecureRandom random = Holder.random;
		random.nextBytes(bytes);
	}

	/**
	 * Generate and return a new session identifier.
	 */
	public static String generateSessionId(int sessionIdLength) {

		byte random[] = new byte[16];

		// Render the result as a String of hexadecimal digits
		StringBuilder buffer = new StringBuilder();

		int resultLenBytes = 0;

		while (resultLenBytes < sessionIdLength) {
			getRandomBytes(random);
			for (int j = 0; j < random.length && resultLenBytes < sessionIdLength; j++) {
				byte b1 = (byte) ((random[j] & 0xf0) >> 4);
				byte b2 = (byte) (random[j] & 0x0f);
				if (b1 < 10)
					buffer.append((char) ('0' + b1));
				else
					buffer.append((char) ('A' + (b1 - 10)));
				if (b2 < 10)
					buffer.append((char) ('0' + b2));
				else
					buffer.append((char) ('A' + (b2 - 10)));
				resultLenBytes++;
			}
		}

		return buffer.toString();
	}
}
