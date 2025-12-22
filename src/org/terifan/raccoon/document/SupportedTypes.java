package org.terifan.raccoon.document;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;


public class SupportedTypes
{
	public static boolean isSupported(Object aValue)
	{
		return isSimpleType(aValue) || isExtendedType(aValue);
	}


	public static void assertSupported(Object aObject)
	{
		if (!isSupported(aObject))
		{
			if (aObject instanceof Enum)
			{
				throw new UnsupportedTypeException("Enums are not supported as they are inherently unsafe for serialization: " + aObject.getClass().getCanonicalName());
			}

			throw new IllegalArgumentException("Unsupported type: " + aObject.getClass().getCanonicalName());
		}
	}


	public static boolean isSimpleType(Object aValue)
	{
		if (aValue == null)
		{
			return true;
		}

		Class<? extends Object> cls = aValue.getClass();

		return String.class == cls
			|| Document.class == cls || Document.class.isAssignableFrom(cls)
			|| Array.class == cls || Array.class.isAssignableFrom(cls)
			|| Integer.class == cls || Integer.TYPE == cls
			|| Boolean.class == cls || Boolean.TYPE == cls
			|| Double.class == cls || Double.TYPE == cls
			|| Long.class == cls || Long.TYPE == cls
			|| Float.class == cls || Float.TYPE == cls
			|| Byte.class == cls || Byte.TYPE == cls
			|| Short.class == cls || Short.TYPE == cls
			|| Character.class == cls || Character.TYPE == cls;
	}


	public static boolean isExtendedType(Object aValue)
	{
		Class<? extends Object> cls = aValue.getClass();

		return ObjectId.class == cls
			|| byte[].class == cls
			|| UUID.class == cls
			|| BigInteger.class ==  cls
			|| BigDecimal.class == cls
			|| LocalDateTime.class == cls
			|| LocalDate.class == cls
			|| LocalTime.class == cls
			|| OffsetDateTime.class == cls
			|| OffsetTime.class == cls
			|| ZonedDateTime.class == cls
			|| Duration.class ==  cls
		;
	}


	public static String encode(Object aValue, boolean aTyped)
	{
		if (!isExtendedType(aValue))
		{
			throw new IllegalArgumentException("Not a supported extended type: " + (aValue == null ? null : aValue.getClass()));
		}

		if (aValue == null)
		{
			return "null";
		}
		if (!aTyped)
		{
			if (aValue instanceof byte[] v)
			{
				return Arrays.toString(v);
			}
			return "\"" + escapeString(aValue.toString()) + "\"";
		}
		if (aValue instanceof byte[] v)
		{
			return "Base64(" + Base64.getEncoder().withoutPadding().encodeToString(v) + ")";
		}
		return aValue.getClass().getSimpleName() + "(" + aValue + ")";
	}


	/**
	 * Decodes an encoded value e.g. "ObjectId(65dc9ad1b09c81b0e278e2c2)" return an instance of ObjectId. Unsupported types return null.
	 */
	public static Object decode(String aText)
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


	static String escapeString(String aString)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0, len = aString.length(); i < len; i++)
		{
			sb.append(escapeChar(aString.charAt(i)));
		}
		return sb.toString();
	}


	static String escapeChar(char c)
	{
		switch (c)
		{
			case '\"':
				return "\\\"";
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
}
