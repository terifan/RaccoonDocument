package org.terifan.raccoon.document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map.Entry;
import static org.terifan.raccoon.document.BinaryType.ARRAY;
import static org.terifan.raccoon.document.BinaryType.DOCUMENT;
import static org.terifan.raccoon.document.BinaryType.STRING;


class BinaryEncoder extends BinaryOutputStream implements AutoCloseable
{
	private Lookup mDocStructs = new Lookup(true);
	private Lookup mArrStructs = new Lookup(true);
	private HashMap<BinaryType, LRU<?>> mValueLookup = new HashMap<>();


	public BinaryEncoder(OutputStream aOutputStream)
	{
		super(aOutputStream);
	}


	public void writeObject(Object aObject) throws IOException
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
// string,4,int,float,string,uuid

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
				LRU lookup = mValueLookup.computeIfAbsent(type, t->new LRU<String>(true));

				int ref = lookup.indexOf(s);
				if (ref == -1)
				{
					writeVarint(s.length());
					writeUTF(s);
					lookup.add(s);
				}
				else
				{
					writeVarint(-ref-1);
				}
				break;
			}
//			case INT:
//			{
//				Lookup<Object> lookup = mValueLookup.computeIfAbsent(type, t->new Lookup<>());
//
//				int ref = lookup.indexOf(value);
//				if (ref == -1)
//				{
//					lookup.add(value);
//					type.encoder.encode(this, (Integer)value*2);
//				}
//				else
//				{
//					writeVarint(ref*2+1);
//				}
//				break;
//			}
			default:
				type.encoder.encode(this, value);
//				Lookup<Object> lookup = mValueLookup.computeIfAbsent(type, t->new Lookup<>());
//
//				int ref = lookup.indexOf(value);
//				if (ref == -1)
//				{
//					writeVarint(0);
//					lookup.add(value);
//					type.encoder.encode(this, value);
//				}
//				else
//				{
//					writeVarint(ref);
//				}
				break;
		}
	}
}
