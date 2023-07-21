package org.terifan.raccoon.document;

import java.io.Serializable;
import static java.lang.Integer.parseUnsignedInt;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

	private final int mTime;
	private final int mSession;
	private final int mSequence;


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


	public static class Key
	{
		final char[][] encoder = new char[24][32];
		final int[][] decoder = new int[24][128];


		public Key(long aSeed)
		{
			Random rnd = new Random(aSeed);
			for (int k = 0; k < 24; k++)
			{
				Arrays.fill(decoder[k], -1);
				for (int i = 0; i < 32;)
				{
					int j = rnd.nextInt(62);
					if (decoder[k][Holder.BASE62[j]] == -1)
					{
						decoder[k][Holder.BASE62[j]] = i;
						encoder[k][i++] = Holder.BASE62[j];
					}
				}
			}
		}
	}


	public String toArmoredString(Key aKey)
	{
		int now = encodeTime();

		char[] buf = new char[24];
		encode(aKey, buf, 0, ((long)mTime << 8) + (0xff & (now >> 16)));
		encode(aKey, buf, 1, ((long)mSession << 8) + (0xff & (now >> 8)));
		encode(aKey, buf, 2, ((long)mSequence << 8) + (0xff & (now >> 0)));
		return new String(buf);
	}


	public static ObjectId fromArmoredString(Key aKey, String aName)
	{
		long a = decode(aKey, aName, 0);
		long b = decode(aKey, aName, 1);
		long c = decode(aKey, aName, 2);

		LocalDateTime now = decodeTime((int)(((0xff & a) << 16) + ((0xff & b) << 8) + (0xff & c)));
//		System.out.println(now);

		return new ObjectId((int)(a >> 8), (int)(b >> 8), (int)(c >> 8));
	}


	protected int encodeTime()
	{
		LocalDateTime dateTime = LocalDateTime.now();
		return (dateTime.getYear() % 30) * 367 * 24 * 60 + dateTime.getDayOfYear() * 24 * 60 + dateTime.getHour() * 60 + dateTime.getMinute();
	}


	protected static LocalDateTime decodeTime(int aNow)
	{
		int ye = aNow / 367 / 24 / 60;
		int da = (aNow / 24 / 60) % 367;
		int ho = (aNow / 60) % 24;
		int mi = aNow % 60;
		int currentYear = LocalDate.now().getYear();
		ye += 30 * (currentYear / 30);
		if (ye > currentYear)
		{
			ye -= 30;
		}
		return LocalDateTime.of(LocalDate.ofYearDay(ye, da), LocalTime.of(ho, mi));
	}


	private void encode(Key aKey, char[] aBuffer, int aIndex, long aValue)
	{
		for (int i = 35, j = 8 * aIndex; i >= 0; i-=5, j++)
		{
			int symbol = (int)(31 & (aValue >>> i));
			aBuffer[j] = aKey.encoder[j][symbol];
		}
	}


	private static long decode(Key aKey, String aName, int aIndex)
	{
		long value = 0;
		for (int i = 35, j = 8 * aIndex; i >= 0; i-=5, j++)
		{
			int symbol = aKey.decoder[j][aName.charAt(j)];
			if (symbol == -1)
			{
				throw new IllegalArgumentException();
			}
			value |= ((long)symbol) << i;
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

			for (int i = 0; i < 100; i++)
			{
				ObjectId in = ObjectId.randomId();
				String encoded = in.toArmoredString(new Key(3));
				ObjectId out = ObjectId.fromArmoredString(new Key(3), encoded);
				System.out.printf("%s  %s  %s  %s%n", encoded, in, out, out.equals(in));
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
