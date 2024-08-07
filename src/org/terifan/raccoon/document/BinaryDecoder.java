package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import static org.terifan.raccoon.document.BinaryCodec.ARRAY;
import static org.terifan.raccoon.document.BinaryCodec.DOCUMENT;
import static org.terifan.raccoon.document.BinaryCodec.TERMINATOR;


class BinaryDecoder extends BinaryInput
{
	public BinaryDecoder(InputStream aInputStream, boolean aDecodeIdOnly)
	{
		super(aInputStream, aDecodeIdOnly);
	}


	@Override
	Object readValue(BinaryCodec aType) throws IOException
	{
		switch (aType)
		{
			case DOCUMENT:
				return readDocument(new Document());
			case ARRAY:
				return readArray(new Array());
			default:
				return aType.decoder.decode(this);
		}
	}


	void unmarshal(KeyValueContainer aContainer) throws IOException
	{
		Token token = readToken();

		if (aContainer instanceof Document v)
		{
			if (token.type == BinaryCodec.ARRAY)
			{
				throw new StreamException("Attempt to unmarshal a Document when binary stream contains an Array.");
			}
			if (token.type != BinaryCodec.DOCUMENT)
			{
				throw new StreamException("Stream corrupted.");
			}

			readDocument(v);
		}
		else if (aContainer instanceof Array v)
		{
			if (token.type == BinaryCodec.DOCUMENT)
			{
				throw new StreamException("Attempt to unmarshal an Array when binary stream contains a Document.");
			}
			if (token.type != BinaryCodec.ARRAY)
			{
				throw new StreamException("Stream corrupted.");
			}

			readArray(v);
		}
		else
		{
			throw new StreamException("Stream corrupted.");
		}
	}


	@SuppressWarnings("unchecked")
	Object unmarshal(Class aType) throws IOException
	{
		try
		{
			Token token = readToken();

			switch (token.type)
			{
				case DOCUMENT:
					return readDocument((Document)aType.getConstructor().newInstance());
				case ARRAY:
					return readArray((Array)aType.getConstructor().newInstance());
				case TERMINATOR:
					return token.type;
				default:
					return readValue(token.type);
			}
		}
		catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e)
		{
			throw new IOException(e);
		}
	}
}
