package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import static org.terifan.raccoon.document.BinaryCodec.ARRAY;
import static org.terifan.raccoon.document.BinaryCodec.DOCUMENT;
import static org.terifan.raccoon.document.BinaryCodec.TERMINATOR;


public class BinaryDecoder extends BinaryInputStream
{
	private ArrayList<Object> mObjectLookup;
	private ArrayList<ArrayList<Entry>> mStructLookup;


	BinaryDecoder(InputStream aInputStream)
	{
		super(aInputStream);
	}


	Object unmarshal() throws IOException
	{
		mObjectLookup = new ArrayList<>();
		mStructLookup = new ArrayList<>();

		Entry entry = readEntry();

		switch (entry.type)
		{
			case DOCUMENT:
				return readDocument(new Document(), entry);
			case ARRAY:
				return readArray(new Array(), entry);
			case TERMINATOR:
				return entry.type;
			default:
				return readValue(entry.type);
		}
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

			readDocument(v, entry);
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

			readArray(v, entry);
		}
		else
		{
			throw new StreamException("Stream corrupted.");
		}
	}


	Document readDocument(Document aDocument, Entry aEntry) throws IOException
	{
		Entry header = aEntry != null ? aEntry : readEntry();

		if (header.type == BinaryCodec.REFERENCE)
		{
			return (Document)mObjectLookup.get(header.value);
		}

		ArrayList<Entry> entries;
		if (header.type == BinaryCodec.BINARY)
		{
			entries = mStructLookup.get(header.value);
		}
		else
		{
			entries = new ArrayList<>();
			for (int i = 0; i < header.value; i++)
			{
				Entry entry = readEntry();
				entry.name = readUTF(entry.value);
				entries.add(entry);
			}
			mStructLookup.add(entries);
		}

		for (Entry entry : entries)
		{
			aDocument.put(entry.name, readValue(entry.type));
		}

		mObjectLookup.add(aDocument);

		return aDocument;
	}


	Array readArray(Array aArray, Entry aEntry) throws IOException
	{
		Entry header = aEntry != null ? aEntry : readEntry();

		if (header.type == BinaryCodec.REFERENCE)
		{
			return (Array)mObjectLookup.get(header.value);
		}

		for (int i = 0; i < header.value;)
		{
			Entry entry = readEntry();

			for (int j = 0; j < entry.value; j++, i++)
			{
				aArray.add(readValue(entry.type));
			}
		}

		mObjectLookup.add(aArray);

		return aArray;
	}


	private Object readValue(BinaryCodec aType) throws IOException
	{
		Object value;
		switch (aType)
		{
			case DOCUMENT:
				value = readDocument(new Document(), null);
				break;
			case ARRAY:
				value = readArray(new Array(), null);
				break;
			case REFERENCE:
				throw new IllegalStateException();
//				value = mObjectLookup.get((int)readUnsignedVarint());
//				break;
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
		token.type = BinaryCodec.values()[(int)params];

		return token;
	}


	static class Entry
	{
		BinaryCodec type;
		int value;
		String name;


		@Override
		public String toString()
		{
			return "{type=" + type + ", value=" + value + ", name=" + name + "}";
		}
	}

//	public static enum VisitorResult
//	{
//		TERMINATE,
//		SKIP,
//		SKIP_SIBLINGS,
//		SKIP_SUBTREE,
//		CONTINUE;
//
//
//		boolean isSkip()
//		{
//			switch (this)
//			{
//				case VisitorResult.SKIP_SUBTREE:
//				case VisitorResult.SKIP_SIBLINGS:
//				case VisitorResult.SKIP:
//					return true;
//				default:
//					return false;
//			}
//		}
//	}
}
