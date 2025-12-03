package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.InputStream;
import static org.terifan.raccoon.document.BinaryCodec.ARRAY;
import static org.terifan.raccoon.document.BinaryCodec.DOCUMENT;
import static org.terifan.raccoon.document.BinaryCodec.TERMINATOR;


public class BinaryDecoder extends BinaryInputStream
{
	BinaryDecoder(InputStream aInputStream)
	{
		super(aInputStream);
	}


	Object unmarshal() throws IOException
	{
		Token token = readToken();

		switch (token.type)
		{
			case DOCUMENT:
				Document d = new Document();
				readDocument(d, VisitorResult.CONTINUE);
				return d;
			case ARRAY:
				Array a = new Array();
				readArray(a, VisitorResult.CONTINUE);
				return a;
			case TERMINATOR:
				return token.type;
			default:
				return readValue(token.type, VisitorResult.CONTINUE);
		}
	}


	void unmarshal(Collection aContainer) throws IOException
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

			readDocument(v, VisitorResult.CONTINUE);
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

			readArray(v, VisitorResult.CONTINUE);
		}
		else
		{
			throw new StreamException("Stream corrupted.");
		}
	}


	Token readToken() throws IOException
	{
		long params = readInterleaved();

		Token token = new Token();
		token.value = (int)(params >>> 32);
		token.type = BinaryCodec.values()[(int)params];

		return token;
	}


	Document readDocument(Document aDocument, VisitorResult aState) throws IOException
	{
		for (;;)
		{
			Token token = readToken();
			if (token.type == BinaryCodec.TERMINATOR)
			{
				break;
			}

			String key = readUTF(token.value);
			Object value = readValue(token.type, aState);
			aDocument.putImpl(key, value);
		}

		return aDocument;
	}


	Array readArray(Array aArray, VisitorResult aState) throws IOException
	{
		for (;;)
		{
			Token token = readToken();

			if (token.type == BinaryCodec.TERMINATOR)
			{
				break;
			}

			for (int i = 0; i < token.value; i++)
			{
				aArray.add(readValue(token.type, aState));
			}
		}

		return aArray;
	}


	private Object readValue(BinaryCodec aType, VisitorResult aState) throws IOException
	{
		switch (aType)
		{
			case DOCUMENT:
				Document d = new Document();
				readDocument(d, aState);
				return d;
			case ARRAY:
				Array a = new Array();
				readArray(a, aState);
				return a;
			default:
				return aType.decoder.decode(this, aState);
		}
	}


	static class Token
	{
		int value;
		BinaryCodec type;


		@Override
		public String toString()
		{
			return type.name();
		}
	}


	public static enum VisitorResult
	{
		TERMINATE,
		SKIP,
		SKIP_SIBLINGS,
		SKIP_SUBTREE,
		CONTINUE;


		boolean isSkip()
		{
			switch (this)
			{
				case VisitorResult.SKIP_SUBTREE:
				case VisitorResult.SKIP_SIBLINGS:
				case VisitorResult.SKIP:
					return true;
				default:
					return false;
			}
		}
	}
}
