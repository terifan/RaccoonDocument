package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.LinkedList;
import java.util.UUID;


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

			LinkedList<Collection> history = new LinkedList<>();
			history.add(aContainer);

			switch (aContainer)
			{
				case Document v:
					return (T)readDocument(history, v);
				case Array v:
					return (T)readArray(history, v);
				default:
					throw new IllegalArgumentException();
			}
		}
		catch (IOException e)
		{
			throw new IllegalArgumentException(e);
		}
	}


	private Document readDocument(LinkedList<Collection> aHistory, Document aDocument) throws IOException
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

			aDocument.putImpl(key, readValue(aHistory, readChar()));
		}

		return aDocument;
	}


	private Array readArray(LinkedList<Collection> aHistory, Array aArray) throws IOException
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
				aArray.add(readValue(aHistory, c));
			}
			catch (UnsupportedEncodingException e)
			{
				// ignore, an array ending with a delimiter will cause this intentionally
			}
		}

		return aArray;
	}


	private Object readValue(LinkedList<Collection> aHistory, char aChar) throws IOException
	{
		switch (aChar)
		{
			case '[':
				Array arr = new Array();
				aHistory.add(arr);
				readArray(aHistory, arr);
				aHistory.removeLast();
				return arr;
			case '{':
				Document doc = new Document();
				aHistory.add(doc);
				readDocument(aHistory, doc);
				aHistory.removeLast();
				return doc;
			case '\"':
			case '\'':
				String s = readString(aChar);
				if (s.startsWith("{{{CyclicReferece:") && s.endsWith("}}}"))
				{
					return aHistory.get(Integer.parseInt(s.substring(18, s.length() - 3)));
				}
				return s;
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

		Object o = decode(in);
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


	/**
	 * Decodes an encoded value e.g. "ObjectId(65dc9ad1b09c81b0e278e2c2)" return an instance of ObjectId. Unsupported types return null.
	 */
	private static Object decode(String aText)
	{
		if ("true".equalsIgnoreCase(aText))
		{
			return true;
		}
		if ("false".equalsIgnoreCase(aText))
		{
			return false;
		}
		if (aText.startsWith("ObjectId("))
		{
			return ObjectId.fromString(aText.substring(9, aText.length() - 1));
		}
		if (aText.startsWith("Base64("))
		{
			return Base64.getDecoder().decode(aText.substring(7, aText.length() - 1));
		}
		if (aText.startsWith("UUID("))
		{
			return UUID.fromString(aText.substring(5, aText.length() - 1));
		}
		if (aText.startsWith("BigInteger("))
		{
			return new BigInteger(aText.substring(11, aText.length() - 1));
		}
		if (aText.startsWith("BigDecimal("))
		{
			return new BigDecimal(aText.substring(11, aText.length() - 1));
		}
		if (aText.startsWith("LocalDateTime("))
		{
			return LocalDateTime.parse(aText.substring(14, aText.length() - 1));
		}
		if (aText.startsWith("LocalDate("))
		{
			return LocalDate.parse(aText.substring(10, aText.length() - 1));
		}
		if (aText.startsWith("LocalTime("))
		{
			return LocalTime.parse(aText.substring(10, aText.length() - 1));
		}
		if (aText.startsWith("OffsetDateTime("))
		{
			return OffsetDateTime.parse(aText.substring(15, aText.length() - 1));
		}
		if (aText.startsWith("OffsetTime("))
		{
			return OffsetTime.parse(aText.substring(11, aText.length() - 1));
		}
		if (aText.startsWith("ZonedDateTime("))
		{
			return ZonedDateTime.parse(aText.substring(14, aText.length() - 1));
		}
		if (aText.startsWith("Duration("))
		{
			return Duration.parse(aText.substring(9, aText.length() - 1));
		}
		if (aText.startsWith("0x"))
		{
			return Long.valueOf(aText.substring(2), 16);
		}
		if (aText.matches("[+-]?(\\d*\\.)?\\d+[f|F]"))
		{
			return Float.valueOf(aText.substring(0, aText.length() - 1));
		}
		if (aText.matches("[+-]?[0-9]{1,}[L|l]"))
		{
			return Long.valueOf(aText.substring(0, aText.length() - 1));
		}
		if (aText.contains("."))
		{
			return Double.valueOf(aText);
		}
		return null;
	}
}
