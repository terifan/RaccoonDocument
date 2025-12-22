package org.terifan.raccoon.document;

import java.io.IOException;
import java.util.Arrays;


public class Lookup
{
	private LRU<ByteKey> mLRU;


	public Lookup(boolean aEncode)
	{
		mLRU = new LRU<>(aEncode);
	}


	public void write(BinaryOutputStream out, byte[] header) throws IOException
	{
		ByteKey key = new ByteKey(header);
		int ref = mLRU.indexOf(key);
		if (ref == -1)
		{
			out.writeVarint(header.length);
			out.write(header);
			mLRU.add(key);
		}
		else
		{
			out.writeVarint(-ref - 1);
		}
	}


	byte[] read(BinaryDecoder aIn) throws IOException
	{
		int ref = (int)aIn.readVarint();

		byte[] header;
		if (ref < 0)
		{
			header = mLRU.valueAt(-ref-1).mBuffer;
		}
		else
		{
			header = aIn.readNBytes(ref);
			mLRU.add(new ByteKey(header));
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
