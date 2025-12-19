package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import static org.terifan.raccoon.document.BinaryCodec.ARRAY;
import static org.terifan.raccoon.document.BinaryCodec.DOCUMENT;
import static org.terifan.raccoon.document.BinaryCodec.isReferencableValue;


class BinaryEncoder extends BinaryOutputStream implements AutoCloseable
{
	private LRU<String> mStringLookup;
	private LRU<String> mNameLookup;
	private LRU<Object> mValueLookup;
	private LRU<Document> mDocumentLookup;
	private LRU<Array> mArrayLookup;


	public BinaryEncoder(OutputStream aOutputStream)
	{
		super(aOutputStream);

		mStringLookup = new LRU<>();
		mNameLookup = new LRU<>();
		mValueLookup = new LRU<>();
		mDocumentLookup = new LRU<>();
		mArrayLookup = new LRU<>();
	}


	public void writeObject(Object aObject) throws IOException
	{
		if (aObject instanceof Document v)
		{
			writeInterleaved(BinaryCodec.DOCUMENT, 0);
			writeDocument(v);
		}
		else if (aObject instanceof Array v)
		{
			writeInterleaved(BinaryCodec.ARRAY, 0);
			writeArray(v);
		}
		else
		{
			writeField(aObject);
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

		writeInterleaved(type, 0);
		writeValue(type, aObject);
	}


	void writeDocument(Document aDocument) throws IOException
	{
		int ref = mDocumentLookup.indexOf(aDocument);
		if (ref != -1)
		{
			writeVarint(-ref - 1);
			return;
		}

		writeVarint(aDocument.size());

		for (Map.Entry<String, Object> entry : aDocument.entrySet())
		{
			String name = entry.getKey();
			Object value = entry.getValue();
			BinaryCodec type;

			int tmp = mValueLookup.indexOf(value);
			if (tmp != -1)
			{
				type = BinaryCodec.REFERENCE;
				value = tmp;
			}
			else
			{
				type = BinaryCodec.identify(value);
			}

			tmp = mNameLookup.indexOf(name);
			if (tmp != -1)
			{
				writeInterleaved(type, tmp * 2 + 1);
			}
			else
			{
				writeInterleaved(type, name.length() * 2);
				writeUTF(name);
				mNameLookup.add(name);
			}

			writeValue(type, value);
		}

		mDocumentLookup.add(aDocument);
	}


	void writeArray(Array aArray) throws IOException
	{
		int ref = mArrayLookup.indexOf(aArray);
		if (ref != -1)
		{
			writeVarint(-ref - 1);
			return;
		}

		writeVarint(aArray.size());

		ArrayList<Object> values = new ArrayList<>();

		for (int offset = 0; offset < aArray.size();)
		{
			BinaryCodec nextType = null;

			for (int i = offset; i < aArray.size(); i++)
			{
				Object value = aArray.get(i);
				BinaryCodec type;

				int tmp = mValueLookup.indexOf(value);
				if (tmp != -1)
				{
					type = BinaryCodec.REFERENCE;
					value = tmp;
				}
				else
				{
					type = BinaryCodec.identify(value);
				}

				if (nextType != type && nextType != null)
				{
					break;
				}

				values.add(value);
				nextType = type;
			}

			writeInterleaved(nextType, values.size());

			for (Object value : values)
			{
				writeValue(nextType, value);
			}

			offset += values.size();
			values.clear();
		}

		mArrayLookup.add(aArray);
	}


	private void writeValue(BinaryCodec aType, Object aValue) throws IOException
	{
		switch (aType)
		{
			case DOCUMENT:
				writeDocument((Document)aValue);
				break;
			case ARRAY:
				writeArray((Array)aValue);
				break;
			case REFERENCE:
				writeUnsignedVarint((Integer)aValue);
				break;
			case STRING:
				String s = (String)aValue;
				int i = mStringLookup.indexOf(s);
				if (i != -1)
				{
					writeVarint(-i - 1);
				}
				else
				{
					writeVarint(s.length());
					writeUTF(s);
					mStringLookup.add(s);
				}
				break;
			default:
				aType.encoder.encode(this, aValue);

				if (isReferencableValue(aType, aValue))
				{
					mValueLookup.add(aValue);
				}
				break;
		}
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
