package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import static org.terifan.raccoon.document.SupportedTypes.ARRAY;
import static org.terifan.raccoon.document.SupportedTypes.DOCUMENT;
import static org.terifan.raccoon.document.SupportedTypes.TERMINATOR;


class BinaryIterator extends BinaryInput
{
	private ArrayDeque<Token> mType;
	private ArrayDeque<Token> mArrayType;
	private ArrayDeque<Integer> mArrayRemainings;


	public BinaryIterator(InputStream aInputStream)
	{
		super(aInputStream);
		mArrayRemainings = new ArrayDeque<>();
	}


	public boolean hasNext()
	{
		return mType == null || !mType.isEmpty();
	}


	public Value next() throws IOException
	{
		if (mType != null && !mType.isEmpty() && mType.peekLast().type == ARRAY)
		{
			Token componentType = mArrayType.peekLast();

			System.out.println("///"+componentType);

			if (mArrayRemainings.peekLast() > 1)
			{
				mArrayRemainings.addLast(mArrayRemainings.removeLast() - 1);
			}
			else
			{
				mArrayRemainings.removeLast();
			}

			if (componentType.type != DOCUMENT && componentType.type != ARRAY)
			{
				return new Value(componentType, null, readValue(componentType.type));
			}
		}

		Token token = readToken();

		if (mType == null)
		{
			mType = new ArrayDeque<>();
			mArrayType = new ArrayDeque<>();
		}

		String name = null;
		if (token.type != TERMINATOR && !mType.isEmpty())
		{
			if (mType.peek().type == DOCUMENT)
			{
				name = readUTF(token.value);
			}
		}

		switch (token.type)
		{
			case ARRAY:
				mType.addLast(token);
				return new Value(token, name, null);
			case DOCUMENT:
				mType.addLast(token);
				return new Value(token, name, null);
			case TERMINATOR:
				mType.removeLast();
				return new Value(token, name, null);
			default:
				return new Value(token, name, readValue(token.type));
		}
	}


	record Value(Token token, String name, Object value){};


	@Override
	Object readValue(SupportedTypes aType) throws IOException
	{
		return aType.decoder.decode(this);
	}
}
