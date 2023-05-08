package org.terifan.raccoon.document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map.Entry;


class BinaryEncoder implements AutoCloseable
{
	final static int VERSION = 1;

	private MurmurHash3 mChecksum;
	private OutputStream mOutputStream;
	private boolean mUseFixedPrimitives = true;
	private final byte[] writeBuffer = new byte[8];


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


	void writeDocument(Document aDocument) throws IOException
	{
		for (Entry<String, Object> entry : aDocument.entrySet())
		{
			String key = entry.getKey();
			Object value = entry.getValue();
			SupportedTypes type = SupportedTypes.identify(value);

//			if (mUseFixedPrimitives && (type == SupportedTypes.SHORT || type == SupportedTypes.INT || type == SupportedTypes.LONG || type == SupportedTypes.FLOAT || type == SupportedTypes.DOUBLE))
//			{
//				ByteArrayOutputStream baos = new ByteArrayOutputStream();
//				BinaryEncoder encoder = new BinaryEncoder(baos);
//				type.encoder.encode(encoder, value);
//				if (type == SupportedTypes.SHORT && baos.size() >= 2)
//				{
//					type = SupportedTypes.FIXEDSHORT;
//				}
//				if (type == SupportedTypes.INT && baos.size() >= 4)
//				{
//					type = SupportedTypes.FIXEDINT;
//				}
//				if (type == SupportedTypes.LONG && baos.size() >= 8)
//				{
//					type = SupportedTypes.FIXEDLONG;
//				}
//				if (type == SupportedTypes.FLOAT && baos.size() >= 4)
//				{
//					type = SupportedTypes.FIXEDFLOAT;
//				}
//				if (type == SupportedTypes.DOUBLE && baos.size() >= 8)
//				{
//					type = SupportedTypes.FIXEDDOUBLE;
//				}
//			}

			writeToken(type, key.length());
			writeUTF(key);
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
				if (type != nextType && type != null)
				{
					break;
				}
				type = nextType;
			}

//			if (mUseFixedPrimitives && (type == SupportedTypes.SHORT || type == SupportedTypes.INT || type == SupportedTypes.LONG || type == SupportedTypes.FLOAT || type == SupportedTypes.DOUBLE))
//			{
//				ByteArrayOutputStream baos = new ByteArrayOutputStream();
//				BinaryEncoder encoder = new BinaryEncoder(baos);
//				for (int i = 0; i < runLen; i++)
//				{
//					type.encoder.encode(encoder, aArray.get(offset + i));
//				}
//				if (type == SupportedTypes.SHORT && baos.size() >= 2 * runLen)
//				{
//					type = SupportedTypes.FIXEDSHORT;
//				}
//				if (type == SupportedTypes.INT && baos.size() >= 4 * runLen)
//				{
//					type = SupportedTypes.FIXEDINT;
//				}
//				if (type == SupportedTypes.LONG && baos.size() >= 8 * runLen)
//				{
//					type = SupportedTypes.FIXEDLONG;
//				}
//				if (type == SupportedTypes.FLOAT && baos.size() >= 4 * runLen)
//				{
//					type = SupportedTypes.FIXEDFLOAT;
//				}
//				if (type == SupportedTypes.DOUBLE && baos.size() >= 8 * runLen)
//				{
//					type = SupportedTypes.FIXEDDOUBLE;
//				}
//			}

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


	void writeByte(int aValue) throws IOException
	{
		mOutputStream.write(aValue);
		if (mChecksum != null)
		{
			mChecksum.updateByte(aValue);
		}
	}


	void writeShort(short aValue) throws IOException
	{
		writeBuffer[0] = (byte)(aValue >>> 8);
		writeBuffer[1] = (byte)(aValue >>> 0);
		writeBytes(writeBuffer, 0, 2);
	}


	void writeInt(int aValue) throws IOException
	{
		writeBuffer[0] = (byte)(aValue >>> 24);
		writeBuffer[1] = (byte)(aValue >>> 16);
		writeBuffer[2] = (byte)(aValue >>> 8);
		writeBuffer[3] = (byte)(aValue >>> 0);
		writeBytes(writeBuffer, 0, 4);
	}


	BinaryEncoder writeLong(long aValue) throws IOException
	{
		writeBuffer[0] = (byte)(aValue >>> 56);
		writeBuffer[1] = (byte)(aValue >>> 48);
		writeBuffer[2] = (byte)(aValue >>> 40);
		writeBuffer[3] = (byte)(aValue >>> 32);
		writeBuffer[4] = (byte)(aValue >>> 24);
		writeBuffer[5] = (byte)(aValue >>> 16);
		writeBuffer[6] = (byte)(aValue >>> 8);
		writeBuffer[7] = (byte)(aValue >>> 0);
		writeBytes(writeBuffer, 0, 8);
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


	private void writeUTF(String aInput) throws IOException
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
	}


	void writeInterleaved(int aX, int aY) throws IOException
	{
		writeUnsignedVarint((shift(aX) << 1) | shift(aY));
	}


	/**
	 * Does not close the underlying stream.
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
