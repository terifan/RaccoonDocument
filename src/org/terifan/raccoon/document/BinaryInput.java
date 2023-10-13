package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.InputStream;
import static org.terifan.raccoon.document.BinaryEncoder.VERSION;
import static org.terifan.raccoon.document.SupportedTypes.ARRAY;
import static org.terifan.raccoon.document.SupportedTypes.DOCUMENT;
import static org.terifan.raccoon.document.SupportedTypes.TERMINATOR;


abstract class BinaryInput
{
	private final byte[] mReadBuffer = new byte[8];
	private MurmurHash3 mChecksum;
	private InputStream mInputStream;


	BinaryInput(InputStream aInputStream)
	{
		mInputStream = aInputStream;
	}


	Token readToken() throws IOException
	{
		boolean first = mChecksum == null;
		if (first)
		{
			mChecksum = new MurmurHash3(VERSION);
		}

		int checksum = getChecksumValue();
		long params = readInterleaved();

		Token token = new Token();
		token.checksum = checksum;
		token.value = (int)(params >>> 32);
		token.type = SupportedTypes.values()[(int)params];

		if (first && token.value != VERSION)
		{
			throw new StreamException("Unsupported stream encoding version: " + token.value);
		}
		if (token.type == SupportedTypes.TERMINATOR && token.value != token.checksum)
		{
			throw new StreamException("Checksum error in data stream");
		}

		return token;
	}


	static class Token
	{
		int value;
		int checksum;
		SupportedTypes type;


		@Override
		public String toString()
		{
			return type.name();
		}
	}


	Document readDocument(Document aDocument) throws IOException
	{
		for (;;)
		{
			Token token = readToken();
			if (token.type == SupportedTypes.TERMINATOR)
			{
				if (token.value != token.checksum)
				{
					throw new StreamException("Checksum error in data stream");
				}
				break;
			}

			String key = readUTF(token.value);

			aDocument.putImpl(key, readValue(token.type));
		}

		return aDocument;
	}


	Array readArray(Array aArray) throws IOException
	{
		for (;;)
		{
			Token token = readToken();

			if (token.type == SupportedTypes.TERMINATOR)
			{
				if (token.value != token.checksum)
				{
					throw new StreamException("Checksum error in data stream");
				}
				break;
			}

			for (int i = 0; i < token.value; i++)
			{
				aArray.add(readValue(token.type));
			}
		}

		return aArray;
	}


	abstract Object readValue(SupportedTypes aType) throws IOException;


	void close() throws IOException
	{
		mInputStream = null;
	}


	int getChecksumValue()
	{
		return mChecksum.getValue4bits();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


//	DocumentEntity unmarshal() throws IOException
//	{
//		Token token = readToken();
//		if(token.type == SupportedTypes.ARRAY)
//			return readArray(new Array());
//		return readDocument(new Document());
//	}
	Object unmarshal() throws IOException
	{
		BinaryInput.Token token = readToken();

		switch (token.type)
		{
			case DOCUMENT:
				return readDocument(new Document());
			case ARRAY:
				return readArray(new Array());
			case TERMINATOR:
				return token.type;
			default:
				return readValue(token.type);
		}
	}


	int readByte() throws IOException
	{
		int c = mInputStream.read();
		if (c == -1)
		{
			throw new StreamException("Premature end of stream");
		}
		mChecksum.updateByte(c);
		return c;
	}


	short readShort() throws IOException
	{
		return (short)((readByte() << 8) | readByte());
	}


	int readInt() throws IOException
	{
		readBytes(mReadBuffer, 0, 4);
		return ((mReadBuffer[0] & 0xff) << 24)
			+ ((mReadBuffer[1] & 0xff) << 16)
			+ ((mReadBuffer[2] & 0xff) << 8)
			+ ((mReadBuffer[3] & 0xff) << 0);
	}


	long readLong() throws IOException
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


	byte[] readBytes(byte[] aBuffer) throws IOException
	{
		int len = mInputStream.read(aBuffer);
		mChecksum.updateBytes(aBuffer, 0, len);
		return aBuffer;
	}


	byte[] readBytes(byte[] aBuffer, int aOffset, int aLength) throws IOException
	{
		int len = mInputStream.read(aBuffer, aOffset, aLength);
		mChecksum.updateBytes(aBuffer, aOffset, aLength);
		if (len != aLength)
		{
			throw new IOException("Error reading from underlying stream.");
		}
		return aBuffer;
	}


	long readVarint() throws IOException
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


	long readUnsignedVarint() throws IOException
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


	String readUTF(int aLength) throws IOException
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


	long readInterleaved() throws IOException
	{
		long p = readUnsignedVarint();
		return (reverseShift(p) << 32) | reverseShift(p >>> 1);
	}


	static long reverseShift(long aWord)
	{
		aWord &= 0x5555555555555555L;

		aWord = (aWord | (aWord >> 1)) & 0x3333333333333333L;
		aWord = (aWord | (aWord >> 2)) & 0x0f0f0f0f0f0f0f0fL;
		aWord = (aWord | (aWord >> 4)) & 0x00ff00ff00ff00ffL;
		aWord = (aWord | (aWord >> 8)) & 0x0000ffff0000ffffL;
		aWord = (aWord | (aWord >> 16)) & 0x00000000ffffffffL;

		return aWord;
	}
}
