package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import static org.terifan.raccoon.document.BinaryCodec.isCompactString;
import test_document._Log;


class BinaryEncoder extends BinaryOutputStream
{
	private HashMap<Object, Integer> mObjectLookup;
	private HashMap<ByteKey, Integer> mStructLookup;


	public BinaryEncoder(OutputStream aOutputStream)
	{
		super(aOutputStream);

		mObjectLookup = new HashMap<>();
		mStructLookup = new HashMap<>();
	}


	void marshal(Object aObject) throws IOException
	{
		if (aObject instanceof Document v)
		{
			writeDocument(v);
		}
		else if (aObject instanceof Array v)
		{
			writeArray(v);
		}
		else
		{
			BinaryCodec type = BinaryCodec.identify(aObject);

			if (type == null)
			{
				throw new IllegalArgumentException("Unsupported type: " + aObject.getClass().getCanonicalName());
			}

			writeValue(type, aObject);
		}

//		for (Entry<Object, Integer> entry : mObjectLookup.entrySet())
//		{
//			System.out.println("** " + entry.getKey());
//		}
//		for (Entry<ByteKey, Integer> entry : mStructLookup.entrySet())
//		{
//			System.out.println("## " + entry.getKey().toString().replace('\r', '-').replace('\n', '-').replace('\t', '-'));
//		}
	}


	void writeDocument(Document aDocument) throws IOException
	{
		Integer ref2 = mObjectLookup.get(aDocument);
		if (ref2 != null)
		{
			writeInterleaved(BinaryCodec.REFERENCE.ordinal(), ref2);
			return;
		}

		ArrayList<Object> pendingValues = new ArrayList<>();
		ArrayList<BinaryCodec> pendingTypes = new ArrayList<>();

		BinaryBufferedOutputStream header = new BinaryBufferedOutputStream();

		for (Entry<String, Object> entry : aDocument.entrySet())
		{
			String key = entry.getKey();
			Object value = entry.getValue();
			BinaryCodec type = BinaryCodec.identify(value);

			pendingTypes.add(type);
			pendingValues.add(value);

			header.writeInterleaved(type.ordinal(), key.length());
			header.writeUTF(key);
		}

		byte[] headerData = header.finish();
		ByteKey key = new ByteKey(headerData);
		Integer ref = mStructLookup.get(key);
		if (ref != null)
		{
			writeInterleaved(BinaryCodec.BINARY.ordinal(), ref);
		}
		else
		{
			writeInterleaved(BinaryCodec.DOCUMENT.ordinal(), aDocument.size());
			writeBytes(headerData);
			mStructLookup.put(key, mStructLookup.size());
		}

		for (int i = 0; i < pendingValues.size(); i++)
		{
			BinaryCodec type = pendingTypes.get(i);
			Object value = pendingValues.get(i);

			writeValue(type, value);
		}

		mObjectLookup.put(aDocument, mObjectLookup.size());
	}


	void writeArray(Array aArray) throws IOException
	{
		Integer ref = mObjectLookup.get(aArray);
		if (ref != null)
		{
			writeInterleaved(BinaryCodec.REFERENCE.ordinal(), ref);
			return;
		}

		writeInterleaved(BinaryCodec.ARRAY.ordinal(), aArray.size());

		for (int offset = 0, elementCount = aArray.size(); offset < elementCount;)
		{
			ArrayList<Object> pending = new ArrayList<>();
			BinaryCodec nextType = null;
			int runLen = 0;

			for (int i = offset; i < elementCount; i++, runLen++)
			{
				Object value = aArray.get(i);
				BinaryCodec type = BinaryCodec.identify(value);

				if (nextType != type && nextType != null)
				{
					break;
				}

				pending.add(value);
				nextType = type;
			}

			writeInterleaved(nextType.ordinal(), runLen);

			for (int i = 0; --runLen >= 0; i++, offset++)
			{
				Object value = pending.get(i);

				writeValue(nextType, value);
			}
		}

		mObjectLookup.put(aArray, mObjectLookup.size());
	}


	private void writeValue(BinaryCodec aType, Object aValue) throws IOException
	{
		switch (aType)
		{
			case DOCUMENT:
				if (aValue instanceof Document v)
				{
					writeDocument(v);
				}
				else
				{
					writeUnsignedVarint((Integer)aValue);
				}
				break;
			case ARRAY:
				if (aValue instanceof Array v)
				{
					writeArray(v);
				}
				else
				{
					writeUnsignedVarint((Integer)aValue);
				}
				break;
			case REFERENCE:
				throw new IllegalStateException();
//				writeUnsignedVarint((Integer)aValue);
//				break;
			default:
				aType.encoder.encode(this, aValue);
				break;
		}
	}


	static boolean isReferencableValue(BinaryCodec aType, Object aValue)
	{
		if (aType == BinaryCodec.BOOLEAN || aType == BinaryCodec.REFERENCE)
		{
			return false;
		}
		if (aType == BinaryCodec.INT || aType == BinaryCodec.LONG || aType == BinaryCodec.BYTE || aType == BinaryCodec.SHORT)
		{
			long v = ((Number)aValue).longValue();
			return v < -63 || v > 63;
		}
		return true;
	}


	static class ByteKey
	{
		final byte[] mBuffer;


		public ByteKey(byte[] aBuffer)
		{
			mBuffer = aBuffer;
		}


		@Override
		public String toString()
		{
			return new String(mBuffer);
		}


		@Override
		public int hashCode()
		{
			return Arrays.hashCode(mBuffer);
		}


		@Override
		public boolean equals(Object aOther)
		{
			return aOther == this || aOther instanceof ByteKey v && Arrays.equals(mBuffer, v.mBuffer);
		}
	}
}
