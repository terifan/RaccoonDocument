package org.terifan.raccoon.document;

import java.io.Serializable;
import static java.lang.Integer.parseUnsignedInt;
import static java.lang.Integer.rotateLeft;
import static java.lang.Long.rotateRight;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * ObjectIds are small, likely unique, fast to generate, and ordered. ObjectId values are 12 bytes in length, consisting of:
 * <ul>
 * <li>4-byte time stamp in seconds since midnight January 1, 1970 UTC</li>
 * <li>4-byte session id per JVM instance</li>
 * <li>4-byte incrementing counter, initialized to a random value</li>
 * </ul>
 */
public final class ObjectId implements Serializable, Comparable<ObjectId>
{
	private final static long serialVersionUID = 1;

	public final static int LENGTH = 12;

	private final static int L1 = 9;
	private final static int L2 = 13;
	private final static int L3 = 5;
	private final static int R1 = 7;
	private final static int R2 = 25;
	private final static int R3 = 10;

	private final int mTime;
	private final int mSession;
	private final int mSequence;


	// lazy initialization
	private static class Holder
	{
		final static SecureRandom PRNG = new SecureRandom();
		final static int SESSION = PRNG.nextInt();
		final static AtomicInteger SEQUENCE = new AtomicInteger(PRNG.nextInt());
		final static char[] BASE62ENC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
		final static int[] BASE62DEC = new int[128];


		static
		{
			for (int i = 0; i < 62; i++)
			{
				BASE62DEC[BASE62ENC[i]] = i;
			}
		}
	}


	private ObjectId(int aTime, int aSession, int aSequence)
	{
		mTime = aTime;
		mSession = aSession;
		mSequence = aSequence;
	}


	/**
	 * Get time part from the ObjectId
	 *
	 * @return the time in seconds
	 */
	public int timestamp()
	{
		return mTime;
	}


	/**
	 * Get the sequence counter from the ObjectId
	 *
	 * @return sequence counter
	 */
	public int sequence()
	{
		return mSequence;
	}


	/**
	 * Get the session ID from the ObjectId
	 *
	 * @return session ID
	 */
	public int session()
	{
		return mSession;
	}


	/**
	 * Create a random ObjectId
	 *
	 * @return a new ObjectId
	 */
	public static ObjectId randomId()
	{
		return new ObjectId((int)(System.currentTimeMillis() / 1000), Holder.SESSION, Holder.SEQUENCE.getAndIncrement());
	}


	/**
	 * Create a ObjectId from the three parts
	 *
	 * @param aTime time in seconds
	 * @param aSession session id
	 * @param aSequence sequence counter
	 * @return a new ObjectId
	 */
	public static ObjectId fromParts(int aTime, int aSession, int aSequence)
	{
		return new ObjectId(aTime, aSession, aSequence);
	}


	/**
	 * Create a ObjectId from a serialized instance
	 *
	 * @param aBuffer serialized instance
	 * @return a new ObjectId
	 */
	public static ObjectId fromByteArray(byte[] aBuffer)
	{
		if (aBuffer == null || aBuffer.length != LENGTH)
		{
			throw new IllegalArgumentException("data must be " + LENGTH + " bytes in length");
		}

		return new ObjectId(getInt32(aBuffer, 0), getInt32(aBuffer, 4), getInt32(aBuffer, 8));
	}


	/**
	 * Create a ObjectId from a hexadecimal serialized version
	 *
	 * @param aName hexadecimal serialized string
	 * @return a new ObjectId
	 */
	public static ObjectId fromString(String aName)
	{
		return new ObjectId(parseUnsignedInt(aName.substring(0, 8), 16), parseUnsignedInt(aName.substring(8, 16), 16), parseUnsignedInt(aName.substring(16, 24), 16));
	}


	/**
	 * Key used to encrypt an ObjectId.
	 */
	public static class Key
	{
		// random 32-bit values
		private final int[] value =
		{
			0x0A9F75ED, 0xB0399D91, 0x848542E3, 0x3B5E35EB, 0xC32B695A, 0xE0F51840, 0xBBAA11B9, 0xDBE7792E, 0x7A61E325
		};

		private final static int[] SHIFTS =
		{
			12, 52, 30, 3, 22, 43
		};


		/**
		 * Create a Key used for encrypting an ObjectId.
		 *
		 * @param aKey secret numbers used to initialize the key.
		 */
		public Key(long... aKey)
		{
			long sum = 0;

			for (long k : aKey)
			{
				for (int s : SHIFTS)
				{
					sum ^= rotateRight(k, s);

					for (int i = 0; i < value.length; i++)
					{
						sum = (sum * 0x5DEECE66DL + 0xBL) & 0x0000FFFFFFFFFFFFL;
						value[i] ^= (int)(sum >>> 16);
					}
				}
			}
		}
	}


