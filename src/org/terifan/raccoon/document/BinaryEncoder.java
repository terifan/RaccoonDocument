package org.terifan.raccoon.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map.Entry;
import org.terifan.raccoon.document.BinaryDecoder.Field;
import static org.terifan.raccoon.document.BinaryType.ARRAY;
import static org.terifan.raccoon.document.BinaryType.DOCUMENT;
import static org.terifan.raccoon.document.BinaryType.STRING;


public class BinaryEncoder extends BinaryOutputStream implements AutoCloseable
{
	private LookupMap<String> mStrings;
	private LookupMap<ByteKey> mArrHeaders;
	private LookupMap<ByteKey> mDocHeaders;
	private LookupMap<Array> mArrInstances;
	private LookupMap<Document> mDocInstances;


	public BinaryEncoder(OutputStream aOutputStream)
	{
		super(aOutputStream);

		mStrings = new LookupMap<>(true);
		mArrHeaders = new LookupMap<>(true);
		mDocHeaders = new LookupMap<>(true);
		mArrInstances = new LookupMap<>(true);
		mDocInstances = new LookupMap<>(true);
	}


	public BinaryEncoder writeField(String aName, Object aObject) throws IOException
	{
		writeType(BinaryType.FIELD);
		writeValue(BinaryType.FIELD, new Field(aName, BinaryType.identify(aObject), aObject));
		return this;
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
		int n = mDocInstances.indexOf(aDocument);
		if (n >= 0)
		{
			writeUnsignedVarint(n << 2);
			return;
		}

		mDocInstances.add(aDocument);
		byte[] header = createHeader(aDocument);
		ByteKey key = new ByteKey(header);
		int ref = mDocHeaders.indexOf(key);

		if (ref != -1)
		{
			writeUnsignedVarint((ref << 2) + 1);
		}
		else
		{
			writeUnsignedVarint((header.length << 2) + 2);
			write(header);
			mDocHeaders.add(key);
		}

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
		int n = mArrInstances.indexOf(aArray);
		if (n >= 0)
		{
			writeUnsignedVarint(n << 2);
			return;
		}

		mArrInstances.add(aArray);
		byte[] header = createHeader(aArray);
		ByteKey key = new ByteKey(header);
		int ref = mArrHeaders.indexOf(key);

		if (ref != -1)
		{
			writeUnsignedVarint((ref << 2) + 1);
		}
		else
		{
			writeUnsignedVarint((header.length << 2) + 2);
			write(header);
			mArrHeaders.add(key);
		}

		BinaryInputStream fields = new BinaryInputStream(new ByteArrayInputStream(header));

		int offset = 0;
		for (BinaryType type; (type = fields.readType()) != BinaryType.TERMINATOR;)
		{
			int runLen = fields.readUnsignedVarint();

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
				writeString(mStrings, (String)value);
				break;
			default:
				type.encoder.encode(this, value);
				break;
		}
	}


	private byte[] createHeader(Document aDocument) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BinaryOutputStream bos = new BinaryOutputStream(baos);
		for (Entry<String, Object> entry : aDocument.entrySet())
		{
			Object value = entry.getValue();
			BinaryType type = BinaryType.identify(value);
			bos.writeType(type);
			bos.writeString(entry.getKey());
		}

		return baos.toByteArray();
	}


	private byte[] createHeader(Array aArray) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BinaryOutputStream bos = new BinaryOutputStream(baos);
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

		return baos.toByteArray();
	}
}
