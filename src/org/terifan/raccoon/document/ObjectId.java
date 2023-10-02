package org.terifan.raccoon.document;

import java.io.Serializable;
import static java.lang.Integer.parseUnsignedInt;
import java.security.SecureRandom;
import java.util.Arrays;
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
		final static Key DEFAULT_KEY = new Key(new byte[32]);
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
	 * Key used for protecting an ObjectId.
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
			Random rnd = new Random(((long)getInt32(aSecretKey, 0) << 32) + getInt32(aSecretKey, 4));
			for (int i = 0, k = 0; i < 2; i++)
			{
				for (int j = 0; j < 3; j++, k++)
				{
					tweak[i][j] = rnd.nextInt() ^ getInt32(aSecretKey, 8 + k * 4);
				}
			}
			for (int k = 0; k < 18; k++)
			{
				Arrays.fill(fromBase62[k], -1);
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
	 * Return an armoured String representation of this ObjectId using the default zero key.
	 */
	public String toArmouredString()
	{
		return toArmouredString(Holder.DEFAULT_KEY);
	}


	/**
	 * @return a decoded ObjectId using the default zero key or null if the decoding failed.
	 */
	public static ObjectId fromArmouredString(String aName)
	{
		return fromArmouredString(Holder.DEFAULT_KEY, aName);
	}


	/**
	 * @return the ObjectId as an encrypted 18 character Base62 encoded String. The encoded String also contains a checksum.
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

		int chk = Math.abs((int)(a * F1 + b * F2 + c * F3));
		int c0 = aKey.chk[0][chk / F4 % 13];
		int c1 = aKey.chk[1][chk / F5 % 13];
		int c2 = aKey.chk[2][chk / F6 % 13];

		long A = (a & 0xffffffffL) * 13 + c0;
		long B = (b & 0xffffffffL) * 13 + c1;
		long C = (c & 0xffffffffL) * 13 + c2;

		char[] buf = new char[18];
		encodeBase62(aKey, buf, A, 0);
		encodeBase62(aKey, buf, B, 1);
		encodeBase62(aKey, buf, C, 2);
		return new String(buf);
	}


	/**
	 *
	 * @param aKey the Key used to armour the ObjectId
	 * @param aName the encoded representation of an ObjectId
	 * @return the decoded ObjectId or null if decoding was unsuccessful
	 */
	public static ObjectId fromArmouredString(Key aKey, String aName)
	{
		try
		{
			long A = decodeBase62(aKey, aName, 0);
			long B = decodeBase62(aKey, aName, 1);
			long C = decodeBase62(aKey, aName, 2);

			int a = (int)(A / 13);
			int b = (int)(B / 13);
			int c = (int)(C / 13);

			int chk = Math.abs((int)(a * F1 + b * F2 + c * F3));
			int c0 = aKey.chk[0][chk / F4 % 13];
			int c1 = aKey.chk[1][chk / F5 % 13];
			int c2 = aKey.chk[2][chk / F6 % 13];
			if (A % 13 != c0 || B % 13 != c1 || C % 13 != c2)
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
		catch (Exception e)
		{
			return null;
		}
	}


	private void encodeBase62(Key aKey, char[] aOutput, long aValue, int aIndex)
	{
		for (int start = 6 * aIndex, j = start + 6; --j >= start; )
		{
			int symbol = (int)(aValue % 62);
			aOutput[j] = aKey.toBase62[j][symbol];
			aValue /= 62;
		}
	}


	private static long decodeBase62(Key aKey, String aName, int aIndex)
	{
		long value = 0;
		for (int i = 0, j = 6 * aIndex; i < 6; i++, j++)
		{
			int symbol = aKey.fromBase62[j][aName.charAt(j)];
			if (symbol == -1)
			{
				throw new IllegalArgumentException();
			}
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
			Key key = new Key(new byte[32]);

			for (int i = 0; i < 100; i++)
			{
				ObjectId in = ObjectId.randomId();
				String encoded = in.toArmouredString(key);
				ObjectId out = ObjectId.fromArmouredString(key, encoded);
				System.out.printf("%s  %s  %s  %s%n", encoded, in, out == null ? "-".repeat(24) : out, in.equals(out));
				if (!in.equals(out)) throw new IllegalStateException();
			}

			System.out.println(ObjectId.fromArmouredString(key, "4AtV3s6qzehyi9papA"));
			System.out.println(ObjectId.fromArmouredString(key, "4AtV3s6qzehyi9papB"));
			System.out.println(ObjectId.fromArmouredString(key, "4AtV3s6qzehyi9papC"));
			System.out.println(ObjectId.fromArmouredString(key, "4AtV3s6qzehyi9papD"));
			System.out.println(ObjectId.fromArmouredString(key, "4AtV3s6qzehyi9papE"));
			System.out.println(ObjectId.fromArmouredString(key, "4AtV3s6qzehyi9papF"));
			System.out.println(ObjectId.fromArmouredString(key, "4AtV3s6qzehyi9papG"));
			System.out.println(ObjectId.fromArmouredString(key, "4AtV3s6qzehyi9papH"));
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
