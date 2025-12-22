package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;


public class JSONDecoder
{
	private PushbackReader mReader;
	private boolean mRestoreShortValues;


	public JSONDecoder()
	{
	}


	public JSONDecoder setRestoreShortValues(boolean aRestoreShortValues)
	{
		mRestoreShortValues = aRestoreShortValues;
		return this;
	}


	/**
	 * @param aContainer either null or an instance of Document or Array that will be appended to.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Collection> T unmarshal(Reader aJSON, T aContainer)
	{
		mReader = new PushbackReader(aJSON, 1);

		try
		{
			if (aContainer == null)
			{
				int c = mReader.read();
				if (c == '{')
				{
					aContainer = (T)new Document();
				}
				else if (c == '[')
				{
					aContainer = (T)new Array();
				}
				else
				{
					throw new IllegalArgumentException("First character in JSON must be a bracket or a curly bracket.");
				}
			}
			else
			{
				int c = mReader.read();
				if (c != '[' && c != '{')
				{
					mReader.unread(c);
				}
			}

			switch (aContainer)
			{
				case Document v:
					return (T)readDocument(v);
				case Array v:
					return (T)readArray(v);
				default:
					throw new IllegalArgumentException();
			}
		}
		catch (IOException e)
		{
			throw new IllegalArgumentException(e);
		}
	}


	private Document readDocument(Document aDocument) throws IOException
	{
		for (int i = 0;; i++)
		{
			char c = readChar();

			if (c == '}')
			{
				break;
			}
			if (i > 0)
			{
				if (c != ',')
				{
					throw new IOException("Expected comma between elements: " + c);
				}

				c = readChar();
			}

			if (c == '}') // allow badly formatted json with unneccessary commas before ending brace
			{
				break;
			}
			if (c != '\"' && c != '\'')
			{
				mReader.unread(c);
			}

			String key = readString(c);

			if (readChar() != ':')
			{
				throw new IOException("Expected colon sign after key: " + key);
			}

			aDocument.putImpl(key, readValue(readChar()));
		}

		return aDocument;
	}


	private Array readArray(Array aArray) throws IOException
	{
		for (int i = 0;; i++)
		{
			char c = readChar();

			if (c == ']')
			{
				break;
			}
			if (c == ':')
			{
				throw new IOException("Found colon after element in array");
			}

			if (i > 0)
			{
				if (c != ',')
				{
					throw new IOException("Expected comma between elements: found: " + c);
				}

				c = readChar();
			}

			try
			{
				aArray.add(readValue(c));
			}
			catch (UnsupportedEncodingException e)
			{
				// ignore, an array ending with a delimiter will cause this intentionally
			}
		}

		return aArray;
	}


	private Object readValue(char aChar) throws IOException
	{
		switch (aChar)
		{
			case '[':
				return readArray(new Array());
			case '{':
				return readDocument(new Document());
			case '\"':
			case '\'':
				return readString(aChar);
			default:
				mReader.unread(aChar);
				return readLiteral();
		}
	}


	private String readString(int aTerminator) throws IOException
	{
		boolean unquoted = false;
		if (aTerminator != '\"' && aTerminator != '\'')
		{
			unquoted = true;
			aTerminator = 0;
		}

		StringBuilder sb = new StringBuilder();

		for (;;)
		{
			char c = readByte(unquoted);

			if (c == aTerminator || unquoted && (Character.isWhitespace(c) || c == ':' || c == ',' || c == '}' || c == ']'))
			{
				if (unquoted && c != 0)
				{
					mReader.unread(c);
				}
				return sb.toString();
			}
			if (c == '\\')
			{
				c = readEscapeSequence();
			}

			sb.append(c);
		}
	}


	private Object readLiteral() throws IOException
	{
		StringBuilder sb = new StringBuilder();
		boolean terminator = false;

		for (;;)
		{
			char c;
			try
			{
				c = readByte();
			}
			catch (Exception e)
			{
				terminator = true;
				mReader.unread('}');
				break;
			}

			if (c == '}' || c == ']' || c == ',' || Character.isWhitespace(c))
			{
				terminator = c == '}' || c == ']';
				mReader.unread(c);
				break;
			}
			if (c == '\\')
			{
				c = readEscapeSequence();
			}

			sb.append(c);
		}

		String in = sb.toString().trim();

		if (terminator && "".equalsIgnoreCase(in))
		{
			throw new UnsupportedEncodingException();
		}

		if ("null".equalsIgnoreCase(in))
		{
			return null;
		}

		Object o = SupportedTypes.decode(in);
		if (o != null)
		{
			return o;
		}

		try
		{
			long v = Long.parseLong(in);
			if (mRestoreShortValues)
			{
				if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE)
				{
					return (byte)v;
				}
				if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE)
				{
					return (short)v;
				}
			}
			if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE)
			{
				return (int)v;
			}
			return v;
		}
		catch (NumberFormatException e)
		{
			// ignore, faster to allow an exception than to regex the value before parsing
		}

		return in;
	}


	private char readEscapeSequence() throws IOException, NumberFormatException
	{
		char c = readByte();
		switch (c)
		{
			case '\"':
				return '\"';
			case '\\':
				return '\\';
			case 'n':
				return '\n';
			case 'r':
				return '\r';
			case 't':
				return '\t';
			case 'b':
				return '\b';
			case 'f':
				return '\f';
			case 'u':
				return (char)Integer.parseInt("" + readByte() + readByte() + readByte() + readByte(), 16);
			default:
				return c;
		}
	}


	private char readChar() throws IOException
	{
		for (;;)
		{
			char c = readByte();
			if (!Character.isWhitespace(c))
			{
				return c;
			}
		}
	}


	private char readByte() throws IOException
	{
		int c = mReader.read();
		if (c == -1)
		{
			throw new IOException("Unexpected end of stream.");
		}
		return (char)c;
	}


	private char readByte(boolean aUnquoted) throws IOException
	{
		int c = mReader.read();
		if (c == -1)
		{
			if (aUnquoted)
			{
				return 0;
			}
			throw new IOException("Unexpected end of stream.");
		}
		return (char)c;
	}
}
