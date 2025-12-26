package org.terifan.raccoon.document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import static org.terifan.raccoon.document.BinaryType.ARRAY;
import static org.terifan.raccoon.document.BinaryType.BOOLEAN;
import static org.terifan.raccoon.document.BinaryType.BYTE;
import static org.terifan.raccoon.document.BinaryType.DOCUMENT;
import static org.terifan.raccoon.document.BinaryType.NULL;


public class BinaryDecoder extends BinaryInputStream implements AutoCloseable, Iterator<Object>, Iterable<Object>
{
	private Lookup mDocStructs;
	private Lookup mArrStructs;
	private LookupMap<Document> mDocLookup;
	private LookupMap<Array> mArrLookup;
	private LookupMap<String> mStringLookup;

	private boolean mReady;
	private boolean mEnded;
	private Object mNext;


	public BinaryDecoder(InputStream aInputStream)
	{
		super(aInputStream);

		mStringLookup = new LookupMap<>(false);
		mArrStructs = new Lookup(false);
		mDocStructs = new Lookup(false);
		mArrLookup = new LookupMap<>(false);
		mDocLookup = new LookupMap<>(false);
	}


	@Override
	public Iterator<Object> iterator()
	{
		return this;
	}


	@Override
	public boolean hasNext()
	{
		if (!mReady)
		{
			try
			{
				mNext = readObjectImpl();
				mReady = !mEnded;
			}
			catch (IOException e)
			{
				mEnded = true;
			}
		}
		return mReady;
	}


	@Override
	public Object next()
	{
		if (mEnded)
		{
			throw new StreamException("Reading beyond end of stream.");
		}
		try
		{
			return readObject();
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}


	public <T> T readObject() throws IOException
	{
		if (!mReady && !mEnded)
		{
			mNext = readObjectImpl();
			mReady = !mEnded;
		}
		if (mEnded)
		{
			throw new IOException("Reading beyond end of stream.");
		}

		Object value = mNext;
		mNext = null;
		mReady = false;
		return (T)value;
	}


	private <T> T readObjectImpl() throws IOException
	{
		BinaryType type = readType();
		if (type == BinaryType.TERMINATOR)
		{
			mEnded = true;
			return null;
		}
		return (T)readValue(type);
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
		int n = (int)readVarint();
		if (n > 0 && (n & 1) == 1)
		{
			return mDocLookup.valueAt(n / 2);
		}

		Document document = new Document();

		mDocLookup.add(document);

		byte[] header = mDocStructs.read(this, n);

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
		int n = (int)readVarint();
		if (n > 0 && (n & 1) == 1)
		{
			return mArrLookup.valueAt(n / 2);
		}

		Array array = new Array();

		mArrLookup.add(array);

		byte[] header = mArrStructs.read(this, n);

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
				value = readString(mStringLookup);
				break;
			default:
				value = aType.decoder.decode(this);
				break;
		}

		return value;
	}
}
