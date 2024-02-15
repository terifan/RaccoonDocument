package org.terifan.raccoon.document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map.Entry;
import java.util.UUID;


class YMLEncoder
{
	private StringBuilder mBuffer;
	private boolean mPendingLineBreak;


	public String marshal(KeyValueContainer aValue)
	{
		mBuffer = new StringBuilder();

		if (aValue instanceof Document v)
		{
			marshalDocument(v, "");
		}
		else if (aValue instanceof Array v)
		{
			marshalArray(v, "");
		}

		return mBuffer.toString();
	}


	private void marshalDocument(Document aDocument, String aIndent)
	{
		for (Entry<String, Object> entry : aDocument.entrySet())
		{
			print(aIndent + escapeString(entry.getKey(), true) + ":");

			Object value = entry.getValue();
			if (value instanceof Document v)
			{
				print("\n");
				marshalDocument(v, aIndent + "  ");
			}
			else if (value instanceof Array v)
			{
				print("\n");
				marshalArray(v, aIndent.replace('-', ' '));
			}
			else
			{
				print(" ");
				marshalValue(value);
				mPendingLineBreak = true;
			}
			aIndent = aIndent.replace('-', ' ');
		}
	}


	private void marshalArray(Array aArray, String aIndent)
	{
		for (Object entry : aArray)
		{
			Object value = entry;
			if (value instanceof Document v)
			{
				marshalDocument(v, aIndent + "- ");
			}
			else if (value instanceof Array v)
			{
				marshalArray(v, aIndent + "- ");
			}
			else
			{
				print(aIndent + "- ");
				marshalValue(value);
				mPendingLineBreak = true;
			}
			aIndent = aIndent.replace('-', ' ');
		}
	}


	private void marshalValue(Object aValue)
	{
		if (aValue instanceof String v)
		{
			print(escapeString(v, false));
		}
		else if (aValue == null)
		{
			print("null");
		}
		else if (aValue instanceof ObjectId v)
		{
			print("0x" + v + " #" + aValue.getClass().getSimpleName());
		}
		else if (aValue instanceof byte[] v)
		{
			print(Base64.getEncoder().withoutPadding().encodeToString(v) + " #Binary");
		}
		else if (aValue instanceof Character v)
		{
			print(escapeChar(v));
		}
		else if (aValue instanceof Double v)
		{
			print(formatDecimal(v));
		}
		else if (aValue instanceof Float v)
		{
			print(formatDecimal(v));
		}
		else if (aValue instanceof BigDecimal
			|| aValue instanceof UUID
			|| aValue instanceof LocalDate
			|| aValue instanceof LocalTime
			|| aValue instanceof LocalDateTime
			|| aValue instanceof OffsetDateTime
			|| aValue instanceof byte[])
		{
			print(escapeString(aValue.toString(), false) + " #" + aValue.getClass().getSimpleName());
		}
		else if (aValue instanceof Number || aValue instanceof Boolean) // note: bigdecimal is number
		{
			print(aValue);
		}
		else
		{
			throw new IllegalArgumentException("Unsupported type: " + aValue.getClass());
		}
	}


	private void print(Object aText)
	{
		if (mPendingLineBreak)
		{
			mPendingLineBreak = false;
			mBuffer.append("\n");
		}
		mBuffer.append(aText);
	}


	private String escapeString(String aString, boolean aKey)
	{
		StringBuilder sb = new StringBuilder();
		boolean escaped = false;
		boolean dash1 = false;
		for (char c : aString.toCharArray())
		{
			String s = escapeChar(c);
			escaped |= s.length() > 1 || dash1 && c == '-';
			dash1 |= c == '-';
			sb.append(s);
		}
		if (escaped || aString.startsWith(" ") || aString.endsWith(" ") || aString.startsWith("\"") || aString.endsWith("\"") || aKey && aString.contains(":"))
		{
			sb.append("'").insert(0, "'");
		}
		return sb.toString();
	}


	private String escapeChar(char c)
	{
		switch (c)
		{
			case '\'':
				return "\\'";
			case '\\':
				return "\\\\";
			case '\n':
				return "\\n";
			case '\r':
				return "\\r";
			case '\t':
				return "\\t";
			case '\b':
				return "\\b";
			case '\f':
				return "\\f";
			default:
				if (c >= ' ')
				{
					return Character.toString(c);
				}
				return String.format("\\u%04X", (int)c);
		}
	}


	private String formatDecimal(Object aText)
	{
		String text = aText.toString();

		if (aText instanceof Double || aText instanceof Float)
		{
			text = text.replace(" ", "");

			int i0 = text.indexOf(',');
			if (i0 != -1)
			{
				int i1 = text.indexOf('.');
				if (i1 != -1)
				{
					if (i0 < i1)
					{
						text = text.replace(",", ""); // handles: 10,000.7
					}
					else
					{
						text = text.replace(".", "").replace(',', '.'); // handles: 10.000,7
					}
				}
				else
				{
					text = text.replace(',', '.'); // handles: 10000.7
				}
			}

			if (text.endsWith(".0"))
			{
				text = text.substring(0, text.length() - 2);
			}
		}

		return text;
	}
}
