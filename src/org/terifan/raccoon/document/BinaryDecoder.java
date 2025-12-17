package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import static org.terifan.raccoon.document.BinaryCodec.ARRAY;
import static org.terifan.raccoon.document.BinaryCodec.DOCUMENT;
import static org.terifan.raccoon.document.BinaryCodec.isReferencableValue;


public class BinaryDecoder extends BinaryInputStream implements AutoCloseable
{
	private ArrayList<String> mStringLookup;
	private ArrayList<String> mNameLookup;
	private HashMap<Integer,Object> mValueLookup;

	private Entry mNextEntry;


	public BinaryDecoder(InputStream aInputStream)
	{
		super(aInputStream);

		mStringLookup = new ArrayList<>();
		mNameLookup = new ArrayList<>();
		mValueLookup = new HashMap<>();
	}


	public boolean next()
	{
		if (mNextEntry == null)
		{
			try
			{
				mNextEntry = readEntry();
			}
			catch (Exception e)
			{
			}
		}

		return mNextEntry != null;
	}


	public <T> T readObject() throws IOException
	{
		Entry entry = mNextEntry != null ? mNextEntry : readEntry();
		mNextEntry = null;

		return (T)readValue(entry.type);
	}


	private Object readField(Entry aEntry) throws IOException
	{
//		if (aEntry.value != 0)
//		{
//			if (aEntry.type == BinaryCodec.INT)
//			{
//				return aEntry.value;
//			}
//			if (aEntry.type == BinaryCodec.SHORT)
//			{
//				return (short)aEntry.value;
//			}
//			if (aEntry.type == BinaryCodec.BYTE)
//			{
//				return (byte)aEntry.value;
//			}
//			if (aEntry.type == BinaryCodec.LONG)
//			{
//				return (long)aEntry.value;
//			}
//			if (aEntry.type == BinaryCodec.FLOAT)
//			{
//				return (float)aEntry.value;
//			}
//			if (aEntry.type == BinaryCodec.DOCUMENT)
//			{
//				return (double)aEntry.value;
//			}
//			if (aEntry.type == BinaryCodec.CHAR)
//			{
//				return (char)aEntry.value;
//			}
//			if (aEntry.type == BinaryCodec.BOOLEAN)
//			{
//				return aEntry.value == 1;
//			}
//			if (aEntry.type == BinaryCodec.REFERENCE)
//			{
//				return mValueLookup.get(aEntry.value);
//			}
//			if (aEntry.type == BinaryCodec.STRING)
//			{
//				String s = readUTF(aEntry.value);
//				mStringLookup.add(s);
//				return s;
//			}
//			if (aEntry.type == BinaryCodec.STRING_REFERENCE)
//			{
//				return mStringLookup.get(aEntry.value);
//			}
//		}
		return readValue(aEntry.type);
	}


	void unmarshal(Collection aContainer) throws IOException
	{
		Entry entry = readEntry();

		if (aContainer instanceof Document v)
		{
			if (entry.type == BinaryCodec.ARRAY)
			{
				throw new StreamException("Attempt to unmarshal a Document when binary stream contains an Array.");
			}
			if (entry.type != BinaryCodec.DOCUMENT)
			{
				throw new StreamException("Stream corrupted.");
			}

			Document d = readDocument();
			v.putAll(d);
		}
		else if (aContainer instanceof Array v)
		{
			if (entry.type == BinaryCodec.DOCUMENT)
			{
				throw new StreamException("Attempt to unmarshal an Array when binary stream contains a Document.");
			}
			if (entry.type != BinaryCodec.ARRAY)
			{
				throw new StreamException("Stream corrupted.");
			}

			Array a = readArray();
			v.clear().addAll(a);
		}
		else
		{
			throw new StreamException("Stream corrupted.");
		}
	}


	Document readDocument() throws IOException
	{
		Document document = new Document();
		for (Entry entry; (entry = readEntry()).type != BinaryCodec.TERMINATOR;)
		{
			if ((entry.value & 1) == 1)
			{
				entry.name = mNameLookup.get(entry.value / 2);
			}
			else
			{
				entry.name = readUTF(entry.value / 2);
				mNameLookup.add(entry.name);
			}

			Object value = readValue(entry.type);

			document.put(entry.name, value);

//			if (entry.type==DOCUMENT||entry.type==ARRAY)mValueLookup.put(mValueLookup.size(), value);
		}
		mValueLookup.put(mValueLookup.size(), document);
		return document;
	}


	Array readArray() throws IOException
	{
		Array array = new Array();
		for (Entry entry; (entry = readEntry()).type != BinaryCodec.TERMINATOR;)
		{
			for (int i = 0; i < entry.value; i++)
			{
				Object value = readValue(entry.type);

				array.add(value);

//				if (entry.type==DOCUMENT||entry.type==ARRAY)mValueLookup.put(mValueLookup.size(), value);
			}
		}
		mValueLookup.put(mValueLookup.size(), array);
		return array;
	}


	private Object readValue(BinaryCodec aType) throws IOException
	{
		Object value;
		switch (aType)
		{
			case DOCUMENT:
				value = readDocument();
				break;
			case ARRAY:
				value = readArray();
				break;
			case REFERENCE:
				value = mValueLookup.get((int)readUnsignedVarint());
				break;
			case STRING:
				int i = (int)readVarint();
				if (i < 0)
				{
					value = mStringLookup.get(-i - 1);
				}
				else
				{
					value = readUTF(i);
					mStringLookup.add((String)value);
				}
				break;
			default:
				value = aType.decoder.decode(this);

				if (isReferencableValue(aType, value))
				{
//					mValueLookup.put(mValueLookup.size(), value);
				}
				break;
		}

		return value;
	}


	Entry readEntry() throws IOException
	{
		long params = readInterleaved();

		Entry token = new Entry();
		token.value = (int)(params >>> 32);
		token.type = BinaryCodec.values()[(int)params];

		return token;
	}


	static class Entry
	{
		BinaryCodec type;
		int value;
		String name;


		public Entry()
		{
		}


		public Entry(BinaryCodec aType, int aValue)
		{
			this.type = aType;
			this.value = aValue;
		}


		@Override
		public int hashCode()
		{
			int hash = 3;
			hash = 29 * hash + Objects.hashCode(this.type);
			hash = 29 * hash + this.value;
			return hash;
		}


		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
			{
				return true;
			}
			if (obj == null)
			{
				return false;
			}
			if (getClass() != obj.getClass())
			{
				return false;
			}
			final Entry other = (Entry)obj;
			if (this.value != other.value)
			{
				return false;
			}
			return this.type == other.type;
		}


		@Override
		public String toString()
		{
			return "{type=" + type + ", value=" + value + ", name=" + name + "}";
		}
	}
}
