package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import static org.terifan.raccoon.document.SupportedTypes.ARRAY;
import static org.terifan.raccoon.document.SupportedTypes.DOCUMENT;
import static org.terifan.raccoon.document.SupportedTypes.TERMINATOR;


class BinaryDecoder extends BinaryInput
{
	public BinaryDecoder(InputStream aInputStream)
	{
		super(aInputStream);
	}


	@Override
	Object readValue(SupportedTypes aType) throws IOException
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


	void unmarshal(KeyValueCollection aContainer) throws IOException
	{
		Token token = readToken();

		if (aContainer instanceof Document)
		{
			if (token.type == SupportedTypes.ARRAY)
			{
				throw new StreamException("Attempt to unmarshal a Document when binary stream contains an Array.");
			}
			if (token.type != SupportedTypes.DOCUMENT)
			{
				throw new StreamException("Stream corrupted.");
			}

			readDocument((Document)aContainer);
		}
		else
		{
			if (token.type == SupportedTypes.DOCUMENT)
			{
				throw new StreamException("Attempt to unmarshal an Array when binary stream contains a Document.");
			}
			if (token.type != SupportedTypes.ARRAY)
			{
				throw new StreamException("Stream corrupted.");
			}

			readArray((Array)aContainer);
		}
	}


	Object unmarshal() throws IOException
	{
		Token token = readToken();

		switch (token.type)
		{
			case DOCUMENT:
				return readDocument(new Document());
			case ARRAY:
				return readArray(new Array());
			case TERMINATOR:
				return token.type;
			default:
				return readValue(token.type);
		}
	}


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
