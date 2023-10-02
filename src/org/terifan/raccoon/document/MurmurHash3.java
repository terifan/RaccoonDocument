package org.terifan.raccoon.document;

import static java.lang.Integer.reverseBytes;


class MurmurHash3
{
	private int mValue;
	private int mBits;
	private int mShift;
	private int mTotal;


	public MurmurHash3(int aSeed)
	{
		mValue = aSeed;
	}


	public MurmurHash3 updateByte(int aValue)
	{
		mBits |= aValue << (24 - mShift);
		mShift += 8;
		mTotal++;
		if (mShift == 32)
		{
			updateImpl(mBits);
			mShift = 0;
			mBits = 0;
		}
		return this;
	}


	public MurmurHash3 updateInt(int aValue)
	{
		updateImpl(mBits | (aValue >>> mShift));
		mBits = aValue << (32 - mShift);
		mTotal += 4;
		return this;
	}


	public MurmurHash3 updateBytes(byte[] aBuffer, int aOffset, int aLength)
	{
		int i = 0;
		for (; i + 3 < aLength; i += 4)
		{
			int v = 0;
			v |= ((0xff & aBuffer[aOffset++]) << 24);
			v |= ((0xff & aBuffer[aOffset++]) << 16);
			v |= ((0xff & aBuffer[aOffset++]) << 8);
			v |= ((0xff & aBuffer[aOffset++]));
			updateInt(v);
		}
		for (; i < aLength; i++)
		{
			updateByte(0xff & aBuffer[aOffset++]);
		}
		return this;
	}


	public MurmurHash3 updateUTF8(CharSequence aData)
	{
		for (int i = 0, len = aData.length(); i < len;)
		{
			int code = aData.charAt(i++);
			if (code < 0x80)
			{
				updateByte(code);
			}
			else if (code < 0x800)
			{
				int k = (0xC0 | (code >> 6)) | ((0x80 | (code & 0x3F)) << 8);
				updateByte(0xff & (k));
				updateByte(0xff & (k >>> 8));
			}
			else if (code < 0xD800 || code > 0xDFFF || i >= len)
			{
				int k = (0xE0 | (code >> 12)) | ((0x80 | ((code >> 6) & 0x3F)) << 8) | ((0x80 | (code & 0x3F)) << 16);
				updateByte(0xff & (k));
				updateByte(0xff & (k >>> 8));
				updateByte(0xff & (k >>> 16));
			}
			else
			{
				int utf32 = aData.charAt(i++);
				utf32 = ((code - 0xD7C0) << 10) + (utf32 & 0x3FF);
				int k = (0xff & (0xF0 | (utf32 >> 18))) | ((0x80 | ((utf32 >> 12) & 0x3F))) << 8 | ((0x80 | ((utf32 >> 6) & 0x3F))) << 16 | (0x80 | (utf32 & 0x3F)) << 24;
				updateInt(k);
			}
		}

		return this;
	}


	private void updateImpl(int aValue)
	{
		aValue = reverseBytes(aValue);
		aValue *= 0xcc9e2d51;
		aValue = (aValue << 15) | (aValue >>> 17);
		aValue *= 0x1b873593;

		mValue ^= aValue;
		mValue = (mValue << 13) | (mValue >>> 19);
		mValue = mValue * 5 + 0xe6546b64;
	}


	public int getValue()
	{
		int o = mValue;

		if (mShift > 0)
		{
			int k1 = reverseBytes(mBits);
			k1 *= 0xcc9e2d51;
			k1 = (k1 << 15) | (k1 >>> 17);
			k1 *= 0x1b873593;
			o ^= k1;
		}

		o ^= mTotal;

		o ^= o >>> 16;
		o *= 0x85ebca6b;
		o ^= o >>> 13;
		o *= 0xc2b2ae35;
		o ^= o >>> 16;

		return o;
	}
}
