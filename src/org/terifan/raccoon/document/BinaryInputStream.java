package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;


public class BinaryInputStream
{
	private final byte[] mReadBuffer = new byte[8];

	protected InputStream mInputStream;


	public BinaryInputStream(InputStream aInputStream)
	{
		mInputStream = aInputStream;
	}


	public int readByte() throws IOException
	{
		int c = mInputStream.read();
		if (c == -1)
		{
			throw new StreamException("Premature end of stream");
		}
		return c;
	}


	public short readShort() throws IOException
	{
		return (short)((readByte() << 8) | readByte());
	}


	public int readInt() throws IOException
	{
		readBytes(mReadBuffer, 0, 4);
		return ((mReadBuffer[0] & 0xff) << 24)
			+ ((mReadBuffer[1] & 0xff) << 16)
			+ ((mReadBuffer[2] & 0xff) << 8)
			+ ((mReadBuffer[3] & 0xff) << 0);
	}


	public long readLong() throws IOException
	{
		readBytes(mReadBuffer, 0, 8);
		return (((long)(mReadBuffer[0] & 0xff) << 56)
			+ ((long)(mReadBuffer[1] & 0xff) << 48)
			+ ((long)(mReadBuffer[2] & 0xff) << 40)
			+ ((long)(mReadBuffer[3] & 0xff) << 32)
			+ ((long)(mReadBuffer[4] & 0xff) << 24)
			+ ((mReadBuffer[5] & 0xff) << 16)
			+ ((mReadBuffer[6] & 0xff) << 8)
			+ ((mReadBuffer[7] & 0xff) << 0));
	}


	public byte[] readBytes(byte[] aBuffer) throws IOException
	{
		int len = mInputStream.read(aBuffer);
		if (len != aBuffer.length)
		{
			throw new IllegalStateException();
		}
		return aBuffer;
	}


	public <T> T skipBytes(int aLength) throws IOException
	{
		byte[] t = new byte[aLength];
		int len = mInputStream.read(t);
		return null;
	}


	public byte[] readBytes(byte[] aBuffer, int aOffset, int aLength) throws IOException
	{
		int len = mInputStream.read(aBuffer, aOffset, aLength);
		if (len != aLength)
		{
			throw new IOException("Error reading from underlying stream. Only " + len + " bytes available, expected " + aLength);
		}
		return aBuffer;
	}


	public long readVarint() throws IOException
	{
		for (long n = 0, result = 0; n < 64; n += 7)
		{
			int b = readByte();
			result += (long)(b & 127) << n;
			if (b < 128)
			{
				return (result >>> 1) ^ -(result & 1);
			}
		}

		throw new StreamException("Variable int64 exceeds maximum length");
	}


	public long readUnsignedVarint() throws IOException
	{
		for (long n = 0, result = 0; n < 64; n += 7)
		{
			int b = readByte();
			result += (long)(b & 127) << n;
			if (b < 128)
			{
				return result;
			}
		}

		throw new StreamException("Variable int64 exceeds maximum length");
	}


	public String readCompactString() throws IOException
	{
		StringBuilder sb = new StringBuilder();

		for (;;)
		{
			int c = readByte();
			sb.append((char)(0x7f & c));
			if (c >= 128)
			{
				break;
			}
		}

		return sb.toString();
	}


	public String readString() throws IOException
	{
		return readUTF((int)readUnsignedVarint());
	}


	public String readUTF(int aLength) throws IOException
	{
		if (aLength < 0)
		{
			throw new StreamException("Negative string length");
		}

		char[] output = new char[aLength];

		for (int i = 0; i < output.length; i++)
		{
			int c = readByte();

			if (c < 128) // 0xxxxxxx
			{
				output[i] = (char)c;
			}
			else if ((c & 0xE0) == 0xC0) // 110xxxxx
			{
				output[i] = (char)(((c & 0x1F) << 6) | (readByte() & 0x3F));
			}
			else if ((c & 0xF0) == 0xE0) // 1110xxxx
			{
				output[i] = (char)(((c & 0x0F) << 12) | ((readByte() & 0x3F) << 6) | (readByte() & 0x3F));
			}
			else
			{
				throw new StreamException("This decoder only handles 16-bit characters: c = " + c);
			}
		}

		return new String(output);
	}


	public long readInterleaved() throws IOException
	{
		long p = readUnsignedVarint();
		return (reverseShift(p) << 32) | reverseShift(p >>> 1);
	}


	private static long reverseShift(long aWord)
	{
		aWord &= 0x5555555555555555L;

		aWord = (aWord | (aWord >> 1)) & 0x3333333333333333L;
		aWord = (aWord | (aWord >> 2)) & 0x0f0f0f0f0f0f0f0fL;
		aWord = (aWord | (aWord >> 4)) & 0x00ff00ff00ff00ffL;
		aWord = (aWord | (aWord >> 8)) & 0x0000ffff0000ffffL;
		aWord = (aWord | (aWord >> 16)) & 0x00000000ffffffffL;

		return aWord;
	}


	/**
	 * note: this implementation will not close the underlying stream.
	 */
	public void close() throws IOException
	{
		mInputStream = null;
	}


	private static long shift(long aWord)
	{
		aWord &= 0xffffffffL;

		aWord = (aWord | (aWord << 16)) & 0x0000ffff0000ffffL;
		aWord = (aWord | (aWord << 8)) & 0x00ff00ff00ff00ffL;
		aWord = (aWord | (aWord << 4)) & 0x0f0f0f0f0f0f0f0fL;
		aWord = (aWord | (aWord << 2)) & 0x3333333333333333L;
		aWord = (aWord | (aWord << 1)) & 0x5555555555555555L;

		return aWord;
	}


	public BigDecimal readDecimal() throws IOException
	{
		char[] s = new char[(int)readUnsignedVarint()];
		for (int i = 0; i < s.length;)
		{
			int v = readByte();
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
}
