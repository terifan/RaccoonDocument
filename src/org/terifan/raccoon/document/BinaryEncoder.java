package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Map.Entry;
import static org.terifan.raccoon.document.SupportedTypes.ARRAY;
import static org.terifan.raccoon.document.SupportedTypes.DOCUMENT;


class BinaryEncoder implements AutoCloseable
{
	final static int VERSION = 1;

	private MurmurHash3 mChecksum;
	private OutputStream mOutputStream;
	private final byte[] mWriteBuffer = new byte[8];


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


	BinaryEncoder writeDocument(Document aDocument) throws IOException
	{
		for (Entry<String, Object> entry : aDocument.entrySet())
		{
			String key = entry.getKey();
			Object value = entry.getValue();
			SupportedTypes type = SupportedTypes.identify(value);

			writeToken(type, key.length());
			writeUTF(key);
			writeValue(type, value);
		}

		terminate();
		return this;
	}


	BinaryEncoder writeArray(Array aArray) throws IOException
	{
		int elementCount = aArray.size();

		for (int offset = 0; offset < elementCount;)
		{
			SupportedTypes type = null;
			int runLen = 0;
//			int esimatedLen = 0;

			for (int i = offset; i < elementCount; i++, runLen++)
			{
				Object v = aArray.get(i);
				SupportedTypes nextType = SupportedTypes.identify(v);
				if (type != nextType && type != null)
				{
					break;
				}
				type = nextType;

//				if (type == SupportedTypes.SHORT || type == SupportedTypes.INT || type == SupportedTypes.LONG)
//				{
//				}
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
		mWriteBuffer[1] = (byte)(aValue >>> 0);
		writeBytes(mWriteBuffer, 0, 2);
		return this;
	}


	BinaryEncoder writeInt(int aValue) throws IOException
	{
		mWriteBuffer[0] = (byte)(aValue >>> 24);
		mWriteBuffer[1] = (byte)(aValue >>> 16);
		mWriteBuffer[2] = (byte)(aValue >>> 8);
		mWriteBuffer[3] = (byte)(aValue >>> 0);
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
		mWriteBuffer[7] = (byte)(aValue >>> 0);
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
		return mChecksum.getValue4bits();
	}


	public static void main(String ... args)
	{
		try
		{
			String s = """
			{
				 "totalSize": 215730, "blockSize": 1048576, "pointers": [{
					 "adr": [2626],
					 "blk": 3,
					 "lvl": 0,
					 "chk": 0,
					 "cmp": 0,
					 "alc": 217088,
					 "phy": 215730,
					 "log": 215730,
					 "key": [-1893756345, -178735963, 151717756, 426488884],
					 "sig": [-59560821, 710175849, 1026987985, -295038902],
					 "gen": 2
				 }]
			 }
			""";

			Document doc = Document.of(s);
			System.out.println(doc);

			System.out.println(doc.toByteArray().length);
			System.out.println(doc.toJson().length());

			for (byte b : doc.toByteArray()) System.out.print(b<32?'.':(char)b);
			System.out.println();
			System.out.println(Base64.getUrlEncoder().encodeToString(doc.toByteArray()).replace("=", ""));

			String t = """
			{
				 "totalSize": 215730, "blockSize": 1048576, "pointers": [[
					 [2626],
					 3,
					 0,
					 0,
					 0,
					 217088,
					 215730,
					 215730,
					 [-1893756345, -178735963, 151717756, 426488884],
					 [-59560821, 710175849, 1026987985, -295038902],
					 2
				 ]]
			 }
			""";

			doc = Document.of(t);
			System.out.println(doc);

			System.out.println(doc.toByteArray().length);
			System.out.println(doc.toJson().length());

			for (byte b : doc.toByteArray()) System.out.print(b<32?'.':(char)b);
			System.out.println();

			System.out.println(Base64.getUrlEncoder().encodeToString(doc.toByteArray()).replace("=", ""));
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}


//package org.terifan.raccoon.document;
//
//import java.io.IOException;
//import java.io.OutputStream;
//import java.util.HashMap;
//import java.util.Map.Entry;
//
//
//class BinaryEncoder implements AutoCloseable
//{
//	final static int VERSION = 1;
//
//	private MurmurHash3 mChecksum;
//	private OutputStream mOutputStream;
//	private final byte[] mWriteBuffer = new byte[8];
//	private HashMap<String, Integer> mKeyToIndex = new HashMap<>();
//	private HashMap<String, Integer> mStuctToIndex = new HashMap<>();
//	private HashMap<Object, Integer> mValueToIndex = new HashMap<>();
//
//
//	public BinaryEncoder(OutputStream aOutputStream)
//	{
//		mOutputStream = aOutputStream;
//	}
//
//
//	void marshal(Object aObject) throws IOException
//	{
//		SupportedTypes type = SupportedTypes.identify(aObject);
//
//		if (type == null)
//		{
//			throw new IllegalArgumentException("Unsupported type: " + aObject.getClass().getCanonicalName());
//		}
//
//		if (mChecksum == null)
//		{
//			mChecksum = new MurmurHash3(VERSION);
//			writeToken(type, VERSION);
//		}
//		else
//		{
//			writeToken(type, getChecksumValue());
//		}
//
//		switch (type)
//		{
//			case DOCUMENT:
//				writeDocument((Document)aObject);
//				break;
//			case ARRAY:
//				writeArray((Array)aObject);
//				break;
//			default:
//				writeValue(type, aObject);
//				break;
//		}
//	}
//
//
//	void writeDocument(Document aDocument) throws IOException
//	{
//		String structFormat = "";
//		for (Entry<String, Object> entry : aDocument.entrySet())
//		{
//			structFormat += SupportedTypes.identify(entry.getValue()).ordinal() + ":" + entry.getKey() + ";";
//		}
//
//		Integer index = mStuctToIndex.get(structFormat);
//		if (index != null)
//		{
//			writeVarint(index);
//
//			for (Entry<String, Object> entry : aDocument.entrySet())
//			{
//				Object value = entry.getValue();
//				SupportedTypes type = SupportedTypes.identify(value);
//
//				writeValue(type, value);
//			}
//		}
//		else
//		{
//			writeVarint(-1);
//
//			mStuctToIndex.put(structFormat, mStuctToIndex.size());
//
//			for (Entry<String, Object> entry : aDocument.entrySet())
//			{
//				String key = entry.getKey();
//				Object value = entry.getValue();
//				SupportedTypes type = SupportedTypes.identify(value);
//
//				boolean done = false;
//
//				index = mKeyToIndex.get(type.ordinal() + ":" + key);
//				if (index != null)
//				{
//					writeToken(SupportedTypes.REF, index);
//				}
//				else
//				{
//					String valueKey = value == null ? null : value.toString();
//					{
//						index = mValueToIndex.get(valueKey);
//						if (index != null)
//						{
//							writeToken(SupportedTypes.REFVALUE, key.length());
//							writeUnsignedVarint(index);
//							done = true;
//						}
//						else
//						{
//							mValueToIndex.put(valueKey, mValueToIndex.size());
//						}
//					}
//
//					if (!done)
//					{
//						writeToken(type, key.length());
//						writeUTF(key);
//					}
//
//					mKeyToIndex.put(type.ordinal() + ":" + key, mKeyToIndex.size());
//				}
//
//				if (!done)
//				{
//					writeValue(type, value);
//				}
//			}
//		}
//
//		terminate();
//	}
//
//
//	void writeArray(Array aArray) throws IOException
//	{
//		int elementCount = aArray.size();
//
//		for (int offset = 0; offset < elementCount;)
//		{
//			SupportedTypes type = null;
//			int runLen = 0;
////			int esimatedLen = 0;
//
//			for (int i = offset; i < elementCount; i++, runLen++)
//			{
//				Object v = aArray.get(i);
//				SupportedTypes nextType = SupportedTypes.identify(v);
//				if (type != nextType && type != null)
//				{
//					break;
//				}
//				type = nextType;
//
////				if (type == SupportedTypes.SHORT || type == SupportedTypes.INT || type == SupportedTypes.LONG)
////				{
////				}
//
//				mValueToIndex.computeIfAbsent(v==null?null:v.toString(), k->mValueToIndex.size());
//			}
//
//			writeToken(type, runLen);
//
//			while (--runLen >= 0)
//			{
//				writeValue(type, aArray.get(offset++));
//			}
//		}
//
//		terminate();
//	}
//
//
//	public void terminate() throws IOException
//	{
//		writeToken(SupportedTypes.TERMINATOR, getChecksumValue());
//	}
//
//
//	private void writeValue(SupportedTypes aType, Object aValue) throws IOException
//	{
//		aType.encoder.encode(this, aValue);
//	}
//
//
//	private void writeToken(SupportedTypes aType, int aValue) throws IOException
//	{
//		writeInterleaved(aType.ordinal(), aValue);
//	}
//
//
//	void writeByte(int aValue) throws IOException
//	{
//		mOutputStream.write(aValue);
//		if (mChecksum != null)
//		{
//			mChecksum.updateByte(aValue);
//		}
//	}
//
//
//	BinaryEncoder writeShort(short aValue) throws IOException
//	{
//		mWriteBuffer[0] = (byte)(aValue >>> 8);
//		mWriteBuffer[1] = (byte)(aValue >>> 0);
//		writeBytes(mWriteBuffer, 0, 2);
//		return this;
//	}
//
//
//	BinaryEncoder writeInt(int aValue) throws IOException
//	{
//		mWriteBuffer[0] = (byte)(aValue >>> 24);
//		mWriteBuffer[1] = (byte)(aValue >>> 16);
//		mWriteBuffer[2] = (byte)(aValue >>> 8);
//		mWriteBuffer[3] = (byte)(aValue >>> 0);
//		writeBytes(mWriteBuffer, 0, 4);
//		return this;
//	}
//
//
//	BinaryEncoder writeLong(long aValue) throws IOException
//	{
//		mWriteBuffer[0] = (byte)(aValue >>> 56);
//		mWriteBuffer[1] = (byte)(aValue >>> 48);
//		mWriteBuffer[2] = (byte)(aValue >>> 40);
//		mWriteBuffer[3] = (byte)(aValue >>> 32);
//		mWriteBuffer[4] = (byte)(aValue >>> 24);
//		mWriteBuffer[5] = (byte)(aValue >>> 16);
//		mWriteBuffer[6] = (byte)(aValue >>> 8);
//		mWriteBuffer[7] = (byte)(aValue >>> 0);
//		writeBytes(mWriteBuffer, 0, 8);
//		return this;
//	}
//
//
//	void writeBytes(byte[] aBuffer) throws IOException
//	{
//		writeBytes(aBuffer, 0, aBuffer.length);
//	}
//
//
//	void writeBytes(byte[] aBuffer, int aOffset, int aLength) throws IOException
//	{
//		mOutputStream.write(aBuffer, aOffset, aLength);
//		mChecksum.updateBytes(aBuffer, aOffset, aLength);
//	}
//
//
//	BinaryEncoder writeVarint(long aValue) throws IOException
//	{
//		aValue = (aValue << 1) ^ (aValue >> 63);
//
//		for (;;)
//		{
//			int b = (int)(aValue & 127);
//			aValue >>>= 7;
//
//			if (aValue == 0)
//			{
//				writeByte(b);
//				return this;
//			}
//
//			writeByte(128 + b);
//		}
//	}
//
//
//	BinaryEncoder writeUnsignedVarint(long aValue) throws IOException
//	{
//		for (;;)
//		{
//			int b = (int)(aValue & 127);
//			aValue >>>= 7;
//
//			if (aValue == 0)
//			{
//				writeByte(b);
//				return this;
//			}
//
//			writeByte(128 + b);
//		}
//	}
//
//
//	BinaryEncoder writeUTF(String aInput) throws IOException
//	{
//		for (int i = 0, len = aInput.length(); i < len; i++)
//		{
//			char c = aInput.charAt(i);
//			if (c <= 0x007F)
//			{
//				writeByte(c & 0x7F);
//			}
//			else if (c <= 0x07FF)
//			{
//				writeByte(0xC0 | ((c >> 6) & 0x1F));
//				writeByte(0x80 | ((c) & 0x3F));
//			}
//			else
//			{
//				writeByte(0xE0 | ((c >> 12) & 0x0F));
//				writeByte(0x80 | ((c >> 6) & 0x3F));
//				writeByte(0x80 | ((c) & 0x3F));
//			}
//		}
//		return this;
//	}
//
//
//	void writeInterleaved(int aX, int aY) throws IOException
//	{
//		writeUnsignedVarint((shift(aX) << 1) | shift(aY));
//	}
//
//
//	/**
//	 * Does not close the underlying stream.
//	 */
//	@Override
//	public void close() throws IOException
//	{
//		mOutputStream = null;
//	}
//
//
//	private static long shift(long aWord)
//	{
//		aWord &= 0xffffffffL;
//
//		aWord = (aWord | (aWord << 16)) & 0x0000ffff0000ffffL;
//		aWord = (aWord | (aWord << 8)) & 0x00ff00ff00ff00ffL;
//		aWord = (aWord | (aWord << 4)) & 0x0f0f0f0f0f0f0f0fL;
//		aWord = (aWord | (aWord << 2)) & 0x3333333333333333L;
//		aWord = (aWord | (aWord << 1)) & 0x5555555555555555L;
//
//		return aWord;
//	}
//
//
//	private int getChecksumValue()
//	{
//		return mChecksum.getValue4();
//	}
//}
