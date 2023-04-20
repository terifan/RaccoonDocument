package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map.Entry;


class BinaryEncoder implements AutoCloseable
{
	final static int VERSION = 1;

	private MurmurHash3 mChecksum;
	private OutputStream mOutputStream;


	public BinaryEncoder(OutputStream aOutputStream)
	{
		mOutputStream = aOutputStream;
	}


	void marshal(Object aObject) throws IOException
	{
		SupportedTypes type = SupportedTypes.identify(aObject);

		if (type == null)
		{
			throw new IllegalArgumentException("Unsupported type: " + aObject.getClass().getCanonicalName());
		}

		if (mChecksum == null)
		{
			mChecksum = new MurmurHash3(VERSION);
			writeToken(type, VERSION);
		}
		else
		{
			writeToken(type, getChecksumValue());
		}

		switch (type)
		{
			case DOCUMENT:
				writeDocument((Document)aObject);
				break;
			case ARRAY:
				writeArray((Array)aObject);
				break;
			default:
				writeValue(type, aObject);
				break;
		}
	}


	private void write(int aByte) throws IOException
	{
		mOutputStream.write(aByte);
		mChecksum.updateByte(aByte);
	}


	void writeBytes(byte[] aBuffer) throws IOException
	{
		mOutputStream.write(aBuffer);
		mChecksum.update(aBuffer, 0, aBuffer.length);
	}


	void writeDocument(Document aDocument) throws IOException
	{
		for (Entry<String, Object> entry : aDocument.entrySet())
		{
			Object value = entry.getValue();
			SupportedTypes type = SupportedTypes.identify(value);

			writeToken(type, entry.getKey().length());
			writeUTF(entry.getKey());
			writeValue(type, value);
		}

		terminate();
	}


	void writeArray(Array aArray) throws IOException
	{
		int elementCount = aArray.size();

		for (int offset = 0; offset < elementCount;)
		{
			SupportedTypes type = null;
			int runLen = 0;

			for (int i = offset; i < elementCount; i++, runLen++)
			{
				SupportedTypes nextType = SupportedTypes.identify(aArray.get(i));
				if (type != null && type != nextType)
				{
					break;
				}
				type = nextType;
			}

			writeToken(type, runLen);

			while (--runLen >= 0)
			{
				writeValue(type, aArray.get(offset++));
			}
		}

		terminate();
	}


	public void terminate() throws IOException
	{
		writeToken(SupportedTypes.TERMINATOR, getChecksumValue());
	}


	private void writeValue(SupportedTypes aType, Object aValue) throws IOException
	{
		aType.encoder.encode(this, aValue);
	}


	private void writeToken(SupportedTypes aType, int aValue) throws IOException
	{
		writeInterleaved(aType.ordinal(), aValue);
	}


	BinaryEncoder writeString(String aValue) throws IOException
	{
		writeUnsignedVarint(aValue.length());
		writeUTF(aValue);
		return this;
	}


	BinaryEncoder writeBuffer(byte[] aBuffer) throws IOException
	{
		writeUnsignedVarint(aBuffer.length);
		writeBytes(aBuffer);
		return this;
	}


	BinaryEncoder writeVarint(long aValue) throws IOException
	{
		aValue = (aValue << 1) ^ (aValue >> 63);

		for (;;)
		{
			int b = (int)(aValue & 127);
			aValue >>>= 7;

			if (aValue == 0)
			{
				write(b);
				return this;
			}

			write(128 + b);
		}
	}


	BinaryEncoder writeUnsignedVarint(long aValue) throws IOException
	{
		for (;;)
		{
			int b = (int)(aValue & 127);
			aValue >>>= 7;

			if (aValue == 0)
			{
				write(b);
				return this;
			}

			write(128 + b);
		}
	}


	private void writeUTF(String aInput) throws IOException
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


	void writeInterleaved(int aX, int aY) throws IOException
	{
		writeUnsignedVarint((shift(aX) << 1) | shift(aY));
	}


	/**
	 * note: Will not close the underlying stream!
	 */
	@Override
	public void close() throws IOException
	{
		mOutputStream = null;
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


	private int getChecksumValue()
	{
		return mChecksum.getValue() & 0b1111;
	}
}
