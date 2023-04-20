package org.terifan.raccoon.document;

import java.util.zip.CRC32;


class Checksum
{
	private final CRC32 mCRC;


	Checksum()
	{
		mCRC = new CRC32();
		mCRC.update(0b10001101);
	}


	Checksum updateByte(int aValue)
	{
		mCRC.update(aValue);
		return this;
	}


	Checksum updateInt(int aValue)
	{
		mCRC.update(0xFF & (aValue >>> 24));
		mCRC.update(0xFF & (aValue >> 16));
		mCRC.update(0xFF & (aValue >> 8));
		mCRC.update(0xFF & aValue);
		return this;
	}


	Checksum updateChars(CharSequence aChars)
	{
		aChars.chars().forEach(c -> {
			if (c > 255)
			{
				mCRC.update(0xff & (c >> 8));
			}
			mCRC.update(0xff & c);
		});
		return this;
	}


	Checksum update(byte[] aBuffer, int aOffset, int aLength)
	{
		try
		{
			mCRC.update(aBuffer, aOffset, aLength);
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			throw new StreamException("Buffer out of range");
		}
		return this;
	}


	int getValue()
	{
		return (int)mCRC.getValue();
	}









//	public static int update(byte[] aData, int aOffset, int aLength, int aSeed)
//	{
//		int h1 = aSeed;
//		int c1 = 0xcc9e2d51;
//		int c2 = 0x1b873593;
//		int roundedEnd = aOffset + (aLength & 0xfffffffc); // round down to 4 byte block
//
//		for (int i = aOffset; i < roundedEnd; i += 4)
//		{
//			int k1 = (aData[i] & 0xff) | ((aData[i + 1] & 0xff) << 8) | ((aData[i + 2] & 0xff) << 16) | (aData[i + 3] << 24);
//			k1 *= c1;
//			k1 = (k1 << 15) | (k1 >>> 17);
//			k1 *= c2;
//
//			h1 ^= k1;
//			h1 = (h1 << 13) | (h1 >>> 19);
//			h1 = h1 * 5 + 0xe6546b64;
//		}
//
//		// tail
//		int k1 = 0;
//
//		switch (aLength & 0x03)
//		{
//			case 3:
//				k1 = (aData[roundedEnd + 2] & 0xff) << 16;
//			// fallthrough
//			case 2:
//				k1 |= (aData[roundedEnd + 1] & 0xff) << 8;
//			// fallthrough
//			case 1:
//				k1 |= (aData[roundedEnd] & 0xff);
//				k1 *= c1;
//				k1 = (k1 << 15) | (k1 >>> 17);
//				k1 *= c2;
//				h1 ^= k1;
//				break;
//			case 0:
//				break;
//			default:
//				throw new IllegalStateException();
//		}
//
//		// finalization
//		h1 ^= aLength;
//
//		h1 ^= h1 >>> 16;
//		h1 *= 0x85ebca6b;
//		h1 ^= h1 >>> 13;
//		h1 *= 0xc2b2ae35;
//		h1 ^= h1 >>> 16;
//
//		return h1;
//	}
}
