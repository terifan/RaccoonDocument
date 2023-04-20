package org.terifan.raccoon.document;

import static java.lang.Integer.reverseBytes;


class MurmurHash3
{
	private int h1;
	private int buf;
	private int len;
	private int total;


	public MurmurHash3(int aSeed)
	{
		h1 = aSeed;
	}


	public void updateByte(int aByte)
	{
		buf |= aByte << (24 - len);
		len += 8;
		total++;
		if (len == 32)
		{
			impl(buf);
			len = 0;
			buf = 0;
		}
	}


	public void updateInt(int aInt)
	{
		total += 4;
		impl(buf | (aInt >>> len));
		buf = aInt << (32 - len);
	}


	public void update(byte[] aBuffer, int aOffset, int aLength)
	{
		for (int i = 0; i < aLength; i++)
		{
			updateByte(0xff & aBuffer[aOffset++]);
		}
	}


	public MurmurHash3 updateChars(CharSequence aChars)
	{
		aChars.chars().forEach(c ->
		{
			if (c > 255)
			{
				updateByte(0xff & (c >> 8));
			}
			updateByte(0xff & c);
		});
		return this;
	}


	private void impl(int aValue)
	{
		aValue = reverseBytes(aValue);
		aValue *= 0xcc9e2d51;
		aValue = (aValue << 15) | (aValue >>> 17);
		aValue *= 0x1b873593;

		h1 ^= aValue;
		h1 = (h1 << 13) | (h1 >>> 19);
		h1 = h1 * 5 + 0xe6546b64;
	}


	public int getValue()
	{
		int o = h1;

		if (len > 0)
		{
			int k1 = reverseBytes(buf);
			k1 *= 0xcc9e2d51;
			k1 = (k1 << 15) | (k1 >>> 17);
			k1 *= 0x1b873593;
			o ^= k1;
		}

		o ^= total;

		o ^= o >>> 16;
		o *= 0x85ebca6b;
		o ^= o >>> 13;
		o *= 0xc2b2ae35;
		o ^= o >>> 16;

		return o;
	}


//	public static void main(String... args)
//	{
//		try
//		{
//			for (int test = 1; test < 20; test++)
//			{
//				Checksum chk = new Checksum();
//
//				byte[] buf = new byte[test];
//
//				int i = 0;
//				for (int j = 0; j < test % 5; j++)
//				{
//					chk.updateByte(0xff & buf[i++]);
//				}
//
//				for (; i + 3 < buf.length-7;)
//				{
//					int d = 0xff & buf[i++];
//					int c = 0xff & buf[i++];
//					int b = 0xff & buf[i++];
//					int a = 0xff & buf[i++];
//					chk.updateInt((d << 24) | (c << 16) | (b << 8) | (a << 0));
//				}
//
//				if (i + 5 < buf.length)
//				{
//					chk.update(buf, i, 5);
//					i += 5;
//				}
//
//				for (; i < buf.length;)
//				{
//					chk.updateByte(0xff & buf[i++]);
//				}
//
//				int hash32 = MurmurHash3.hash32(buf, 0);
//
//				System.out.println(chk.getValue() == hash32);
//			}
//		}
//		catch (Throwable e)
//		{
//			e.printStackTrace(System.out);
//		}
//	}
}
