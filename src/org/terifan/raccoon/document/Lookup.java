package org.terifan.raccoon.document;

import java.io.IOException;
import java.util.Arrays;


class Lookup
{
	private LookupMap<ByteKey> mMap;


	public Lookup(boolean aEncode)
	{
		mMap = new LookupMap<>(aEncode);
	}


	public void write(BinaryOutputStream aOut, byte[] aHeader) throws IOException
	{
		ByteKey key = new ByteKey(aHeader);
		int ref = mMap.indexOf(key);
		if (ref == -1)
		{
			aOut.writeVarint(-aHeader.length - 1);
			aOut.write(aHeader);
			mMap.add(key);
		}
		else
		{
			aOut.writeVarint(ref * 2);
		}
	}


	byte[] read(BinaryDecoder aIn, int aIndex) throws IOException
	{
		byte[] header;
		if (aIndex < 0)
		{
			header = new byte[-aIndex - 1];
			aIn.read(header);
			mMap.add(new ByteKey(header));
		}
		else
		{
			header = mMap.valueAt(aIndex / 2).mBuffer;
		}

		return header;
	}


	static class ByteKey
	{
		final byte[] mBuffer;


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
}
