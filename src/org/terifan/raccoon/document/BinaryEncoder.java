package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.terifan.raccoon.document.BinaryDecoder.Entry;


public class BinaryEncoder extends BinaryOutputStream implements AutoCloseable
{
	private HashMap<ByteKey, Integer> mDocStructLookup;
	private HashMap<ByteKey, Integer> mArrStructLookup;
	private HashMap<String, Integer> mStringLookup;
	private HashMap<String, Integer> mNameLookup;
	private HashMap<Object, Integer> mValueLookup;

	public static boolean DEBUG;


	public BinaryEncoder(OutputStream aOutputStream)
	{
		super(aOutputStream);

		mDocStructLookup = new HashMap<>();
		mArrStructLookup = new HashMap<>();
		mStringLookup = new HashMap<>();
		mNameLookup = new HashMap<>();
		mValueLookup = new HashMap<>();

		mValueLookup.put(null, 0);
		mStringLookup.put(null, 0);
	}


	@Override
	public void close()
	{
	}


//	public void writeTerminate() throws IOException
//	{
//		writeInterleaved(BinaryCodec.TERMINATE, 0);
//	}
	public void writeObject(Object aObject) throws IOException
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
			writeField(aObject);
		}

		if (DEBUG)
		{
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
			for (Map.Entry<String, Integer> entry : mNameLookup.entrySet())
			{
				System.out.println("key: " + entry.getKey().toString().replace('\r', '-').replace('\n', '-').replace('\t', '-'));
			}
			for (Map.Entry<Object, Integer> entry : mValueLookup.entrySet())
			{
				System.out.println("val: " + entry.getKey().toString().replace('\r', '-').replace('\n', '-').replace('\t', '-'));
			}
		}
	}


	void writeField(Object aObject) throws IOException, UnsupportedTypeException
	{
		BinaryCodec type = BinaryCodec.identify(aObject);

		if (type == null)
		{
			if (aObject instanceof Enum)
			{
				throw new UnsupportedTypeException("Enums are not supported as they are inherently unsafe for serialization: " + aObject.getClass().getCanonicalName());
			}

			throw new UnsupportedTypeException(aObject.getClass().getCanonicalName());
		}

		if (aObject != null && mValueLookup.containsKey(aObject))
		{
			type = BinaryCodec.REFERENCE;
			aObject = mValueLookup.get(aObject);
		}

		if (type == BinaryCodec.INT && ((Integer)aObject) > 0 && ((Integer)aObject) < 200)
		{
			writeInterleaved(type, (Integer)aObject);
		}
		else if (type == BinaryCodec.REFERENCE && ((Integer)aObject) < 200)
		{
			writeInterleaved(type, ((Integer)aObject));
		}
		else if (type == BinaryCodec.STRING && mStringLookup.containsKey(aObject) && mStringLookup.get(aObject) < 200)
		{
			writeInterleaved(BinaryCodec.STRING_REFERENCE, mStringLookup.get(aObject));
		}
		else if (type == BinaryCodec.STRING && ((String)aObject).length() < 200 && !mStringLookup.containsKey(aObject))
		{
			writeInterleaved(type, ((String)aObject).length());
			writeUTF((String)aObject);
			mStringLookup.put((String)aObject, mStringLookup.size());
		}
		else
		{
			writeInterleaved(type, 0);
			writeValue(type, aObject);
		}
	}


	void writeDocument(Document aDocument) throws IOException
	{
		Integer ref2 = mValueLookup.get(aDocument);
		if (ref2 != null)
		{
			writeInterleaved(BinaryCodec.REFERENCE, ref2);
			return;
		}

		ArrayList<Entry> entries = new ArrayList<>();
		BinaryBufferedOutputStream header = new BinaryBufferedOutputStream();

		for (Map.Entry<String, Object> entry : aDocument.entrySet())
		{
			String name = entry.getKey();
			Object value = entry.getValue();
			BinaryCodec type = BinaryCodec.identify(value);

//			if (mValueLookup.containsKey(value))
//			{
//				type = BinaryCodec.REFERENCE;
//				value = mValueLookup.get(value);
//			}
			entries.add(new Entry(type, value));

			if (mNameLookup.containsKey(name))
			{
				header.writeInterleaved(type, mNameLookup.get(name) * 2 + 1);
			}
			else
			{
				header.writeInterleaved(type, name.length() * 2);
				header.writeUTF(name);
				mNameLookup.put(name, mNameLookup.size());
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
			write(headerData);
			mDocStructLookup.put(key, mDocStructLookup.size());
		}

		for (Entry entry : entries)
		{
			writeValue(entry.type, entry.object);
		}

		mValueLookup.put(aDocument, mValueLookup.size());
	}


	void writeArray(Array aArray) throws IOException
	{
		Integer ref2 = mValueLookup.get(aArray);
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

//				if (mValueLookup.containsKey(value))
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
			write(headerData);
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

		mValueLookup.put(aArray, mValueLookup.size());
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
