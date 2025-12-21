package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import static org.terifan.raccoon.document.BinaryType.ARRAY;
import static org.terifan.raccoon.document.BinaryType.DOCUMENT;
import static org.terifan.raccoon.document.BinaryType.isReferencableValue;


public class BinaryDecoder extends BinaryInputStream implements AutoCloseable
{
	private Entry mNextEntry;


	public BinaryDecoder(InputStream aInputStream)
	{
		super(aInputStream);
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
		return (T)readValue(readType());
	}


	void unmarshal(Collection aContainer) throws IOException
	{
		BinaryType type = readType();

		if (aContainer instanceof Document v)
		{
			if (type == BinaryType.ARRAY)
			{
				throw new StreamException("Attempt to unmarshal a Document when binary stream contains an Array.");
			}
			if (type != BinaryType.DOCUMENT)
			{
				throw new StreamException("Stream corrupted.");
			}

			Document d = readDocument();
			v.putAll(d);
		}
		else if (aContainer instanceof Array v)
		{
			if (type == BinaryType.DOCUMENT)
			{
				throw new StreamException("Attempt to unmarshal an Array when binary stream contains a Document.");
			}
			if (type != BinaryType.ARRAY)
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
		int len = (int)readUnsignedVarint();

		Document document = new Document();
		for (int i = 0; i < len; i++)
		{
			String name = readString();
			BinaryType type = readType();
			Object value = readValue(type);

			document.put(name, value);
		}

		return document;
	}


	Array readArray() throws IOException
	{
		int len = (int)readUnsignedVarint();

		Array array = new Array();
		for (int offset = 0; offset < len; )
		{
			BinaryType type = readType();
			int runLen = (int)readUnsignedVarint();

			for (; --runLen >= 0; offset++)
			{
				array.add(readValue(type));
			}
		}

		return array;
	}


	private Object readValue(BinaryType aType) throws IOException
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
			case STRING:
				value = readString();
				break;
			default:
				value = aType.decoder.decode(this);
				break;
		}

		return value;
	}


	Entry readEntry() throws IOException
	{
		long params = readInterleaved();

		Entry token = new Entry();
		token.value = (int)(params >>> 32);
		token.type = BinaryType.values()[(int)params];

		return token;
	}


	static class Entry
	{
		BinaryType type;
		int value;
		String name;


		public Entry()
		{
		}


		public Entry(BinaryType aType, int aValue)
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
