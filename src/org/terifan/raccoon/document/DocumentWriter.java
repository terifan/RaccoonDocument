package org.terifan.raccoon.document;

import java.io.IOException;


public class DocumentWriter
{
	private BinaryEncoder mEncoder;


	public DocumentWriter(BinaryEncoder aEncoder)
	{
		mEncoder = aEncoder;
	}


	public DocumentWriter beginDocument() throws IOException
	{
		mEncoder.writeInterleaved(BinaryCodec.DOCUMENT, 0);
		return this;
	}


	public DocumentWriter endDocument() throws IOException
	{
		mEncoder.writeInterleaved(BinaryCodec.TERMINATOR, 0);
		return this;
	}


	public DocumentWriter beginArray() throws IOException
	{
		mEncoder.writeInterleaved(BinaryCodec.ARRAY, 0);
		return this;
	}


	public DocumentWriter endArray() throws IOException
	{
		mEncoder.writeInterleaved(BinaryCodec.TERMINATOR, 0);
		return this;
	}


	public DocumentWriter name(String aName) throws IOException
	{
		mEncoder.writeInterleaved(BinaryCodec.FIELD, 0);
		mEncoder.writeString(aName);
		return this;
	}


	public DocumentWriter nullValue() throws IOException
	{
		mEncoder.writeInterleaved(BinaryCodec.NULL, 0);
		return this;
	}


	public DocumentWriter value(Object aValue) throws IOException
	{
		BinaryCodec type = BinaryCodec.identify(aValue);

		if (type == null)
		{
			if (aValue instanceof Enum)
			{
				throw new UnsupportedTypeException("Enums are not supported as they are inherently unsafe for serialization: " + aValue.getClass().getCanonicalName());
			}

			throw new UnsupportedTypeException(aValue.getClass().getCanonicalName());
		}

		mEncoder.writeField(aValue);
		return this;
	}
}