	/**
	 * Return an encrypted String representation of this ObjectId in Base62. The encoded String also contains a checksum used for validation
	 * when decoding.
	 *
	 * @param aKey the Key used for encryption
	 * @return the ObjectId as an encrypted String representation
	 */
	public String toArmouredString(Key aKey)
	{
		int a = mTime;
		int b = mSession;
		int c = mSequence;

		a ^= aKey.value[0];
		b ^= aKey.value[1];
		c ^= aKey.value[2];
		for (int i = 0; i < 3; i++)
		{
			a -= b ^ rotateLeft(c, L1);
			b -= c ^ rotateLeft(a, L2);
			c -= a ^ rotateLeft(b, L3);
			a ^= aKey.value[3 + i];
			a += b ^ rotateLeft(c, R1);
			b += c ^ rotateLeft(a, R2);
			c += a ^ rotateLeft(b, R3);
		}
		a ^= aKey.value[6];
		b ^= aKey.value[7];
		c ^= aKey.value[8];

		int chk = ((31 + a) * 31 + b) * 31 + c;
		long A = (a & 0xFFFFFFFFL) * 13L + mod13(chk);
		long B = (b & 0xFFFFFFFFL) * 13L + mod13(chk / 13);
		long C = (c & 0xFFFFFFFFL) * 13L + mod13(chk / 13 / 13);

		char[] output = new char[18];
		encodeBase62(output, A, 0);
		encodeBase62(output, B, 6);
		encodeBase62(output, C, 12);

		return new String(output);
	}


	/**
	 * Return an ObjectId instance from an encrypted String representation
	 *
	 * @param aKey the Key used for encryption
	 * @param aName the encrypted String representation
	 * @return the decoded ObjectId or null if the decoded checksum is incorrect
	 */
	public static ObjectId fromArmouredString(Key aKey, String aName)
	{
		char[] buf = aName.toCharArray();
		long A = decodeBase62(buf, 0);
		long B = decodeBase62(buf, 6);
		long C = decodeBase62(buf, 12);

		int a = (int)(A / 13);
		int b = (int)(B / 13);
		int c = (int)(C / 13);

		int chk = ((31 + a) * 31 + b) * 31 + c;
		if ((A % 13) != mod13(chk) || ((B % 13) != mod13(chk / 13)) || ((C % 13) != mod13(chk / 13 / 13)))
		{
			return null;
		}

		a ^= aKey.value[6];
		b ^= aKey.value[7];
		c ^= aKey.value[8];
		for (int i = 0; i < 3; i++)
		{
			c -= a ^ rotateLeft(b, R3);
			b -= c ^ rotateLeft(a, R2);
			a -= b ^ rotateLeft(c, R1);
			a ^= aKey.value[5 - i];
			c += a ^ rotateLeft(b, L3);
			b += c ^ rotateLeft(a, L2);
			a += b ^ rotateLeft(c, L1);
		}
		a ^= aKey.value[0];
		b ^= aKey.value[1];
		c ^= aKey.value[2];

		return new ObjectId(a, b, c);
	}


	/**
	 * Encode the ObjectId as a Base62 (URL safe) string, always 18 bytes in length. The encoded String also contains a checksum used for
	 * validation when decoding.
	 *
	 * @return the ObjectId as an 18 character Base62 encoded String.
	 */
	public String toBase62String()
	{
		int a = mTime;
		int b = mSession;
		int c = mSequence;

		int chk = ((31 + a) * 31 + b) * 31 + c;
		long A = (a & 0xFFFFFFFFL) * 13L + mod13(chk);
		long B = (b & 0xFFFFFFFFL) * 13L + mod13(chk / 13);
		long C = (c & 0xFFFFFFFFL) * 13L + mod13(chk / 13 / 13);

		char[] output = new char[18];
		encodeBase62(output, A, 0);
		encodeBase62(output, B, 6);
		encodeBase62(output, C, 12);

		return new String(output);
	}


	/**
	 * Decode a Base62 encoded ObjectId.
	 *
	 * Simplified decoder including checksum test:
	 * <pre>
	 *	String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	 *	String text = "00000D00000Q00000d";
	 *	int[] p = new int[3];
	 *	int[] c = new int[3];
	 *	for (int i = 0; i &lt; 3; i++)
	 *	{
	 *		long v = 0;
	 *		for (int j = 0; j &lt; 6; j++)
	 *		{
	 *			v *= 62;
	 *			v += BASE62.indexOf(text.charAt(i * 6 + j));
	 *		}
	 *		c[i] = (int)(v % 13);
	 *		p[i] = (int)(v / 13);
	 *	}
	 *	int t = p[0] ^ p[1] ^ p[2];
	 *	if (((t) % 13) != c[0] || ((t / 13) % 13) != c[1] || ((t / 13 / 13) % 13) != c[2])
	 *	{
	 *		throw new IllegalStateException("checksum error");
	 *	}
	 *	System.out.println(p[0] + "," + p[1] + "," + p[2]);
	 * </pre>
	 *
	 * @return the decoded ObjectId or null if the decoded checksum is incorrect
	 */
	public static ObjectId fromBase62String(String aName)
	{
		if (aName == null || aName.length() != 18)
		{
			throw new IllegalArgumentException("Provided aName is null or wrong length: " + (aName == null ? "null" : aName.length()));
		}

		char[] buf = aName.toCharArray();
		long A = decodeBase62(buf, 0);
		long B = decodeBase62(buf, 6);
		long C = decodeBase62(buf, 12);

		int a = (int)(A / 13);
		int b = (int)(B / 13);
		int c = (int)(C / 13);

		int chk = ((31 + a) * 31 + b) * 31 + c;
		if ((A % 13) != mod13(chk) || ((B % 13) != mod13(chk / 13)) || ((C % 13) != mod13(chk / 13 / 13)))
		{
			return null;
		}

		return new ObjectId(a, b, c);
	}


