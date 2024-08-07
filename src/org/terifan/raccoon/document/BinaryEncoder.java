package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map.Entry;
import java.util.function.Function;
import static org.terifan.raccoon.document.BinaryCodec.ARRAY;
import static org.terifan.raccoon.document.BinaryCodec.DOCUMENT;


class BinaryEncoder implements AutoCloseable
{
	final static int VERSION = 1;

	private MurmurHash3 mChecksum;
	private OutputStream mOutputStream;
	private final byte[] mWriteBuffer = new byte[8];
	private final Function<Object, Boolean> mFilter;

	final Dictionary mDictionary;


	public BinaryEncoder(OutputStream aOutputStream, Function<Object, Boolean> aFilter)
	{
		this(aOutputStream, aFilter, null);
	}


	public BinaryEncoder(OutputStream aOutputStream, Function<Object, Boolean> aFilter, Dictionary aDictionary)
	{
		mOutputStream = aOutputStream;
		mFilter = aFilter;
		mDictionary = aDictionary;
	}


	void marshal(Object aObject) throws IOException
	{
		BinaryCodec type = BinaryCodec.identify(aObject);

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


	BinaryEncoder writeDocument(Document aDocument) throws IOException
	{
		for (Entry<String, Object> entry : aDocument.entrySet())
		{
			String key = entry.getKey();

			if (mFilter.apply(key))
			{
				Object value = entry.getValue();
				BinaryCodec type = BinaryCodec.identify(value);

				if (mDictionary != null)
				{
					if (mDictionary.encode(value) != null)
					{
						type = BinaryCodec.DICTIONARY;
					}

					Integer index = mDictionary.get(key);
					if (index != null)
					{
						writeToken(type, 2 * index + 1);
					}
					else
					{
						writeToken(type, 2 * key.length());
						writeUTF(key);
					}
				}
				else
				{
					writeToken(type, 2 * key.length());
					writeUTF(key);
				}

				writeValue(type, value);
			}
		}

		terminate();
		return this;
	}


	BinaryEncoder writeArray(Array aArray) throws IOException
	{
		int elementCount = aArray.size();

		for (int offset = 0; offset < elementCount;)
		{
			BinaryCodec type = null;
			int runLen = 0;

			for (int i = offset; i < elementCount; i++, runLen++)
			{
				BinaryCodec nextType = BinaryCodec.identify(aArray.get(i));
				if (type != nextType && type != null)
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
		return this;
	}


	public void terminate() throws IOException
	{
		writeToken(BinaryCodec.TERMINATOR, getChecksumValue());
	}


	private void writeValue(BinaryCodec aType, Object aValue) throws IOException
	{
		aType.encoder.encode(this, aValue);
	}


	private void writeToken(BinaryCodec aType, int aValue) throws IOException
	{
		writeInterleaved(aType.ordinal(), aValue);
	}


	BinaryEncoder writeByte(int aValue) throws IOException
	{
		mOutputStream.write(aValue);
		if (mChecksum != null)
		{
			mChecksum.updateByte(aValue);
		}
		return this;
	}


	BinaryEncoder writeShort(short aValue) throws IOException
	{
		mWriteBuffer[0] = (byte)(aValue >>> 8);
		mWriteBuffer[1] = (byte)(aValue);
		writeBytes(mWriteBuffer, 0, 2);
		return this;
	}


	BinaryEncoder writeInt(int aValue) throws IOException
	{
		mWriteBuffer[0] = (byte)(aValue >>> 24);
		mWriteBuffer[1] = (byte)(aValue >>> 16);
		mWriteBuffer[2] = (byte)(aValue >>> 8);
		mWriteBuffer[3] = (byte)(aValue);
		writeBytes(mWriteBuffer, 0, 4);
		return this;
	}


	BinaryEncoder writeLong(long aValue) throws IOException
	{
		mWriteBuffer[0] = (byte)(aValue >>> 56);
		mWriteBuffer[1] = (byte)(aValue >>> 48);
		mWriteBuffer[2] = (byte)(aValue >>> 40);
		mWriteBuffer[3] = (byte)(aValue >>> 32);
		mWriteBuffer[4] = (byte)(aValue >>> 24);
		mWriteBuffer[5] = (byte)(aValue >>> 16);
		mWriteBuffer[6] = (byte)(aValue >>> 8);
		mWriteBuffer[7] = (byte)(aValue);
		writeBytes(mWriteBuffer, 0, 8);
		return this;
	}


	void writeBytes(byte[] aBuffer) throws IOException
	{
		writeBytes(aBuffer, 0, aBuffer.length);
	}


	void writeBytes(byte[] aBuffer, int aOffset, int aLength) throws IOException
	{
		mOutputStream.write(aBuffer, aOffset, aLength);
		mChecksum.updateBytes(aBuffer, aOffset, aLength);
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
				writeByte(b);
				return this;
			}

			writeByte(128 + b);
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
				writeByte(b);
				return this;
			}

			writeByte(128 + b);
		}
	}


	BinaryEncoder writeUTF(String aInput) throws IOException
	{
		for (int i = 0, len = aInput.length(); i < len; i++)
		{
			char c = aInput.charAt(i);
			if (c <= 0x007F)
			{
				writeByte(c & 0x7F);
			}
			else if (c <= 0x07FF)
			{
				writeByte(0xC0 | ((c >> 6) & 0x1F));
				writeByte(0x80 | ((c) & 0x3F));
			}
			else
			{
				writeByte(0xE0 | ((c >> 12) & 0x0F));
				writeByte(0x80 | ((c >> 6) & 0x3F));
				writeByte(0x80 | ((c) & 0x3F));
			}
		}
		return this;
	}


	void writeInterleaved(int aX, int aY) throws IOException
	{
		writeUnsignedVarint((shift(aX) << 1) | shift(aY));
	}


	/**
	 * note: this implementation will not close the underlying stream.
	 */
	@Override
	public void close() throws IOException
	{
		mOutputStream = null;
		mChecksum = null;
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
		return mChecksum.getValue4bits();
	}


//	private Integer parseInt(String aKey)
//	{
//		int v = 0;
//		for (int i = 0; i < aKey.length(); i++)
//		{
//			char c = aKey.charAt(i);
//			if (c < '0' || c > '9' || i == 0 && c == '0')
//			{
//				return null;
//			}
//			v *= 10;
//			v += c - '0';
//		}
//		return v;
//	}
}
