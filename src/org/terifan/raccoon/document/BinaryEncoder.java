package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.terifan.raccoon.document.BinaryDecoder.Entry;


public class BinaryEncoder extends BinaryOutputStream
{
	private HashMap<Object, Integer> mObjectLookup;
	private HashMap<ByteKey, Integer> mDocStructLookup;
	private HashMap<ByteKey, Integer> mArrStructLookup;
	private HashMap<String, Integer> mStringLookup;
	private HashMap<String, Integer> mKeyLookup;
	private HashMap<Object, Integer> mValueLookup;

	public static boolean DEBUG;


	public BinaryEncoder(OutputStream aOutputStream)
	{
		super(aOutputStream);

		mObjectLookup = new HashMap<>();
		mDocStructLookup = new HashMap<>();
		mArrStructLookup = new HashMap<>();
		mStringLookup = new HashMap<>();
		mKeyLookup = new HashMap<>();
		mValueLookup = new HashMap<>();
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

		if (DEBUG)
		{
			for (Map.Entry<Object, Integer> entry : mObjectLookup.entrySet())
			{
				System.out.println("obj: " + entry.getKey());
			}
			for (Map.Entry<ByteKey, Integer> entry : mDocStructLookup.entrySet())
			{
				System.out.println("doc: " + entry.getKey().toString().replace('\r', '-').replace('\n', '-').replace('\t', '-'));
			}
			for (Map.Entry<ByteKey, Integer> entry : mArrStructLookup.entrySet())
			{
				System.out.println("arr: " + entry.getKey().toString().replace('\r', '-').replace('\n', '-').replace('\t', '-'));
			}
			for (Map.Entry<String, Integer> entry : mStringLookup.entrySet())
			{
				System.out.println("str: " + entry.getKey().toString().replace('\r', '-').replace('\n', '-').replace('\t', '-'));
			}
			for (Map.Entry<String, Integer> entry : mKeyLookup.entrySet())
			{
				System.out.println("key: " + entry.getKey().toString().replace('\r', '-').replace('\n', '-').replace('\t', '-'));
			}
			for (Map.Entry<Object, Integer> entry : mValueLookup.entrySet())
			{
				System.out.println("val: " + entry.getKey().toString().replace('\r', '-').replace('\n', '-').replace('\t', '-'));
			}
		}
	}


	void writeDocument(Document aDocument) throws IOException
	{
		Integer ref2 = mObjectLookup.get(aDocument);
		if (ref2 != null)
		{
			writeInterleaved(BinaryCodec.REFERENCE, ref2);
			return;
		}

		ArrayList<Entry> entries = new ArrayList<>();
		BinaryBufferedOutputStream header = new BinaryBufferedOutputStream();

		for (Map.Entry<String, Object> entry : aDocument.entrySet())
		{
			String key = entry.getKey();
			Object value = entry.getValue();
			BinaryCodec type = BinaryCodec.identify(value);

			if (type == BinaryCodec.COMPACT_STRING && mStringLookup.containsKey(value))
			{
				type = BinaryCodec.STRING;
			}
//			if (isReferencableValue(type, value) && mValueLookup.containsKey(value))
//			{
//				type = BinaryCodec.REFERENCE;
//				value = mValueLookup.get(value);
//			}

			entries.add(new Entry(type, value));

			if (mKeyLookup.containsKey(key))
			{
				header.writeInterleaved(type, mKeyLookup.get(key) * 2 + 1);
			}
			else
			{
				header.writeInterleaved(type, key.length() * 2);
				header.writeUTF(key);
				mKeyLookup.put(key, mKeyLookup.size());
			}
		}

		byte[] headerData = header.finish();
		ByteKey key = new ByteKey(headerData);
		Integer ref = mDocStructLookup.get(key);
		if (ref != null)
		{
			writeInterleaved(BinaryCodec.BINARY, ref);
		}
		else
		{
			writeInterleaved(BinaryCodec.DOCUMENT, aDocument.size());
			writeBytes(headerData);
			mDocStructLookup.put(key, mDocStructLookup.size());
		}

		for (Entry entry : entries)
		{
			writeValue(entry.type, entry.object);
		}

		mObjectLookup.put(aDocument, mObjectLookup.size());
	}


	void writeArray(Array aArray) throws IOException
	{
		Integer ref2 = mObjectLookup.get(aArray);
		if (ref2 != null)
		{
			writeInterleaved(BinaryCodec.REFERENCE, ref2);
			return;
		}

		ArrayList<Entry> entries = new ArrayList<>();
		BinaryBufferedOutputStream header = new BinaryBufferedOutputStream();

		for (int offset = 0, elementCount = aArray.size(); offset < elementCount;)
		{
			BinaryCodec nextType = null;
			int runLen = 0;

			for (int i = offset; i < elementCount; i++, runLen++)
			{
				Object value = aArray.get(i);
				BinaryCodec type = BinaryCodec.identify(value);

				if (type == BinaryCodec.COMPACT_STRING && mStringLookup.containsKey(value))
				{
					type = BinaryCodec.STRING;
				}
//				if (isReferencableValue(type, value) && mValueLookup.containsKey(value))
//				{
//					type = BinaryCodec.REFERENCE;
//					value = mValueLookup.get(value);
//				}

				if (nextType != type && nextType != null)
				{
					break;
				}

				nextType = type;
			}

			entries.add(new Entry(nextType, runLen));
			header.writeInterleaved(nextType, runLen);
			offset += runLen;
		}

		byte[] headerData = header.finish();
		ByteKey key = new ByteKey(headerData);
		Integer ref = mArrStructLookup.get(key);
		if (ref != null)
		{
			writeInterleaved(BinaryCodec.BINARY, ref);
		}
		else
		{
			writeInterleaved(BinaryCodec.ARRAY, entries.size());
			writeBytes(headerData);
			mArrStructLookup.put(key, mArrStructLookup.size());
		}

		int i = 0;
		for (Entry entry : entries)
		{
			for (int j = 0; j < entry.value; j++)
			{
				Object v = aArray.get(i++);
				writeValue(entry.type, v);
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
			case COMPACT_STRING:
				aType.encoder.encode(this, aValue);
				break;
			case STRING:
				String s = (String)aValue;
				if (mStringLookup.containsKey(s))
				{
					writeVarint(-mStringLookup.get(s) - 1);
				}
				else
				{
					writeVarint(s.length());
					writeUTF(s);
					mStringLookup.put(s, mStringLookup.size());
				}
				break;
			case REFERENCE:
				writeUnsignedVarint((Integer)aValue);
				break;
			default:
				aType.encoder.encode(this, aValue);

				if (isReferencableValue(aType, aValue))
				{
					mValueLookup.put(aValue, mValueLookup.size());
				}
				break;
		}
	}


	static boolean isReferencableValue(BinaryCodec aType, Object aValue)
	{
		if (aType == BinaryCodec.BOOLEAN || aType == BinaryCodec.REFERENCE || aType == BinaryCodec.ARRAY || aType == BinaryCodec.DOCUMENT || aType == BinaryCodec.STRING)
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
