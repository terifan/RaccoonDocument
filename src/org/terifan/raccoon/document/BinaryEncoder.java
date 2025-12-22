package org.terifan.raccoon.document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map.Entry;
import static org.terifan.raccoon.document.BinaryType.ARRAY;
import static org.terifan.raccoon.document.BinaryType.DOCUMENT;
import static org.terifan.raccoon.document.BinaryType.STRING;


public class BinaryEncoder extends BinaryOutputStream implements AutoCloseable
{
	public static boolean REFS;

	private Lookup mDocStructs = new Lookup(true);
	private Lookup mArrStructs = new Lookup(true);
	private LookupMap<String> mStringLookup = new LookupMap<String>(true);


	public BinaryEncoder(OutputStream aOutputStream)
	{
		super(aOutputStream);
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
		BufferedBinaryOutputStream bos = new BufferedBinaryOutputStream();
		for (Entry<String, Object> entry : aDocument.entrySet())
		{
			Object value = entry.getValue();
			BinaryType type = BinaryType.identify(value);
			bos.writeType(type);
			bos.writeString(entry.getKey());
		}

		byte[] header = bos.finish();

		mDocStructs.write(this, header);

		BinaryInputStream fields = new BinaryInputStream(new ByteArrayInputStream(header));

		for (BinaryType type; (type = fields.readType()) != BinaryType.TERMINATOR;)
		{
			String name = fields.readString();
			Object value = aDocument.get(name);

			writeValue(type, value);
		}
	}


	void writeArray(Array aArray) throws IOException
	{
		BufferedBinaryOutputStream bos = new BufferedBinaryOutputStream();
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

			bos.writeType(type);
			bos.writeUnsignedVarint(runLen);
			offset += runLen;
		}

		byte[] header = bos.finish();

		mArrStructs.write(this, header);

		BinaryInputStream fields = new BinaryInputStream(new ByteArrayInputStream(header));

		int offset = 0;
		for (BinaryType type; (type = fields.readType()) != BinaryType.TERMINATOR;)
		{
			long runLen = fields.readUnsignedVarint();

			while (--runLen >= 0)
			{
				writeValue(type, aArray.get(offset++));
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
			case NULL:
			case BYTE:
			case BOOLEAN:
				type.encoder.encode(this, value);
				break;
			case STRING:
			{
				String s = (String)value;
				int ref = mStringLookup.indexOf(s);
				if (ref == -1)
				{
					writeVarint(s.length());
					writeUTF(s);
					mStringLookup.add(s);
				}
				else
				{
					writeVarint(-ref-1);
				}
				break;
			}
			default:
				type.encoder.encode(this, value);
				break;
		}
	}
}
