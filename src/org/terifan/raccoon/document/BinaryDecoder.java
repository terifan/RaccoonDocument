package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.InputStream;
import static org.terifan.raccoon.document.BinaryEncoder.VERSION;


class BinaryDecoder implements AutoCloseable
{
	private MurmurHash3 mChecksum;
	private InputStream mInputStream;


	BinaryDecoder(InputStream aInputStream)
	{
		mInputStream = aInputStream;
	}


	void unmarshal(KeyValueCollection aContainer) throws IOException
	{
		if (mChecksum == null)
		{
			mChecksum = new MurmurHash3(VERSION);
		}

		Token token = readToken();

		if (token.value != BinaryEncoder.VERSION)
		{
			throw new StreamException("Unsupported stream encoding version: " + token.value);
		}

		if (aContainer instanceof Document)
		{
			if (token.type == SupportedTypes.ARRAY)
			{
				throw new StreamException("Attempt to unmarshal a Document failed; found an Array in the binary stream.");
			}
			else if (token.type != SupportedTypes.DOCUMENT)
			{
				throw new StreamException("Stream corrupted.");
			}

			readDocument((Document)aContainer);
		}
		else
		{
			if (token.type == SupportedTypes.DOCUMENT)
			{
				throw new StreamException("Attempt to unmarshal an Array failed; found a Document in the binary stream.");
			}
			else if (token.type != SupportedTypes.ARRAY)
			{
				throw new StreamException("Stream corrupted.");
			}

			readArray((Array)aContainer);
		}
	}


	Object unmarshal() throws IOException
	{
		boolean first = mChecksum == null;
		if (first)
		{
			mChecksum = new MurmurHash3(VERSION);
		}

		Token token = readToken();

		if (first)
		{
			if (token.value != VERSION)
			{
				throw new StreamException("Unsupported stream encoding version: " + token.value);
			}
		}
		else if (token.value != token.checksum)
		{
			throw new StreamException("Checksum error in data stream");
		}

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


	private int readByte() throws IOException
	{
		int c = mInputStream.read();
		if (c == -1)
		{
			throw new StreamException("Premature end of stream");
		}
		mChecksum.updateByte(c);
		return c;
	}


	byte[] readBytes(byte[] aBuffer) throws IOException
	{
		int len = mInputStream.read(aBuffer);
		mChecksum.updateBytes(aBuffer, 0, len);
		return aBuffer;
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


	private Object readValue(SupportedTypes aType) throws IOException
	{
		return aType.decoder.decode(this);
	}


	private Token readToken() throws IOException
	{
		int checksum = getChecksumValue();
		long params = readInterleaved();

		try
		{
			Token token = new Token();
			token.checksum = checksum;
			token.value = (int)(params >>> 32);
			token.type = SupportedTypes.values()[(int)params];
			return token;
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			throw new StreamException("Type parameter out of range");
		}
	}


	private static class Token
	{
		int value;
		int checksum;
		SupportedTypes type;
	}


	String readString() throws IOException
	{
		return readUTF((int)readUnsignedVarint());
	}


	byte[] readBuffer() throws IOException
	{
		return readBytes(new byte[(int)readUnsignedVarint()]);
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


	private String readUTF(int aLength) throws IOException
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


	@Override
	public void close() throws IOException
	{
		mInputStream = null;
	}


	private int getChecksumValue()
	{
		return mChecksum.getValue() & 0b1111;
	}
}
