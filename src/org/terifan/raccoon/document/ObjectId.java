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
	private final static Instance STATIC_INSTANCE = new Instance(0);

	public final static int LENGTH = 12;

	private final int mTime;
	private final int mSession;
	private final int mSequence;


	private static class Holder
	{
		final static SecureRandom PRNG = new SecureRandom();
		final static int SESSION = PRNG.nextInt();
		final static AtomicInteger SEQUENCE = new AtomicInteger(PRNG.nextInt());
		final static char[] BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
		final static long[] SHIFT =
		{
			1, 62, 3844, 238328, 14776336, 916132832, 56800235584L, 3521614606208L
		};
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


	public static class Instance
	{
		final char[][] encoder = new char[24][62];
		final int[][] decoder = new int[24][128];
		final int[][] tweak = new int[2][3];


		public Instance(long aSecret)
		{
			Random rnd = new Random(aSecret);
			for (int i = 0; i < 2; i++)
			{
				for (int j = 0; j < 3; j++)
				{
					tweak[i][j] = rnd.nextInt();
				}
			}
			for (int k = 0; k < 24; k++)
			{
				Arrays.fill(decoder[k], -1);
				int[] order = order(62, rnd);
				for (int i = 0; i < 62;)
				{
					int j = order[i];
					decoder[k][Holder.BASE62[j]] = i;
					encoder[k][i++] = Holder.BASE62[j];
				}
			}
		}
	}


	private static int[] order(int aLength, Random aRandom)
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


	public String toArmoredString()
	{
		return toArmoredString(STATIC_INSTANCE);
	}


	public static ObjectId fromArmoredString(String aName)
	{
		return fromArmoredString(STATIC_INSTANCE, aName);
	}


	public String toArmoredString(Instance aInstance)
	{
		int a = mTime;
		int b = mSession;
		int c = mSequence;

		a ^= aInstance.tweak[0][0];
		b ^= aInstance.tweak[0][1];
		c ^= aInstance.tweak[0][2];
		for (int i = 0; i < 3; i++)
		{
			c -= Integer.rotateLeft(a, 9) ^ Integer.rotateRight(b, 7);
			b -= Integer.rotateLeft(c, 13) ^ Integer.rotateRight(a, 25);
			a -= Integer.rotateLeft(b, 5) ^ Integer.rotateRight(c, 10);
		}
		a ^= aInstance.tweak[1][0];
		b ^= aInstance.tweak[1][1];
		c ^= aInstance.tweak[1][2];

		long chk = 0xffffffffL & ((long)a + (long)b + (long)c);
		long A = (a & 0xffffffffL) * 13 + (chk % 13);
		long B = (b & 0xffffffffL) * 13 + (chk / 13 % 13);
		long C = (c & 0xffffffffL) * 13 + (chk / 13 / 13 % 13);

		StringBuilder buf = new StringBuilder();
		encode(aInstance, buf, A, 0, 6);
		encode(aInstance, buf, B, 6, 6);
		encode(aInstance, buf, C, 12, 6);
		return buf.toString();
	}


	public static ObjectId fromArmoredString(Instance aInstance, String aName)
	{
		try
		{
			long A = decode(aInstance, aName, 0, 6);
			long B = decode(aInstance, aName, 6, 6);
			long C = decode(aInstance, aName, 12, 6);

			int a = (int)(A / 13);
			int b = (int)(B / 13);
			int c = (int)(C / 13);

			long chk = 0xffffffffL & ((long)a + (long)b + (long)c);
			if (A % 13 != chk % 13 || B % 13 != chk / 13 % 13 || C % 13 != chk / 13 / 13 % 13)
			{
				return null;
			}

			a ^= aInstance.tweak[1][0];
			b ^= aInstance.tweak[1][1];
			c ^= aInstance.tweak[1][2];
			for (int i = 0; i < 3; i++)
			{
				a += Integer.rotateLeft(b, 5) ^ Integer.rotateRight(c, 10);
				b += Integer.rotateLeft(c, 13) ^ Integer.rotateRight(a, 25);
				c += Integer.rotateLeft(a, 9) ^ Integer.rotateRight(b, 7);
			}
			a ^= aInstance.tweak[0][0];
			b ^= aInstance.tweak[0][1];
			c ^= aInstance.tweak[0][2];

			return new ObjectId(a, b, c);
		}
		catch (Exception e)
		{
			return null;
		}
	}


	private void encode(Instance aInstance, StringBuilder aOutput, long aValue, int aIndex, int aLength)
	{
		for (int i = 0, j = aIndex; i < aLength; i++, j++)
		{
			int symbol = (int)(aValue % 62);
			aOutput.append(aInstance.encoder[j][symbol]);
			aValue /= 62;
		}
	}


	private static long decode(Instance aInstance, String aName, int aIndex, int aLength)
	{
		long value = 0;
		for (int i = 0, j = aIndex; i < aLength; i++, j++)
		{
			int symbol = aInstance.decoder[j][aName.charAt(j)];
			if (symbol == -1)
			{
				throw new IllegalArgumentException();
			}
			value += symbol * Holder.SHIFT[i];
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
//			long t = System.currentTimeMillis();
//			for (int i = 0; i < 100; i++)
//			{
//				ObjectId objectId = ObjectId.randomId();
//
//				System.out.printf("%s %10d %10d %s%n", objectId, 0xffffffffL & objectId.session(), 0xffffffffL & objectId.sequence(), new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(objectId.time()));
//
//				if (!ObjectId.fromString(objectId.toString()).equals(objectId))
//				{
//					System.out.println("#");
//				}
//				if (!ObjectId.fromBytes(objectId.toByteArray()).equals(objectId))
//				{
//					System.out.println("#");
//				}
//			}
//			System.out.println(System.currentTimeMillis() - t);

			Instance instance = new Instance(321954098761L);

			for (int i = 0; i < 100; i++)
			{
				ObjectId in = ObjectId.randomId();
				String encoded = in.toArmoredString(instance);
				ObjectId out = ObjectId.fromArmoredString(instance, encoded);
				System.out.printf("%s  %s  %s  %s%n", encoded, in, out == null ? "-".repeat(24) : out, in.equals(out));
				if (!in.equals(out)) throw new IllegalStateException();
			}

//			System.out.println(ObjectId.fromArmoredString(instance, "UNR9gIdZObndSWNQ1lDoIcf0"));
//			System.out.println(ObjectId.fromArmoredString(instance, "UNR9gIdZObndSWNQ1lDoIcf1"));
//			System.out.println(ObjectId.fromArmoredString(instance, "UNR9gIdZObndSWNQ1lDoIcf2"));
//			System.out.println(ObjectId.fromArmoredString(instance, "UNR9gIdZObndSWNQ1lDoIcf3"));
//			System.out.println(ObjectId.fromArmoredString(instance, "UNR9gIdZObndSWNQ1lDoIcf4"));
//			System.out.println(ObjectId.fromArmoredString(instance, "UNR9gIdZObndSWNQ1lDoIcf5"));
//			System.out.println(ObjectId.fromArmoredString(instance, "UNR9gIdZObndSWNQ1lDoIcf6"));
//			System.out.println(ObjectId.fromArmoredString(instance, "UNR9gIdZObndSWNQ1lDoIcf7"));
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
