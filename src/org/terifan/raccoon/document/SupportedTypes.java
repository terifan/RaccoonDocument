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
import java.util.Date;
import java.util.UUID;


class SupportedTypes
{
	static boolean isSupported(Object aValue)
	{
		return isSimpleType(aValue) || isExtendedType(aValue);
	}


	static void assertSupported(Object aObject)
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


	static boolean isSimpleType(Object aValue)
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


	static boolean isExtendedType(Object aValue)
	{
		Class<? extends Object> cls = aValue.getClass();

		return ObjectId.class == cls
			|| byte[].class == cls
			|| UUID.class == cls
			|| BigInteger.class ==  cls
			|| BigDecimal.class == cls
			|| Date.class == cls
			|| LocalDateTime.class == cls
			|| LocalDate.class == cls
			|| LocalTime.class == cls
			|| OffsetDateTime.class == cls
			|| OffsetTime.class == cls
			|| ZonedDateTime.class == cls
			|| Duration.class ==  cls
		;
	}


	static String encode(Object aValue, boolean aTyped)
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
			case '\t':
				return "\\t";
			case '\n':
				return "\\n";
			case '\r':
				return "\\r";
			case '\"':
				return "\\\"";
			case '\\':
				return "\\\\";
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
