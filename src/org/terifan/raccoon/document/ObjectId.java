package org.terifan.raccoon.document;

import java.io.Serializable;
import static java.lang.Integer.parseUnsignedInt;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


/*
 *   0                   1                   2                   3
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                              time                             |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                             session                           |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                            sequence                           |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 * time      - 32 bits - time in seconds since midnight January 1, 1970 UTC
 * session   - 32 bits - random value used in all intances per JVM instance
 * sequence  - 32 bits - incrementing counter, initialized to a random value
 */
public final class ObjectId implements Serializable, Comparable<ObjectId>
{
	private final static long serialVersionUID = 1;

	public final static int LENGTH = 12;

	private final static long F1 = 5712613008489222801L;
	private final static long F2 = 25214903917L;
	private final static long F3 = 281474976710655L;
	private final static int F4 = 1;
	private final static int F5 = 83;
	private final static int F6 = 7349;
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
		final static char[] BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
	}


	private ObjectId(int aTime, int aSession, int aSequence)
	{
		mTime = aTime;
		mSession = aSession;
		mSequence = aSequence;
	}


	public long time()
	{
		return mTime * 1000L;
	}


	public int sequence()
	{
		return mSequence;
	}


	public int session()
	{
		return mSession;
	}


	public static ObjectId randomId()
	{
		return new ObjectId((int)(System.currentTimeMillis() / 1000), Holder.SESSION, Holder.SEQUENCE.getAndIncrement());
	}


	public static ObjectId fromParts(int aTime, int aSession, int aSequence)
	{
		return new ObjectId(aTime, aSession, aSequence);
	}


	public static ObjectId fromBytes(byte[] aBuffer)
	{
		if (aBuffer == null || aBuffer.length != LENGTH)
		{
			throw new IllegalArgumentException("data must be " + LENGTH + " bytes in length");
		}

		return new ObjectId(getInt32(aBuffer, 0), getInt32(aBuffer, 4), getInt32(aBuffer, 8));
	}


	public static ObjectId fromString(String aName)
	{
		return new ObjectId(parseUnsignedInt(aName.substring(0, 8), 16), parseUnsignedInt(aName.substring(8, 16), 16), parseUnsignedInt(aName.substring(16, 24), 16));
	}


	/**
	 * Key used to encrypt an ObjectId.
	 */
	public static class Key
	{
		final char[][] toBase62 = new char[18][62];
		final int[][] fromBase62 = new int[18][128];
		final int[][] tweak = new int[2][3];
		final int[][] chk = new int[3][13];


		/**
		 *
		 * @param aSecretKey a 256-bit random value (eight ints) used to initialize the key.
		 */
		public Key(byte[] aSecretKey)
		{
			if (aSecretKey == null || aSecretKey.length != 32)
			{
				throw new IllegalArgumentException("Bad secret key, must be 8 ints.");
			}

			Random rnd = new Random((getInt32L(aSecretKey, 0) << 32) | getInt32L(aSecretKey, 4));
			for (int i = 0, k = 8; i < 2; i++)
			{
				for (int j = 0; j < 3; j++, k += 4)
				{
					tweak[i][j] = rnd.nextInt() ^ getInt32(aSecretKey, k);
				}
			}
			for (int k = 0; k < 18; k++)
			{
				int[] order = shuffle(62, rnd);
				for (int i = 0; i < 62;)
				{
					int j = order[i];
					fromBase62[k][Holder.BASE62[j]] = i;
					toBase62[k][i++] = Holder.BASE62[j];
				}
			}
			for (int i = 0; i < 3; i++)
			{
				chk[i] = shuffle(13, rnd);
			}
		}
	}


	private static int[] shuffle(int aLength, Random aRandom)
	{
		int[] order = new int[aLength];
		for (int i = 0; i < aLength; i++)
		{
			order[i] = i;
		}
		for (int i = 0; i < aLength; i++)
		{
			int j = aRandom.nextInt(aLength);
			int t = order[j];
			order[j] = order[i];
			order[i] = t;
		}
		return order;
	}


	/**
	 * Return an encrypted String representation of this ObjectId
	 *
	 * @param aKey the Key used to encrypt the ObjectId
	 * @return the ObjectId as an encrypted 18 character Base62 encoded String. The encoded String also contains a checksum used for
	 * validation when decoding.
	 */
	public String toArmouredString(Key aKey)
	{
		int a = mTime;
		int b = mSession;
		int c = mSequence;

		a ^= aKey.tweak[0][0];
		b ^= aKey.tweak[0][1];
		c ^= aKey.tweak[0][2];
		for (int i = 0; i < 3; i++)
		{
			c -= Integer.rotateLeft(a, L1) ^ Integer.rotateRight(b, R1);
			b -= Integer.rotateLeft(c, L2) ^ Integer.rotateRight(a, R2);
			a -= Integer.rotateLeft(b, L3) ^ Integer.rotateRight(c, R3);
		}
		a ^= aKey.tweak[1][0];
		b ^= aKey.tweak[1][1];
		c ^= aKey.tweak[1][2];

		long chk = 0xffffffffL & (a * F1 + b * F2 + c * F3);
		long c0 = aKey.chk[0][Math.abs((int)(chk / F4 % 13))];
		long c1 = aKey.chk[1][Math.abs((int)(chk / F5 % 13))];
		long c2 = aKey.chk[2][Math.abs((int)(chk / F6 % 13))];

		long A = (a & 0xffffffffL) + (c0 << 32);
		long B = (b & 0xffffffffL) + (c1 << 32);
		long C = (c & 0xffffffffL) + (c2 << 32);

		char[] buf = new char[18];
		encodeBase62(aKey, buf, A, 0);
		encodeBase62(aKey, buf, B, 1);
		encodeBase62(aKey, buf, C, 2);
		return new String(buf);
	}


	/**
	 * Return an ObjectId from an encrypted String representation
	 *
	 * @param aKey the Key used to encrypt the ObjectId
	 * @param aName the encrypted String representation of an ObjectId
	 * @return the decoded ObjectId or null if the String provided is invalid
	 */
	public static ObjectId fromArmouredString(Key aKey, String aName)
	{
		long A = decodeBase62(aKey, aName, 0);
		long B = decodeBase62(aKey, aName, 1);
		long C = decodeBase62(aKey, aName, 2);

		int a = (int)A;
		int b = (int)B;
		int c = (int)C;

		long chk = 0xffffffffL & (a * F1 + b * F2 + c * F3);
		long c0 = aKey.chk[0][Math.abs((int)(chk / F4 % 13))];
		long c1 = aKey.chk[1][Math.abs((int)(chk / F5 % 13))];
		long c2 = aKey.chk[2][Math.abs((int)(chk / F6 % 13))];
		if ((A >> 32) != c0 || (B >> 32) != c1 || (C >> 32) != c2)
		{
			return null;
		}

		a ^= aKey.tweak[1][0];
		b ^= aKey.tweak[1][1];
		c ^= aKey.tweak[1][2];
		for (int i = 0; i < 3; i++)
		{
			a += Integer.rotateLeft(b, L3) ^ Integer.rotateRight(c, R3);
			b += Integer.rotateLeft(c, L2) ^ Integer.rotateRight(a, R2);
			c += Integer.rotateLeft(a, L1) ^ Integer.rotateRight(b, R1);
		}
		a ^= aKey.tweak[0][0];
		b ^= aKey.tweak[0][1];
		c ^= aKey.tweak[0][2];

		return new ObjectId(a, b, c);
	}


	private void encodeBase62(Key aKey, char[] aOutput, long aValue, int aIndex)
	{
		for (int start = 6 * aIndex, j = start + 6; --j >= start;)
		{
			int symbol = (int)(aValue % 62);
			aOutput[j] = aKey.toBase62[j][symbol];
			aValue /= 62;
		}
	}


	private static long decodeBase62(Key aKey, String aName, int aIndex)
	{
		long value = 0;
		for (int i = 6 * aIndex, end = i + 6; i < end; i++)
		{
			int symbol = aKey.fromBase62[i][aName.charAt(i) & 0x7f];
			value *= 62;
			value += symbol;
		}

		return value;
	}


	public byte[] toByteArray()
	{
		byte[] buffer = new byte[LENGTH];
		putInt32(buffer, 0, mTime);
		putInt32(buffer, 4, mSession);
		putInt32(buffer, 8, mSequence);
		return buffer;
	}


	@Override
	public String toString()
	{
		return String.format("%08x%08x%08x", mTime, mSession, mSequence);
	}


	@Override
	public int hashCode()
	{
		return mTime ^ mSession ^ mSequence;
	}


	@Override
	public boolean equals(Object aOther)
	{
		if (aOther instanceof ObjectId v)
		{
			return (mTime == v.mTime && mSession == v.mSession && mSequence == v.mSequence);
		}
		return false;
	}


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
		return ((aBuffer[aPosition] & 0xFF) << 24)
			+ ((aBuffer[aPosition + 1] & 0xFF) << 16)
			+ ((aBuffer[aPosition + 2] & 0xFF) << 8)
			+ ((aBuffer[aPosition + 3] & 0xFF));
	}


	private static long getInt32L(byte[] aBuffer, int aPosition)
	{
		return getInt32(aBuffer, aPosition) & 0xffffffffL;
	}


	private static void putInt32(byte[] aBuffer, int aPosition, int aValue)
	{
		aBuffer[aPosition] = (byte)(aValue >>> 24);
		aBuffer[aPosition + 1] = (byte)(aValue >> 16);
		aBuffer[aPosition + 2] = (byte)(aValue >> 8);
		aBuffer[aPosition + 3] = (byte)(aValue);
	}


	public static void main(String... args)
	{
		try
		{
			byte[] kd = new byte[32];
			kd[0] = 2;
			Key key = new Key(kd);

			HashMap<Character, Integer> cc = new HashMap<>();
			HashMap<Integer, HashMap<Character, Integer>> ci = new HashMap<>();

			for (int i = 0; i < 10_000_000; i++)
//			for (int i = 0; i < 10; i++)
			{
				ObjectId in = ObjectId.randomId();
				String encoded = in.toArmouredString(key);
				ObjectId out = ObjectId.fromArmouredString(key, encoded);
//				System.out.printf("%s  %s  %s  %s%n", encoded, in, out == null ? "-".repeat(24) : out, in.equals(out));
				if (!in.equals(out))
				{
					throw new IllegalStateException();
				}

				for (int j = 0; j < encoded.length(); j++)
				{
					cc.put(encoded.charAt(j), cc.getOrDefault(encoded.charAt(j), 0) + 1);
					HashMap<Character, Integer> m = ci.computeIfAbsent(j, k -> new HashMap<>());
					m.put(encoded.charAt(j), m.getOrDefault(encoded.charAt(j), 0) + 1);
					ci.put(j, m);
				}
			}

			System.out.println(cc);
			for (Entry e : ci.entrySet())
			{
				System.out.println(e.getKey() + " " + e.getValue());
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
