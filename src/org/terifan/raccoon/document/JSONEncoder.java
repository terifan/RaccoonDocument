package org.terifan.raccoon.document;

import java.util.Map.Entry;
import static org.terifan.raccoon.document.SupportedTypes.escapeChar;
import static org.terifan.raccoon.document.SupportedTypes.escapeString;


class JSONEncoder
{
	private StringBuilder mBuffer;
	private boolean mTyped;
	private boolean mCompact;
	private boolean mNewLine;
	private boolean mFirst;
	private char mQuote;
	private int mIndent;


	public String marshal(KeyValueContainer aContainer, boolean aCompact, boolean aTyped, boolean aApostrophes)
	{
		mBuffer = new StringBuilder();
		mNewLine = false;
		mCompact = aCompact;
		mTyped = aTyped;
		mFirst = true;
		mQuote = aApostrophes ? '\'' : '\"';

		if (aContainer instanceof Document v)
		{
			marshalDocument(v, true);
		}
		else if (aContainer instanceof Array v)
		{
			marshalArray(v);
		}
		else
		{
			throw new IllegalArgumentException();
		}

		return mBuffer.toString();
	}


	private void marshalDocument(Document aDocument)
	{
		marshalDocument(aDocument, true);
	}


	private void marshalDocument(Document aDocument, boolean aNewLineOnClose)
	{
		int size = aDocument.size();

		if (size == 0)
		{
			print("{}");
			return;
		}

		boolean hasDocument = aDocument.size() > 5;

		for (Object entry : aDocument.values())
		{
			if (entry instanceof Document)
			{
				hasDocument = true;
				break;
			}
		}

		if (!hasDocument && !isFirst())
		{
			println();
		}

		println("{");
		indent(1);

		for (Entry<String, Object> entry : aDocument.entrySet())
		{
			print(mQuote + escapeString(entry.getKey()) + mQuote + ": ");

			marshal(entry.getValue());

			if (--size > 0)
			{
				println(hasDocument && aNewLineOnClose ? "," : ", ", false);
			}
		}

		if (aNewLineOnClose)
		{
			println();
			indent(-1);
			println("}");
		}
		else
		{
			println();
			indent(-1);
			print("}");
		}
	}


	private void marshalArray(Array aArray)
	{
		int size = aArray.size();

		if (size == 0)
		{
			println("[]");
			return;
		}

		boolean special = aArray.get(0) instanceof Document;
		boolean first = special;
		boolean shortArray = !special && aArray.size() < 10;

		for (int i = 0; shortArray && i < aArray.size(); i++)
		{
			shortArray = !(aArray.get(i) instanceof KeyValueContainer) && !(aArray.get(i) instanceof String);
		}

		if (special)
		{
			print("[");
			indent(aArray.size() > 1 ? 1 : 0);
		}
		else if (shortArray)
		{
			print("[");
		}
		else
		{
			println("[");
			indent(1);
		}

		for (Object value : aArray)
		{
			if (first)
			{
				marshalDocument((Document)value, false);

				if (--size > 0)
				{
					println(", ");
				}
			}
			else
			{
				marshal(value);

				if (--size > 0)
				{
					print(", ", false);
				}
			}

			first = false;
		}

		if (special)
		{
			indent(aArray.size() > 1 ? -1 : 0);
			println("]");
		}
		else if (shortArray)
		{
			println("]");
		}
		else
		{
			println();
			indent(-1);
			println("]");
		}
	}


	private void marshal(Object aValue)
	{
		if (aValue instanceof Document v)
		{
			marshalDocument(v);
		}
		else if (aValue instanceof Array v)
		{
			marshalArray(v);
		}
		else
		{
			marshalValue(aValue);
		}
	}


	private void marshalValue(Object aValue)
	{
		if (aValue instanceof String v)
		{
			print(mQuote + escapeString(v) + mQuote);
		}
		else if (aValue instanceof Character v)
		{
			print(mQuote + escapeChar(v) + mQuote);
		}
		else if (aValue == null)
		{
			print("null");
		}
		else if (SupportedTypes.isExtendedType(aValue))
		{
			print(SupportedTypes.encode(aValue, mTyped));
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


	public void indent(int aDelta)
	{
		mIndent += aDelta;
	}


	public void print(Object aText)
	{
		print(aText, true);
	}


	public void print(Object aText, boolean aIndent)
	{
		String text = formatString(aText);

		if (mCompact && text.endsWith(" "))
		{
			text = stripTrailing(text);
			if (text.isEmpty())
			{
				return;
			}
		}

		if (aIndent)
		{
			printIndent();
		}

		mBuffer.append(text);
		mFirst = false;
	}


	public void println(Object aText)
	{
		println(aText, true);
	}


	public void println(Object aText, boolean aIndent)
	{
		String text = formatString(aText);

		if (mCompact && text.endsWith(" "))
		{
			text = stripTrailing(text);
			if (text.isEmpty())
			{
				return;
			}
		}

		if (aIndent)
		{
			printIndent();
		}

		mBuffer.append(text);
		mNewLine = true;
	}


	public void println()
	{
		mNewLine = true;
	}


	public boolean isFirst()
	{
		return mFirst;
	}


	private String formatString(Object aText)
	{
		if (aText == null)
		{
			return "null";
		}

		if (aText instanceof Double || aText instanceof Float)
		{
			String text = aText.toString().replace(" ", "");

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

			return text;
		}

		return aText.toString();
	}


	private void printIndent()
	{
		if (mNewLine && !mCompact)
		{
			mBuffer.append("\n");
			for (int i = 0; i < mIndent; i++)
			{
				mBuffer.append("\t");
			}
			mNewLine = false;
		}
	}


	private String stripTrailing(String aText)
	{
		while (Character.isWhitespace(aText.charAt(aText.length() - 1)))
		{
			aText = aText.substring(0, aText.length() - 1);
		}
		return aText;
	}
}
