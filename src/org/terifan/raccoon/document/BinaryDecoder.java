package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;
import static org.terifan.raccoon.document.BinaryCodec.ARRAY;
import static org.terifan.raccoon.document.BinaryCodec.DOCUMENT;


public class BinaryDecoder extends BinaryInputStream
{
	private ArrayList<Object> mObjectLookup;
	private ArrayList<ArrayList<Entry>> mDocStructLookup;
	private ArrayList<ArrayList<Entry>> mArrStructLookup;
	private ArrayList<String> mStringLookup;
	private ArrayList<String> mKeyLookup;
	private ArrayList<Object> mValueLookup;


	BinaryDecoder(InputStream aInputStream)
	{
		super(aInputStream);
	}


	Object unmarshal() throws IOException
	{
		mObjectLookup = new ArrayList<>();
		mDocStructLookup = new ArrayList<>();
		mArrStructLookup = new ArrayList<>();
		mStringLookup = new ArrayList<>();
		mKeyLookup = new ArrayList<>();
		mValueLookup = new ArrayList<>();

		Entry entry = readEntry();

		switch (entry.type)
		{
			case DOCUMENT:
				return readDocument(new Document(), entry);
			case ARRAY:
				return readArray(new Array(), entry);
//			case TERMINATOR:
//				return entry.type;
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
			entries = mDocStructLookup.get(header.value);
		}
		else
		{
			entries = new ArrayList<>();
			for (int i = 0; i < header.value; i++)
			{
				Entry entry = readEntry();

				if ((entry.value & 1) == 1)
				{
					entry.name = mKeyLookup.get(entry.value >> 1);
				}
				else
				{
					entry.name = readUTF(entry.value >> 1);
					mKeyLookup.add(entry.name);
				}

				entries.add(entry);
			}
			mDocStructLookup.add(entries);
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

		ArrayList<Entry> entries;
		if (header.type == BinaryCodec.BINARY)
		{
			entries = mArrStructLookup.get(header.value);
		}
		else
		{
			entries = new ArrayList<>();
			for (int i = 0; i < header.value; i++)
			{
				entries.add(readEntry());
			}
			mArrStructLookup.add(entries);
		}

		for (Entry entry : entries)
		{
			for (int i = 0; i < entry.value; i++)
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
			case COMPACT_STRING:
				value = aType.decoder.decode(this);
				mStringLookup.add((String)value);
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
			case REFERENCE:
				value = mValueLookup.get((int)readUnsignedVarint());
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
		token.type = BinaryCodec.values()[(int)params];

		return token;
	}


	static class Entry
	{
		BinaryCodec type;
		int value;
		String name;
		Object object;


		public Entry()
		{
		}


		public Entry(BinaryCodec aType, int aValue)
		{
			this.type = aType;
			this.value = aValue;
		}


		public Entry(BinaryCodec aType, Object aObject)
		{
			this.type = aType;
			this.object = aObject;
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
