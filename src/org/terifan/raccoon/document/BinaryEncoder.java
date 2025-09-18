package org.terifan.raccoon.document;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;


// zip(
//   words
// )
// zip(
//   formats
// )
// document
//   document
//     value
//     document
//       value
//     value
//   value
//   array
//     document
//       value
//       value
//     document
//       value
//       value
class BinaryEncoder implements AutoCloseable
{
	final static int VERSION = 1;

	private OutputStream mOutputStream;
	private final byte[] mWriteBuffer = new byte[8];
	private final Function<Path, Boolean> mFilter;
	private final HashMap<String, Integer> mValues;
	private final HashMap<String, Integer> mFormats;


	public BinaryEncoder(OutputStream aOutputStream, Function<Path, Boolean> aFilter)
	{
		mOutputStream = aOutputStream;
		mFilter = aFilter;
		mValues = new HashMap<>();
		mFormats = new HashMap<>();
	}


	void marshal(Object aObject) throws IOException
	{
		BinaryCodec type = BinaryCodec.identify(aObject);

		if (type == null)
		{
			throw new IllegalArgumentException("Unsupported type: " + aObject.getClass().getCanonicalName());
		}

		writeToken(type, VERSION);

		State state = new State(null, null);
		if (aObject instanceof Document v)
		{
			writeDocument(v, state);
		}
		else if (aObject instanceof Array v)
		{
			writeArray(v, state);
		}
		else
		{
			writeValue(type, aObject, state);
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (DataOutputStream dos = new DataOutputStream(new DeflaterOutputStream(baos, new Deflater(Deflater.DEFAULT_COMPRESSION))))
//		try (DataOutputStream dos = new DataOutputStream(baos))
		{
			dos.writeShort(mValues.size());
			for (Entry<String, Integer> entry : mValues.entrySet())
			{
				dos.writeInt(entry.getKey().length());
				dos.writeUTF(entry.getKey());
			}
		}

		writeVarint(baos.size());
		writeBytes(baos.toByteArray());

		baos = new ByteArrayOutputStream();
		try (DataOutputStream dos = new DataOutputStream(new DeflaterOutputStream(baos, new Deflater(Deflater.DEFAULT_COMPRESSION))))
//		try (DataOutputStream dos = new DataOutputStream(baos))
		{
			dos.writeShort(mFormats.size());
			for (Entry<String, Integer> entry : mFormats.entrySet())
			{
				dos.writeInt(entry.getKey().length());
				dos.write(Base64.getDecoder().decode(entry.getKey()));
			}
		}

		writeVarint(baos.size());
		writeBytes(baos.toByteArray());

		System.out.println("values=" + mValues.size());
		System.out.println("formats=" + mFormats.size());
	}


	BinaryEncoder writeDocument(Document aDocument, State aState) throws IOException
	{
		ByteArrayOutputStream format = new ByteArrayOutputStream();
		for (Entry<String, Object> entry : aDocument.entrySet())
		{
			String key = entry.getKey();
			Object value = entry.getValue();
			BinaryCodec type = BinaryCodec.identify(value);
			format.write(type.ordinal());
			format.write(key.length());
			format.writeBytes(key.getBytes());
		}

		writeVarint(mFormats.computeIfAbsent(Base64.getEncoder().encodeToString(format.toByteArray()), k -> mFormats.size()));

		for (Entry<String, Object> entry : aDocument.entrySet())
		{
			String key = entry.getKey();
			Object value = entry.getValue();

			if (value instanceof Collection)
			{
				aState = aState.enter(key);
			}

			BinaryCodec type = BinaryCodec.identify(value);

			if (type == BinaryCodec.STRING)
			{
				value = mValues.computeIfAbsent((String)value, k -> mValues.size());
			}

//			System.out.printf("%15s %s%n", type, key);

			writeValue(type, value, aState);

			aState.put(key, value instanceof Collection ? "#REF" : value);

			if (value instanceof Collection)
			{
				aState = aState.exit();
			}
		}

		terminate();
		return this;
	}


	BinaryEncoder writeArray(Array aArray, State aState) throws IOException
	{
		for (int offset = 0, elementCount = aArray.size(); offset < elementCount;)
		{
			aState = aState.enter(offset);

			ArrayList<Object> pending = new ArrayList<>();
			BinaryCodec type = null;

			for (int i = offset; i < elementCount; i++)
			{
				Object value = aArray.get(i);
				BinaryCodec nextType = BinaryCodec.identify(value);

				if (type != nextType && type != null)
				{
					break;
				}

				type = nextType;
				pending.add(value);
			}

//			System.out.printf("%15s %s%n", type, pending.size());
			writeToken(type, pending.size());

			for (int i = 0; i < pending.size(); i++, offset++)
			{
				Object value = pending.get(i);

				if (type == BinaryCodec.STRING)
				{
					value = mValues.computeIfAbsent(value.toString(), k -> mValues.size());
				}

				writeValue(type, value, aState);

				aState.put(offset, value instanceof Collection ? "#REF" : value);
			}

			aState = aState.exit();
		}

		terminate();
		return this;
	}


	public void terminate() throws IOException
	{
		writeToken(BinaryCodec.TERMINATOR, 0);
	}


	private void writeValue(BinaryCodec aType, Object aValue, State aState) throws IOException
	{
		aType.encoder.encode(this, aState, aValue);
	}


	void writeToken(BinaryCodec aType, int aValue) throws IOException
	{
		writeInterleaved(aType.ordinal(), aValue);
	}


	BinaryEncoder writeByte(int aValue) throws IOException
	{
		mOutputStream.write(aValue);
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
