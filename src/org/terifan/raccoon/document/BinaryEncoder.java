package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import static org.terifan.raccoon.document.BinaryCodec.isCompactString;


// <obj1><obj2><obj3><strings><structs><directory>
// key1/type, key2/type, key3/type, val1, val2, val3
class BinaryEncoder
{
	private HashMap<Object, Integer> mObjectLookup;
	private HashMap<ByteKey, Integer> mStructLookup;

	private final BinaryOutputStream mOutputStream;


	public BinaryEncoder(OutputStream aOutputStream)
	{
		mOutputStream = new BinaryOutputStream(aOutputStream);
	}


	void marshal(Object aObject) throws IOException
	{
		mObjectLookup = new HashMap<>();
		mStructLookup = new HashMap<>();

		BinaryCodec type = BinaryCodec.identify(aObject);

		if (type == null)
		{
			throw new IllegalArgumentException("Unsupported type: " + aObject.getClass().getCanonicalName());
		}

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
			throw new IllegalStateException();
		}

		mOutputStream.close();

//		for (Entry<Object, Integer> entry : mObjectLookup.entrySet())
//		{
//			System.out.println("** " + entry.getKey());
//		}
//		for (Entry<ByteKey, Integer> entry : mStructsLookup.entrySet())
//		{
//			System.out.println("** " + entry.getKey().toString().replace('\r', '-').replace('\n', '-').replace('\t', '-'));
//		}
	}


	void writeDocument(Document aDocument) throws IOException
	{
		ArrayList<Object> pendingValues = new ArrayList<>();
		ArrayList<BinaryCodec> pendingTypes = new ArrayList<>();

		BinaryBufferedOutputStream buffer = new BinaryBufferedOutputStream();
		for (Entry<String, Object> entry : aDocument.entrySet())
		{
			String key = entry.getKey();
			Object value = entry.getValue();
			BinaryCodec type = BinaryCodec.identify(value);

			Integer ref = mObjectLookup.get(value);
			if (ref != null)
			{
				type = BinaryCodec.REFERENCE;
				value = ref;
			}
			else if (!(type == BinaryCodec.BOOLEAN || (type == BinaryCodec.INT || type == BinaryCodec.LONG || type == BinaryCodec.BYTE || type == BinaryCodec.SHORT) && ((Number)value).longValue() < 128))
			{
				mObjectLookup.put(value, mObjectLookup.size());
			}

			pendingTypes.add(type);
			pendingValues.add(value);

			buffer.writeInterleaved(type.ordinal(), key.length());
			buffer.writeUTF(key);
		}

		byte[] header = buffer.finish();
		ByteKey key = new ByteKey(header);
		Integer ref = mStructLookup.get(key);
		if (ref != null)
		{
			mOutputStream.writeVarint(ref);
		}
		else
		{
			mOutputStream.writeVarint(-header.length);
			mOutputStream.writeBytes(header);
			mStructLookup.put(key, mStructLookup.size());
		}

		for (int i = 0; i < pendingValues.size(); i++)
		{
			BinaryCodec type = pendingTypes.get(i);
			Object value = pendingValues.get(i);

			writeValue(mOutputStream, type, value);
		}
	}


	void writeArray(Array aArray) throws IOException
	{
		for (int offset = 0, elementCount = aArray.size(); offset < elementCount;)
		{
			ArrayList<Object> pending = new ArrayList<>();
			BinaryCodec nextType = null;
			int runLen = 0;

			for (int i = offset; i < elementCount; i++, runLen++)
			{
				Object value = aArray.get(i);
				BinaryCodec type = BinaryCodec.identify(value);

				Integer ref = mObjectLookup.get(value);
				if (ref != null)
				{
					type = BinaryCodec.REFERENCE;
					value = ref;
				}
				else if (!(type == BinaryCodec.BOOLEAN || (type == BinaryCodec.INT || type == BinaryCodec.LONG || type == BinaryCodec.BYTE || type == BinaryCodec.SHORT) && ((Number)value).longValue() < 128))
				{
					mObjectLookup.put(value, mObjectLookup.size());
				}

				if (nextType != type && nextType != null)
				{
					break;
				}

				pending.add(value);
				nextType = type;
			}

			mOutputStream.writeInterleaved(nextType.ordinal(), runLen);

			for (int i = 0; --runLen >= 0; i++, offset++)
			{
				Object value = pending.get(i);

				writeValue(mOutputStream, nextType, value);
			}
		}
	}


	private void writeValue(BinaryOutputStream aOutputStream, BinaryCodec aType, Object aValue) throws IOException
	{
		switch (aType)
		{
			case DOCUMENT:
				writeDocument((Document)aValue);
				break;
			case ARRAY:
				writeArray((Array)aValue);
				break;
			default:
				aType.encoder.encode(aOutputStream, aValue);
				break;
		}
	}


	private static class ByteKey
	{
		private final byte[] mBuffer;


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
			return aOther instanceof ByteKey && Arrays.equals(mBuffer, ((ByteKey)aOther).mBuffer);
		}
	}
}
