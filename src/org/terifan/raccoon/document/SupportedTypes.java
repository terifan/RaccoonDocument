package org.terifan.raccoon.document;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;


public enum SupportedTypes
{
	TERMINATOR(0),
	DOCUMENT(1,
		(aEncoder, aValue) -> aEncoder.writeDocument((Document)aValue),
		aDecoder -> aDecoder.readDocument(new Document())
	),
	ARRAY(2,
		(aEncoder, aValue) -> aEncoder.writeArray((Array)aValue),
		aDecoder -> aDecoder.readArray(new Array())
	),
	/** type: org.terifan.raccoon.document.ObjectId */
	OBJECTID(3,
		(aEncoder, aValue) -> aEncoder.writeBytes(((ObjectId)aValue).toByteArray()),
		aDecoder -> ObjectId.fromBytes(aDecoder.readBytes(new byte[ObjectId.LENGTH]))
	),
	INT(4,
		(aEncoder, aValue) -> aEncoder.writeVarint((Integer)aValue),
		aDecoder -> (int)aDecoder.readVarint()
	),
	DOUBLE(5,
		(aEncoder, aValue) -> aEncoder.writeLong(Double.doubleToLongBits((Double)aValue)),
		aDecoder -> Double.longBitsToDouble(aDecoder.readLong())
	),
	BOOLEAN(6,
		(aEncoder, aValue) -> aEncoder.writeVarint((Boolean)aValue ? 1 : 0),
		aDecoder -> aDecoder.readVarint() == 1
	),
	STRING(7,
		(aEncoder, aValue) -> aEncoder.writeUnsignedVarint(aValue.toString().length()).writeUTF(aValue.toString()),
		aDecoder -> aDecoder.readUTF((int)aDecoder.readUnsignedVarint())
	),
	NULL(8,
		(aEncoder, aValue) -> {},
		aDecoder -> null
	),
	BYTE(9,
		(aEncoder, aValue) -> aEncoder.writeByte(0xff & (Byte)aValue),
		aDecoder -> (byte)aDecoder.readByte()
	),
	SHORT(10,
		(aEncoder, aValue) -> aEncoder.writeVarint((Short)aValue),
		aDecoder -> (short)aDecoder.readVarint()
	),
	LONG(11,
		(aEncoder, aValue) -> aEncoder.writeVarint((Long)aValue),
		aDecoder -> aDecoder.readVarint()
	),
	FLOAT(12,
		(aEncoder, aValue) -> aEncoder.writeInt(Float.floatToIntBits((Float)aValue)),
		aDecoder -> Float.intBitsToFloat((int)aDecoder.readInt())
	),
	/** type: byte[] */
	BINARY(13,
		(aEncoder, aValue) -> aEncoder.writeUnsignedVarint(((byte[])aValue).length).writeBytes((byte[])aValue),
		aDecoder -> aDecoder.readBytes(new byte[(int)aDecoder.readUnsignedVarint()])
	),
	/** type: java.util.UUID */
	UUID(14,
		(aEncoder, aValue) -> aEncoder.writeLong(((UUID)aValue).getMostSignificantBits()).writeLong(((UUID)aValue).getLeastSignificantBits()),
		aDecoder -> new java.util.UUID(aDecoder.readLong(), aDecoder.readLong())
	),
	/** type: java.time.LocalDateTime */
	DATETIME(15,
		(aEncoder, aValue) -> aEncoder.writeInt(localDateToNumber(((LocalDateTime)aValue).toLocalDate())).writeLong(localTimeToNumber(((LocalDateTime)aValue).toLocalTime())),
		aDecoder -> LocalDateTime.of(numberToLocalDate((int)aDecoder.readInt()), numberToLocalTime(aDecoder.readLong()))
	),
	/** type: java.time.LocalDate */
	DATE(16,
		(aEncoder, aValue) -> aEncoder.writeInt(localDateToNumber((LocalDate)aValue)),
		aDecoder -> numberToLocalDate(aDecoder.readInt())
	),
	/** type: java.time.LocalTime */
	TIME(17,
		(aEncoder, aValue) -> aEncoder.writeLong(localTimeToNumber((LocalTime)aValue)),
		aDecoder -> numberToLocalTime(aDecoder.readLong())
	),
	/** type: java.time.OffsetDateTime */
	OFFSETDATETIME(18,
		(aEncoder, aValue) -> aEncoder.writeInt(localDateToNumber(((OffsetDateTime)aValue).toLocalDate())).writeLong(localTimeToNumber(((OffsetDateTime)aValue).toLocalTime())).writeVarint(((OffsetDateTime)aValue).getOffset().getTotalSeconds()),
		aDecoder -> OffsetDateTime.of(numberToLocalDate((int)aDecoder.readInt()), numberToLocalTime(aDecoder.readLong()), ZoneOffset.ofTotalSeconds((int)aDecoder.readVarint()))
	),
	/** type: java.lang.BigDecimal */
	DECIMAL(19,
		(aEncoder, aValue) -> writeDecimal(aEncoder, (BigDecimal)aValue),
		aDecoder -> readDecimal(aDecoder)
	),
	CHAR(20,
		(aEncoder, aValue) -> aEncoder.writeVarint((Character)aValue),
		aDecoder -> (char)aDecoder.readVarint()
	),
	/** fixed size encoding of a Short value */
	FIXEDSHORT(21,
		(aEncoder, aValue) -> aEncoder.writeShort((Short)aValue),
		aDecoder -> (short)aDecoder.readShort()
	),
	/** fixed size encoding of a Integer value */
	FIXEDINT(22,
		(aEncoder, aValue) -> aEncoder.writeInt((Integer)aValue),
		aDecoder -> (int)aDecoder.readInt()
	),
	/** fixed size encoding of a char value */
	FIXEDCHAR(23,
		(aEncoder, aValue) -> aEncoder.writeShort((short)(char)(Character)aValue),
		aDecoder -> (char)aDecoder.readShort()
	),
	/** fixed size encoding of a Long value */
	FIXEDLONG(24,
		(aEncoder, aValue) -> aEncoder.writeLong((Long)aValue),
		aDecoder -> aDecoder.readLong()
	),
	/** variable length encoding of a Float value */
	VARFLOAT(25,
		(aEncoder, aValue) -> aEncoder.writeVarint(Float.floatToIntBits((Float)aValue)),
		aDecoder -> Float.intBitsToFloat((int)aDecoder.readVarint())
	),
	/** variable length encoding of a Double value */
	VARDOUBLE(26,
		(aEncoder, aValue) -> aEncoder.writeVarint(Long.reverseBytes(Double.doubleToLongBits((Double)aValue))),
		aDecoder -> Double.longBitsToDouble(Long.reverseBytes(aDecoder.readVarint()))
	),
	REF(27,
		(aEncoder, aValue) -> {},
		aDecoder -> null
	),
	REFVALUE(28,
		(aEncoder, aValue) -> {},
		aDecoder -> null
	);


