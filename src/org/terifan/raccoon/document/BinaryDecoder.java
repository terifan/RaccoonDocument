package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import static org.terifan.raccoon.document.BinaryType.ARRAY;
import static org.terifan.raccoon.document.BinaryType.DOCUMENT;
import static org.terifan.raccoon.document.BinaryType.STRING;


public class BinaryDecoder extends BinaryInputStream implements AutoCloseable, Iterator<Object>, Iterable<Object>
{
	private ValueLookup<String> mStrings;
	private ArrayList<ArrHeader> mArrHeaders;
	private ArrayList<DocHeader> mDocHeaders;
	private ValueLookup<Array> mArrInstances;
	private ValueLookup<Document> mDocInstances;

	private boolean mReady;
	private boolean mEnded;
	private Object mNext;


	public BinaryDecoder(InputStream aInputStream)
	{
		super(aInputStream);

		mStrings = new ValueLookup<>();
		mArrHeaders = new ArrayList<>();
		mDocHeaders = new ArrayList<>();
		mArrInstances = new ValueLookup<>();
		mDocInstances = new ValueLookup<>();
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
			throw new StreamException("Stream corrupted.");
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
		if (type == null)
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
		DocHeader header;
		int code = readUnsignedVarint();
		switch (code & 0b11)
		{
			case 0:
				return mDocInstances.get(code >> 2);
			case 1:
				header = mDocHeaders.get(code >> 2);
				break;
			case 2:
				mDocHeaders.add(header = new DocHeader(code >> 2));
				break;
			default:
				throw new StreamException("Stream corrupted.");
		}

		Document document = new Document();
		mDocInstances.add(document);
		BinaryType[] types = header.types;
		String[] names = header.names;

		for (int i = 0; i < types.length; i++)
		{
			document.put(names[i], readValue(types[i]));
		}

		return document;
	}


	Array readArray() throws IOException
	{
		ArrHeader header;
		int code = readUnsignedVarint();
		switch (code & 0b11)
		{
			case 0:
				return mArrInstances.get(code >> 2);
			case 1:
				header = mArrHeaders.get(code >> 2);
				break;
			case 2:
				mArrHeaders.add(header = new ArrHeader(code >> 2));
				break;
			default:
				throw new StreamException("Stream corrupted.");
		}

		Array array = new Array();
		mArrInstances.add(array);
		BinaryType[] types = header.types;
		int[] lengths = header.lengths;

		for (int i = 0; i < types.length ; i++)
		{
			BinaryType type = types[i];
			for (int runLen = lengths[i]; --runLen >= 0; )
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
				value = readString(mStrings);
				break;
			default:
				value = aType.decoder.decode(this);
				break;
		}

		return value;
	}


	private class DocHeader
	{
		BinaryType[] types;
		String[] names;


		public DocHeader(int aCount) throws IOException
		{
			types = new BinaryType[aCount];
			names = new String[aCount];
			for (int i = 0; i < aCount; i++)
			{
				BinaryType type = BinaryType.values()[readUnsignedVarint()];
				types[i] = type;
				names[i] = readString();
			}
		}
	}


	private class ArrHeader
	{
		BinaryType[] types;
		int[] lengths;


		public ArrHeader(int aCount) throws IOException
		{
			types = new BinaryType[aCount];
			lengths = new int[aCount];
			for (int i = 0; i < aCount; i++)
			{
				types[i] = BinaryType.values()[readUnsignedVarint()];
				lengths[i] = readUnsignedVarint();
			}
		}
	}


	@SuppressWarnings("serial")
	static class ValueLookup<T> extends ArrayList<T>
	{
	}
}
