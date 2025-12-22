package org.terifan.raccoon.document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import static org.terifan.raccoon.document.BinaryType.ARRAY;
import static org.terifan.raccoon.document.BinaryType.BOOLEAN;
import static org.terifan.raccoon.document.BinaryType.BYTE;
import static org.terifan.raccoon.document.BinaryType.DOCUMENT;
import static org.terifan.raccoon.document.BinaryType.NULL;


public class BinaryDecoder extends BinaryInputStream implements AutoCloseable
{
	private Lookup mDocStructs = new Lookup(false);
	private Lookup mArrStructs = new Lookup(false);
	private HashMap<BinaryType, LRU<?>> mValueLookup = new HashMap<>();


	public BinaryDecoder(InputStream aInputStream)
	{
		super(aInputStream);
	}


//	public boolean next()
//	{
//		if (mNextEntry == null)
//		{
//			try
//			{
//				mNextEntry = readEntry();
//			}
//			catch (Exception e)
//			{
//			}
//		}
//
//		return mNextEntry != null;
//	}


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
		Document document = new Document();

		byte[] header = mDocStructs.read(this);

		BinaryInputStream fields = new BinaryInputStream(new ByteArrayInputStream(header));

		for (BinaryType type; (type = fields.readType()) != BinaryType.TERMINATOR;)
		{
			String name = fields.readString();
			Object value = readValue(type);
			document.put(name, value);
		}

		return document;
	}


	Array readArray() throws IOException
	{
		Array array = new Array();

		byte[] header = mArrStructs.read(this);

		BinaryInputStream fields = new BinaryInputStream(new ByteArrayInputStream(header));

		for (BinaryType type; (type = fields.readType()) != BinaryType.TERMINATOR;)
		{
			long runLen = fields.readUnsignedVarint();

			while (--runLen >= 0)
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
			case NULL:
			case BYTE:
			case BOOLEAN:
				value = aType.decoder.decode(this);
				break;
			case STRING:
				LRU lookup = mValueLookup.computeIfAbsent(aType, t -> new LRU<>(false));
				int i = (int)readVarint();
				if (i < 0)
				{
					value = lookup.valueAt(-i - 1);
				}
				else
				{
					value = readUTF(i);
					lookup.add((String)value);
				}
				break;
			default:
				value = aType.decoder.decode(this);
				break;
		}

		return value;
	}
}