	Encoder encoder;
	Decoder decoder;


	private SupportedTypes(int aCode)
	{
		this(aCode, null, null);
	}


	private SupportedTypes(int aCode, Encoder aEncoder, Decoder aDecoder)
	{
		assert aCode == ordinal();

		encoder = aEncoder;
		decoder = aDecoder;
	}


	public static SupportedTypes identify(Object aValue)
	{
		if (aValue == null)
		{
			return NULL;
		}

		Class<? extends Object> cls = aValue.getClass();

		if (Document.class == cls || Document.class.isAssignableFrom(cls)) return DOCUMENT;
		if (Array.class == cls || Array.class.isAssignableFrom(cls)) return ARRAY;
		if (ObjectId.class == cls) return OBJECTID;
		if (String.class == cls) return STRING;
		if (byte[].class == cls) return BINARY;
		if (Integer.class == cls || Integer.TYPE == cls) return INT;
		if (Boolean.class == cls || Boolean.TYPE == cls) return BOOLEAN;
		if (Double.class == cls || Double.TYPE == cls) return DOUBLE;
		if (Long.class == cls || Long.TYPE == cls) return LONG;
		if (Float.class == cls || Float.TYPE == cls) return FLOAT;
		if (Byte.class == cls || Byte.TYPE == cls) return BYTE;
		if (Short.class == cls || Short.TYPE == cls) return SHORT;
		if (Character.class == cls || Character.TYPE == cls) return CHAR;
		if (UUID.class == cls) return UUID;
		if (BigDecimal.class == cls) return DECIMAL;
		if (OffsetDateTime.class == cls) return OFFSETDATETIME;
		if (LocalDateTime.class == cls) return DATETIME;
		if (LocalDate.class == cls) return DATE;
		if (LocalTime.class == cls) return TIME;

		return null;
	}


