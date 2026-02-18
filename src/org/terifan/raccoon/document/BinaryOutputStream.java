package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.terifan.raccoon.document.BinaryEncoder.IndexLookup;


class BinaryOutputStream implements AutoCloseable
{
	private final byte[] mWriteBuffer = new byte[8];

	private OutputStream mOutputStream;


	BinaryOutputStream(OutputStream aOutputStream)
	{
		mOutputStream = aOutputStream;
	}


	void write(int aValue) throws IOException
	{
		mOutputStream.write(aValue);
	}


	void write(byte[] aBuffer) throws IOException
	{
		write(aBuffer, 0, aBuffer.length);
	}


	void write(byte[] aBuffer, int aOffset, int aLength) throws IOException
	{
		mOutputStream.write(aBuffer, aOffset, aLength);
	}


	void writeInt(int aValue) throws IOException
	{
		mWriteBuffer[0] = (byte)(aValue >>> 24);
		mWriteBuffer[1] = (byte)(aValue >>> 16);
		mWriteBuffer[2] = (byte)(aValue >>> 8);
		mWriteBuffer[3] = (byte)(aValue);
		write(mWriteBuffer, 0, 4);
	}


	void writeLong(long aValue) throws IOException
	{
		mWriteBuffer[0] = (byte)(aValue >>> 56);
		mWriteBuffer[1] = (byte)(aValue >>> 48);
		mWriteBuffer[2] = (byte)(aValue >>> 40);
		mWriteBuffer[3] = (byte)(aValue >>> 32);
		mWriteBuffer[4] = (byte)(aValue >>> 24);
		mWriteBuffer[5] = (byte)(aValue >>> 16);
		mWriteBuffer[6] = (byte)(aValue >>> 8);
		mWriteBuffer[7] = (byte)(aValue);
		write(mWriteBuffer, 0, 8);
	}


	void writeVarint(int aValue) throws IOException
	{
		writeUnsignedVarint((aValue << 1) ^ (aValue >> 31));
	}


	void writeUnsignedVarint(int aValue) throws IOException
	{
		for (;;)
		{
			int b = aValue & 127;
			aValue >>>= 7;

			if (aValue == 0)
			{
				write(b);
				return;
			}

			write(128 + b);
		}
	}


	void writeVarlong(long aValue) throws IOException
	{
		writeUnsignedVarlong((aValue << 1) ^ (aValue >> 63));
	}


	void writeUnsignedVarlong(long aValue) throws IOException
	{
		for (;;)
		{
			int b = (int)(aValue & 127);
			aValue >>>= 7;

			if (aValue == 0)
			{
				write(b);
				return;
			}

			write(128 + b);
		}
	}


	void writeString(String aInput) throws IOException
	{
		writeUnsignedVarint(aInput.length());
		writeUTF(aInput);
	}


	void writeUTF(String aInput) throws IOException
	{
		for (int i = 0, len = aInput.length(); i < len; i++)
		{
			char c = aInput.charAt(i);
			if (c <= 0x007F)
			{
				write(c & 0x7F);
			}
			else if (c <= 0x07FF)
			{
				write(0xC0 | ((c >> 6) & 0x1F));
				write(0x80 | ((c) & 0x3F));
			}
			else
			{
				write(0xE0 | ((c >> 12) & 0x0F));
				write(0x80 | ((c >> 6) & 0x3F));
				write(0x80 | ((c) & 0x3F));
			}
		}
	}


	void writeType(BinaryType aType) throws IOException
	{
		write(aType.ordinal());
	}


	/* Encodes two digits/symbols into a single byte:
	 *
	 *  +   43
	 *  ,   44
	 *  -   45
	 *  .   46
	 *  0-9 48-57
	 *  e   58
	 */
	void writeBigDecimal(BigDecimal aValue) throws IOException
	{
		char[] s = aValue.toString().toCharArray();
		writeUnsignedVarint(s.length);
		for (int k = 0; k < s.length - 1;)
		{
			int a = s[k++];
			int b = s[k++];
			a = (a == 'e' || a == 'E' ? ':' : a) - '+';
			b = (b == 'e' || b == 'E' ? ':' : b) - '+';
			write((a << 4) + b);
		}
		if ((s.length & 1) == 1)
		{
			int a = s[s.length - 1];
			a = (a == 'e' || a == 'E' ? ':' : a) - '+';
			write(a << 4);
		}
	}


	/* Encodes two digits/symbols into a single byte:
	 *
	 *  +   43
	 *  ,   44
	 *  -   45
	 *  .   46
	 *  0-9 48-57
	 *  e   58
	 */
	void writeBigInteger(BigInteger aValue) throws IOException
	{
		char[] s = aValue.toString().toCharArray();
		writeUnsignedVarint(s.length);
		for (int k = 0; k < s.length - 1;)
		{
			int a = s[k++];
			int b = s[k++];
			a = (a == 'e' || a == 'E' ? ':' : a) - '+';
			b = (b == 'e' || b == 'E' ? ':' : b) - '+';
			write((a << 4) + b);
		}
		if ((s.length & 1) == 1)
		{
			int a = s[s.length - 1];
			a = (a == 'e' || a == 'E' ? ':' : a) - '+';
			write(a << 4);
		}
	}


	void writeString(IndexLookup<String> aLookup, String aValue) throws IOException
	{
		int ref = aLookup.lookup(aValue);
		if (ref == -1)
		{
			writeVarint(aValue.length());
			writeUTF(aValue);
			aLookup.add(aValue);
		}
		else
		{
			writeVarint(-ref - 1);
		}
	}


	@Override
	public void close() throws IOException
	{
		if (mOutputStream != null)
		{
			mOutputStream.close();
			mOutputStream = null;
		}
	}
}
