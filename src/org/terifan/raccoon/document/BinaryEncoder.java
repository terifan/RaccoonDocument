package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map.Entry;
import static org.terifan.raccoon.document.BinaryType.ARRAY;
import static org.terifan.raccoon.document.BinaryType.DOCUMENT;
import static org.terifan.raccoon.document.BinaryType.STRING;


class BinaryEncoder extends BinaryOutputStream implements AutoCloseable
{
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
		writeUnsignedVarint(aDocument.size());

		for (Entry<String, Object> entry : aDocument.entrySet())
		{
			Object value = entry.getValue();
			BinaryType type = BinaryType.identify(value);

			writeString(entry.getKey());
			writeType(type);

			switch (type)
			{
				case DOCUMENT:
					writeDocument((Document)value);
					break;
				case ARRAY:
					writeArray((Array)value);
					break;
				case STRING:
					writeString((String)value);
					break;
				default:
					type.encoder.encode(this, value);
					break;
			}
		}
	}


	void writeArray(Array aArray) throws IOException
	{
		writeUnsignedVarint(aArray.size());

		for (int offset = 0; offset < aArray.size();)
		{
			int runLen = 0;
			BinaryType nextType = null;

			for (int i = offset; i < aArray.size(); i++)
			{
				Object value = aArray.get(i);
				BinaryType type = BinaryType.identify(value);

				if (nextType != type && nextType != null)
				{
					break;
				}

				runLen++;
				nextType = type;
			}

			writeType(nextType);
			writeUnsignedVarint(runLen);

			for (; --runLen >= 0 && offset < aArray.size(); )
			{
				writeValue(nextType, aArray.get(offset++));
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
				writeString((String)value);
				break;
			default:
				type.encoder.encode(this, value);
				break;
		}
	}


	static class ByteKey
	{
		final byte[] mBuffer;


		public ByteKey(byte[] aBuffer)
		{
			mBuffer = aBuffer;
		}


		@Override
		public String toString()
		{
			return new String(mBuffer);
		}


		@Override
		public int hashCode()
		{
			return Arrays.hashCode(mBuffer);
		}


		@Override
		public boolean equals(Object aOther)
		{
			return aOther == this || aOther instanceof ByteKey v && Arrays.equals(mBuffer, v.mBuffer);
		}
	}
}
