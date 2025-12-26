package org.terifan.raccoon.document;

import java.util.Arrays;


class ByteKey
{
	public final byte[] mBuffer;


	public ByteKey(byte[] aBuffer)
	{
		mBuffer = aBuffer;
	}


	@Override
	public String toString()
	{
		return new String(mBuffer);
	}


	@Override
	public int hashCode()
	{
		return Arrays.hashCode(mBuffer);
	}


	@Override
	public boolean equals(Object aOther)
	{
		return aOther == this || aOther instanceof ByteKey v && Arrays.equals(mBuffer, v.mBuffer);
	}
}