	@FunctionalInterface
	static interface VariableSize
	{
		boolean test(Object aValue);
	}


	@FunctionalInterface
	static interface Encoder
	{
		void encode(BinaryEncoder aEncoder, Object aValue) throws IOException;
	}


	@FunctionalInterface
	static interface Decoder
	{
		Object decode(BinaryDecoder aDecoder) throws IOException;
	}


	/* Encodes two digits/symbols into a single byte:
	 *
	 *  +   43
	 *  ,   44
	 *  -   45
	 *  .   46
	 *  0-9 48-57
	 *  e   58
	*/
	private static BinaryEncoder writeDecimal(BinaryEncoder aEncoder, BigDecimal aValue) throws IOException
	{
		char[] s = aValue.toString().toCharArray();
		aEncoder.writeUnsignedVarint(s.length);
		for (int k = 0; k < s.length - 1;)
		{
			int a = s[k++];
			int b = s[k++];
			a = (a == 'e' || a == 'E' ? ':' : a) - '+';
			b = (b == 'e' || b == 'E' ? ':' : b) - '+';
			aEncoder.writeByte((a << 4) + b);
		}
		if ((s.length & 1) == 1)
		{
			int a = s[s.length - 1];
			a = (a == 'e' || a == 'E' ? ':' : a) - '+';
			aEncoder.writeByte(a << 4);
		}
		return aEncoder;
	}


	private static BigDecimal readDecimal(BinaryDecoder aDecoder) throws IOException
	{
		char[] s = new char[(int)aDecoder.readUnsignedVarint()];
		for (int i = 0; i < s.length;)
		{
			int v = aDecoder.readByte();
			int a = '+' + (v >>> 4);
			if (a == ':') a = 'E';
			s[i++] = (char)a;
			if (i < s.length)
			{
				int b = '+' + (15 & v);
				if (b == ':') b = 'E';
				s[i++] = (char)b;
			}
		}
		return new BigDecimal(s);
	}


	private static int localDateToNumber(LocalDate aLocalDate)
	{
		return (aLocalDate.getYear() << 16) + (aLocalDate.getMonthValue() << 8) + aLocalDate.getDayOfMonth();
	}


	private static long localTimeToNumber(LocalTime aLocalTime)
	{
		return ((long)aLocalTime.getHour() << 48) + ((long)aLocalTime.getMinute() << 40) + ((long)aLocalTime.getSecond() << 32) + aLocalTime.getNano();
	}


	private static LocalDate numberToLocalDate(int aLocalDate)
	{
		try
		{
			return LocalDate.of(aLocalDate >>> 16, 0xff & (aLocalDate >>> 8), 0xff & aLocalDate);
		}
		catch (DateTimeException e)
		{
			throw new StreamException(e.getMessage());
		}
	}


	private static LocalTime numberToLocalTime(long aLocalTime)
	{
		try
		{
			return LocalTime.of((int)(aLocalTime >>> 48), (int)(0xff & (aLocalTime >>> 40)), (int)(0xff & (aLocalTime >> 32)), (int)(0xffffffffL & aLocalTime));
		}
		catch (DateTimeException e)
		{
			throw new StreamException(e.getMessage());
		}
	}
}
