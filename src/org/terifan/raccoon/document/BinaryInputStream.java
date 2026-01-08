package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;


class BinaryInputStream implements AutoCloseable
{
	private final byte[] mReadBuffer = new byte[8];

	private InputStream mInputStream;


	BinaryInputStream(InputStream aInputStream)
	{
		mInputStream = aInputStream;
	}


	void skip(int aLength) throws IOException
	{
		mInputStream.skip(aLength);
	}


	int read() throws IOException
	{
		int c = mInputStream.read();
		if (c == -1)
		{
			throw new StreamException("Premature end of stream");
		}
		return c;
	}


	byte[] read(byte[] aBuffer) throws IOException
	{
		int len = mInputStream.read(aBuffer);
		if (len != aBuffer.length)
		{
			throw new IllegalStateException();
		}
		return aBuffer;
	}


	int read(byte[] aBuffer, int aOffset, int aLength) throws IOException
	{
		int len = mInputStream.read(aBuffer, aOffset, aLength);
		if (len != aLength)
		{
			throw new IOException("Error reading from underlying stream. Only " + len + " bytes available, expected " + aLength);
		}
		return aLength;
	}


	int readInt() throws IOException
	{
		read(mReadBuffer, 0, 4);
		return ((mReadBuffer[0] & 0xff) << 24)
			+ ((mReadBuffer[1] & 0xff) << 16)
			+ ((mReadBuffer[2] & 0xff) << 8)
			+ ((mReadBuffer[3] & 0xff) << 0);
	}


	long readLong() throws IOException
	{
		read(mReadBuffer, 0, 8);
		return (((long)(mReadBuffer[0] & 0xff) << 56)
			+ ((long)(mReadBuffer[1] & 0xff) << 48)
			+ ((long)(mReadBuffer[2] & 0xff) << 40)
			+ ((long)(mReadBuffer[3] & 0xff) << 32)
			+ ((long)(mReadBuffer[4] & 0xff) << 24)
			+ ((mReadBuffer[5] & 0xff) << 16)
			+ ((mReadBuffer[6] & 0xff) << 8)
			+ ((mReadBuffer[7] & 0xff) << 0));
	}


	int readVarint() throws IOException
	{
		for (int n = 0, result = 0; n < 32; n += 7)
		{
			int b = read();
			result += (b & 127) << n;
			if (b < 128)
			{
				return (result >>> 1) ^ -(result & 1);
			}
		}

		throw new StreamException("Variable int64 exceeds maximum length");
	}


	int readUnsignedVarint() throws IOException
	{
		for (int n = 0, result = 0; n < 32; n += 7)
		{
			int b = read();
			result += (b & 127) << n;
			if (b < 128)
			{
				return result;
			}
		}

		throw new StreamException("Variable int64 exceeds maximum length");
	}


	long readVarlong() throws IOException
	{
		for (long n = 0, result = 0; n < 64; n += 7)
		{
			int b = read();
			result += (long)(b & 127) << n;
			if (b < 128)
			{
				return (result >>> 1) ^ -(result & 1);
			}
		}

		throw new StreamException("Variable int64 exceeds maximum length");
	}


	long readUnsignedVarlong() throws IOException
	{
		for (long n = 0, result = 0; n < 64; n += 7)
		{
			int b = read();
			result += (long)(b & 127) << n;
			if (b < 128)
			{
				return result;
			}
		}

		throw new StreamException("Variable int64 exceeds maximum length");
	}


	String readString() throws IOException
	{
		return readUTF(readUnsignedVarint());
	}


	String readUTF(int aLength) throws IOException
	{
		if (aLength < 0)
		{
			throw new StreamException("Negative string length");
		}

		char[] output = new char[aLength];

		for (int i = 0; i < output.length; i++)
		{
			int c = read();

			if (c < 128) // 0xxxxxxx
			{
				output[i] = (char)c;
			}
			else if ((c & 0xE0) == 0xC0) // 110xxxxx
			{
				output[i] = (char)(((c & 0x1F) << 6) | (read() & 0x3F));
			}
			else if ((c & 0xF0) == 0xE0) // 1110xxxx
			{
				output[i] = (char)(((c & 0x0F) << 12) | ((read() & 0x3F) << 6) | (read() & 0x3F));
			}
			else
			{
				throw new StreamException("This decoder only handles 16-bit characters: c = " + c);
			}
		}

		return new String(output);
	}


	BinaryType readType() throws IOException
	{
		int i = mInputStream.read();
		if (i == -1)
		{
			return BinaryType.TERMINATOR;
		}
		return BinaryType.values()[i];
	}


	BigDecimal readBigDecimal() throws IOException
	{
		char[] s = new char[readUnsignedVarint()];
		for (int i = 0; i < s.length;)
		{
			int v = read();
			int a = '+' + (v >>> 4);
			if (a == ':')
			{
				a = 'E';
			}
			s[i++] = (char)a;
			if (i < s.length)
			{
				int b = '+' + (15 & v);
				if (b == ':')
				{
					b = 'E';
				}
				s[i++] = (char)b;
			}
		}
		return new BigDecimal(s);
	}


	BigInteger readBigInteger() throws IOException
	{
		char[] s = new char[readUnsignedVarint()];
		for (int i = 0; i < s.length;)
		{
			int v = read();
			int a = '+' + (v >>> 4);
			if (a == ':')
			{
				a = 'E';
			}
			s[i++] = (char)a;
			if (i < s.length)
			{
				int b = '+' + (15 & v);
				if (b == ':')
				{
					b = 'E';
				}
				s[i++] = (char)b;
			}
		}
		return new BigInteger(new String(s));
	}


	String readString(LookupMap<String> aLookup) throws IOException
	{
		int i = (int)readVarint();
		if (i < 0)
		{
			return aLookup.valueAt(-i - 1);
		}
		String value = readUTF(i);
		aLookup.add(value);
		return value;
	}


	@Override
	public void close() throws IOException
	{
		if (mInputStream != null)
		{
			mInputStream.close();
			mInputStream = null;
		}
	}

//	long readInterleaved() throws IOException
//	{
//		long p = readUnsignedVarint();
//		return (reverseShift(p >>> 1) << 32) | reverseShift(p);
//	}
//
//
//	private static long reverseShift(long aWord)
//	{
//		aWord &= 0x5555555555555555L;
//
//		aWord = (aWord | (aWord >> 1)) & 0x3333333333333333L;
//		aWord = (aWord | (aWord >> 2)) & 0x0f0f0f0f0f0f0f0fL;
//		aWord = (aWord | (aWord >> 4)) & 0x00ff00ff00ff00ffL;
//		aWord = (aWord | (aWord >> 8)) & 0x0000ffff0000ffffL;
//		aWord = (aWord | (aWord >> 16)) & 0x00000000ffffffffL;
//
//		return aWord;
//	}
}
