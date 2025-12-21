package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.OutputStream;


public class CollectionWriter //implements AutoCloseable
{
//	private BinaryEncoder mEncoder;
//
//
//	public CollectionWriter(OutputStream aOutputStream)
//	{
//		mEncoder = new BinaryEncoder(aOutputStream);
//	}
//
//
//	@Override
//	public void close() throws IOException
//	{
//		mEncoder.close();
//	}
//
//
//	public CollectionWriter beginDocument() throws IOException
//	{
//		mEncoder.writeInterleaved(BinaryCodec.DOCUMENT, 0);
//		return this;
//	}
//
//
//	public CollectionWriter endDocument() throws IOException
//	{
//		mEncoder.writeInterleaved(BinaryCodec.TERMINATOR, 0);
//		return this;
//	}
//
//
//	public CollectionWriter beginArray() throws IOException
//	{
//		mEncoder.writeInterleaved(BinaryCodec.ARRAY, 0);
//		return this;
//	}
//
//
//	public CollectionWriter endArray() throws IOException
//	{
//		mEncoder.writeInterleaved(BinaryCodec.TERMINATOR, 0);
//		return this;
//	}
//
//
//	public CollectionWriter name(String aName) throws IOException
//	{
//		mEncoder.writeInterleaved(BinaryCodec.FIELD, 0);
//		mEncoder.writeString(aName);
//		return this;
//	}
//
//
//	public CollectionWriter nullValue() throws IOException
//	{
//		mEncoder.writeInterleaved(BinaryCodec.NULL, 0);
//		return this;
//	}
//
//
//	public CollectionWriter value(Object aValue) throws IOException
//	{
//		BinaryCodec type = BinaryCodec.identify(aValue);
//
//		if (type == null)
//		{
//			if (aValue instanceof Enum)
//			{
//				throw new UnsupportedTypeException("Enums are not supported as they are inherently unsafe for serialization: " + aValue.getClass().getCanonicalName());
//			}
//
//			throw new UnsupportedTypeException(aValue.getClass().getCanonicalName());
//		}
//
//		mEncoder.writeField(aValue);
//		return this;
//	}
}
