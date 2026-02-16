package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Objects;
import static org.terifan.raccoon.document.BinaryType.ARRAY;
import static org.terifan.raccoon.document.BinaryType.DOCUMENT;
import static org.terifan.raccoon.document.BinaryType.STRING;


public class BinaryEncoder extends BinaryOutputStream implements AutoCloseable
{
	private IndexLookup<String> mStrings;
	private IndexLookup<ArrHeader> mArrHeaders;
	private IndexLookup<DocHeader> mDocHeaders;
	private IndexLookup<Array> mArrInstances;
	private IndexLookup<Document> mDocInstances;


	public BinaryEncoder(OutputStream aOutputStream)
	{
		super(aOutputStream);

		mStrings = new IndexLookup<>();
		mArrHeaders = new IndexLookup<>();
		mDocHeaders = new IndexLookup<>();
		mArrInstances = new IndexLookup<>();
		mDocInstances = new IndexLookup<>();
	}


	public BinaryEncoder writeObject(Object aObject) throws IOException
	{
		if (aObject instanceof Document v)
		{
			writeType(BinaryType.DOCUMENT);
			writeDocument(v);
		}
		else if (aObject instanceof Array v)
		{
			writeType(BinaryType.ARRAY);
			writeArray(v);
		}
		else
		{
			BinaryType type = BinaryType.identify(aObject);
			writeType(type);
			writeValue(type, aObject);
		}
		return this;
	}


	void writeDocument(Document aDocument) throws IOException
	{
		int ref = mDocInstances.lookup(aDocument);
		if (ref >= 0)
		{
			writeUnsignedVarint(ref << 2);
		}
		else
		{
			mDocInstances.add(aDocument);

			DocHeader header = new DocHeader(aDocument);
			ref = mDocHeaders.lookup(header);
			BinaryType[] types = header.types;
			String[] names = header.names;

			if (ref >= 0)
			{
				writeUnsignedVarint((ref << 2) + 1);
			}
			else
			{
				writeUnsignedVarint((types.length << 2) + 2);

				for (int i = 0; i < types.length; i++)
				{
					writeType(types[i]);
					writeString(names[i]);
				}

				mDocHeaders.add(header);
			}

			for (int i = 0; i < types.length; i++)
			{
				writeValue(types[i], aDocument.get(names[i]));
			}
		}
	}


	void writeArray(Array aArray) throws IOException
	{
		int n = mArrInstances.lookup(aArray);
		if (n >= 0)
		{
			writeUnsignedVarint(n << 2);
			return;
		}

		mArrInstances.add(aArray);
		ArrHeader header = new ArrHeader(aArray);
		int ref = mArrHeaders.lookup(header);
		ArrayList<BinaryType> types = header.types;
		ArrayList<Integer> lengths = header.lengths;

		if (ref != -1)
		{
			writeUnsignedVarint((ref << 2) + 1);
		}
		else
		{
			writeUnsignedVarint((types.size() << 2) + 2);

			for (int i = 0; i < types.size(); i++)
			{
				writeType(types.get(i));
				writeUnsignedVarint(lengths.get(i));
			}

			mArrHeaders.add(header);
		}

		int offset = 0;
		for (int i = 0; i < types.size(); i++)
		{
			for (int runLen = lengths.get(i); --runLen >= 0; )
			{
				writeValue(types.get(i), aArray.get(offset++));
			}
		}
	}


	private void writeValue(BinaryType type, Object value) throws IOException
	{
		switch (type)
		{
			case DOCUMENT:
				writeDocument((Document)value);
				break;
			case ARRAY:
				writeArray((Array)value);
				break;
			case STRING:
				writeString(mStrings, (String)value);
				break;
			default:
				type.encoder.encode(this, value);
				break;
		}
	}


	private class ArrHeader
	{
		ArrayList<BinaryType> types = new ArrayList<>();
		ArrayList<Integer> lengths = new ArrayList<>();


		ArrHeader(Array aArray) throws IOException
		{
			for (int offset = 0; offset < aArray.size();)
			{
				int runLen = 0;
				BinaryType type = null;

				for (int i = offset; i < aArray.size(); i++)
				{
					Object value = aArray.get(i);
					BinaryType tmp = BinaryType.identify(value);

					if (type != tmp && type != null)
					{
						break;
					}

					runLen++;
					type = tmp;
				}

				types.add(type);
				lengths.add(runLen);
				offset += runLen;
			}
		}


		@Override
		public int hashCode()
		{
			int hash = 3;
			hash = 53 * hash + Objects.hashCode(this.types);
			hash = 53 * hash + Objects.hashCode(this.lengths);
			return hash;
		}


		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
			{
				return true;
			}
			final ArrHeader other = (ArrHeader)obj;
			if (!Objects.equals(this.types, other.types))
			{
				return false;
			}
			return Objects.equals(this.lengths, other.lengths);
		}
	}


	private class DocHeader
	{
		BinaryType[] types;
		String[] names;


		public DocHeader(Document aDocument)
		{
			types = new BinaryType[aDocument.size()];
			names = new String[aDocument.size()];

			int i = 0;
			for (Entry<String, Object> entry : aDocument.entrySet())
			{
				types[i] = BinaryType.identify(entry.getValue());
				names[i++] = entry.getKey();
			}
		}


		@Override
		public int hashCode()
		{
			int hash = 7;
			hash = 53 * hash + Arrays.deepHashCode(this.types);
			hash = 53 * hash + Arrays.deepHashCode(this.names);
			return hash;
		}


		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
			{
				return true;
			}
			final DocHeader other = (DocHeader)obj;
			if (!Arrays.deepEquals(this.types, other.types))
			{
				return false;
			}
			return Arrays.deepEquals(this.names, other.names);
		}
	}


	@SuppressWarnings("serial")
	static class IndexLookup<T> extends HashMap<T, Integer>
	{
		public void add(T aValue)
		{
			put(aValue, size());
		}


		public int lookup(T aValue)
		{
			return getOrDefault(aValue, -1);
		}
	}
}