	private static int mod13(int v)
	{
		return Math.abs(v % 13);
	}


	private static void encodeBase62(char[] aOutput, long aValue, int aIndex)
	{
		char[] base62 = Holder.BASE62ENC;
		for (int i = 6; --i >= 0;)
		{
			aOutput[aIndex + i] = base62[(int)(aValue % 62)];
			aValue /= 62;
		}
	}


	private static long decodeBase62(char[] aInput, int aIndex)
	{
		long value = 0;
		int[] base62 = Holder.BASE62DEC;
		for (int i = 0; i < 6; i++)
		{
			value *= 62;
			value += base62[aInput[aIndex + i] & 0x7F];
		}
		return value;
	}


	/**
	 * Serialize an ObjectId to a byte array (12 bytes)
	 *
	 * @return serialized byte array of this ObjectId
	 */
	public byte[] toByteArray()
	{
		byte[] buffer = new byte[LENGTH];
		putInt32(buffer, 0, mTime);
		putInt32(buffer, 4, mSession);
		putInt32(buffer, 8, mSequence);
		return buffer;
	}


	/**
	 * Return a hexadecimal string representation of this ObjectId
	 *
	 * @return hexadecimal string representation of this ObjectId
	 */
	@Override
	public String toString()
	{
		return String.format("%08x%08x%08x", mTime, mSession, mSequence);
	}


	/**
	 * @return a hashCode value for this ObjectId
	 */
	@Override
	public int hashCode()
	{
		return ((31 + mTime) * 31 + mSession) * 31 + mSequence;
	}


	/**
	 * @param aOther other object
	 * @return if the provided object is the same as this
	 */
	@Override
	public boolean equals(Object aOther)
	{
		if (aOther instanceof ObjectId v)
		{
			return (mTime == v.mTime && mSession == v.mSession && mSequence == v.mSequence);
		}
		return false;
	}


	/**
	 * Compare two ObjectId instances. Fields are compared in order: time, session and sequence
	 */
	@Override
	public int compareTo(ObjectId aOther)
	{
		return mTime < aOther.mTime ? -1
			: mTime > aOther.mTime ? 1
				: mSession < aOther.mSession ? -1
					: mSession > aOther.mSession ? 1
						: mSequence < aOther.mSequence ? -1
							: mSequence > aOther.mSequence ? 1 : 0;
	}


	private static int getInt32(byte[] aBuffer, int aPosition)
	{
		return ((aBuffer[aPosition] & 0xFF) << 24) + ((aBuffer[aPosition + 1] & 0xFF) << 16) + ((aBuffer[aPosition + 2] & 0xFF) << 8) + ((aBuffer[aPosition + 3] & 0xFF));
	}


	private static void putInt32(byte[] aBuffer, int aPosition, int aValue)
	{
		aBuffer[aPosition] = (byte)(aValue >>> 24);
		aBuffer[aPosition + 1] = (byte)(aValue >> 16);
		aBuffer[aPosition + 2] = (byte)(aValue >> 8);
		aBuffer[aPosition + 3] = (byte)(aValue);
	}


	/**
	 * Compare two ObjectId instances. Fields are compared in order: time, session and sequence
	 */
	public static int compare(byte[] aBuffer1, int aOffset1, byte[] aBuffer2, int aOffset2)
	{
		return getInt32(aBuffer1, aOffset1) < getInt32(aBuffer2, aOffset2) ? -1
			: getInt32(aBuffer1, aOffset1) > getInt32(aBuffer2, aOffset2) ? 1
			: getInt32(aBuffer1, aOffset1 + 4) < getInt32(aBuffer2, aOffset2 + 4) ? -1
			: getInt32(aBuffer1, aOffset1 + 4) > getInt32(aBuffer2, aOffset2 + 4) ? 1
			: getInt32(aBuffer1, aOffset1 + 8) < getInt32(aBuffer2, aOffset2 + 8) ? -1
			: getInt32(aBuffer1, aOffset1 + 8) > getInt32(aBuffer2, aOffset2 + 8) ? 1
			: 0;
	}
}
