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


public enum BinaryCodec
{
	TERMINATOR(0),
	DOCUMENT(1),
//		(aEncoder, aValue) -> aEncoder.writeDocument((Document)aValue),
//		aDecoder -> aDecoder.readDocument(new Document())
//	),
	ARRAY(2),
//		(aEncoder, aValue) -> aEncoder.writeArray((Array)aValue),
//		aDecoder -> aDecoder.readArray(new Array())
//	),
	REFERENCE(3),
//		(aEncoder, aValue) -> aEncoder.writeUnsignedVarint((int)aValue),
//		aDecoder -> (int)aDecoder.readVarint()
//	),
	/** type: org.terifan.raccoon.document.ObjectId */
	OBJECTID(4,
		(aEncoder, aValue) -> aEncoder.writeBytes(((ObjectId)aValue).toByteArray()),
		aDecoder -> ObjectId.fromByteArray(aDecoder.readBytes(new byte[ObjectId.LENGTH]))
	),
	STRING(5,
//		(aEncoder, aValue) -> aEncoder.writeUnsignedVarint((int)aValue),
//		aDecoder -> (int)aDecoder.readUnsignedVarint()
		(aEncoder, aValue) -> aEncoder.writeString(aValue.toString()),
		aDecoder -> aDecoder.readString()
	),
	INT(6,
		(aEncoder, aValue) -> aEncoder.writeVarint((Integer)aValue),
		aDecoder -> (int)aDecoder.readVarint()
	),
	DOUBLE(7,
		(aEncoder, aValue) -> aEncoder.writeLong(Double.doubleToLongBits((Double)aValue)),
		aDecoder -> Double.longBitsToDouble(aDecoder.readLong())
	),
	BOOLEAN(8,
		(aEncoder, aValue) -> aEncoder.writeVarint((Boolean)aValue ? 1 : 0),
		aDecoder -> aDecoder.readVarint() == 1
	),
	NULL(9,
		(aEncoder, aValue) -> {},
		aDecoder -> null
	),
	BYTE(10,
		(aEncoder, aValue) -> aEncoder.writeByte(0xff & (Byte)aValue),
		aDecoder -> (byte)aDecoder.readByte()
	),
	SHORT(11,
		(aEncoder, aValue) -> aEncoder.writeVarint((Short)aValue),
		aDecoder -> (short)aDecoder.readVarint()
	),
	LONG(12,
		(aEncoder, aValue) -> aEncoder.writeVarint((Long)aValue),
		aDecoder -> aDecoder.readVarint()
	),
	FLOAT(13,
		(aEncoder, aValue) -> aEncoder.writeInt(Float.floatToIntBits((Float)aValue)),
		aDecoder -> Float.intBitsToFloat((int)aDecoder.readInt())
	),
	/** type: byte[] */
	BINARY(14,
		(aEncoder, aValue) -> {aEncoder.writeUnsignedVarint(((byte[])aValue).length); aEncoder.writeBytes((byte[])aValue);},
		aDecoder -> aDecoder.readBytes(new byte[(int)aDecoder.readUnsignedVarint()])
	),
	/** type: java.util.UUID */
	UUID(15,
		(aEncoder, aValue) -> {aEncoder.writeLong(((UUID)aValue).getMostSignificantBits());aEncoder.writeLong(((UUID)aValue).getLeastSignificantBits());},
		aDecoder -> new UUID(aDecoder.readLong(), aDecoder.readLong())
	),
	/** type: java.time.LocalDateTime */
	DATETIME(16,
		(aEncoder, aValue) -> {aEncoder.writeInt(localDateToNumber(((LocalDateTime)aValue).toLocalDate()));aEncoder.writeLong(localTimeToNumber(((LocalDateTime)aValue).toLocalTime()));},
		aDecoder -> LocalDateTime.of(numberToLocalDate((int)aDecoder.readInt()), numberToLocalTime(aDecoder.readLong()))
	),
	/** type: java.time.LocalDate */
	DATE(17,
		(aEncoder, aValue) -> aEncoder.writeInt(localDateToNumber((LocalDate)aValue)),
		aDecoder -> numberToLocalDate(aDecoder.readInt())
	),
	/** type: java.time.LocalTime */
	TIME(18,
		(aEncoder, aValue) -> aEncoder.writeLong(localTimeToNumber((LocalTime)aValue)),
		aDecoder -> numberToLocalTime(aDecoder.readLong())
	),
	/** type: java.time.OffsetDateTime */
	OFFSETDATETIME(19,
		(aEncoder, aValue) -> {aEncoder.writeInt(localDateToNumber(((OffsetDateTime)aValue).toLocalDate()));aEncoder.writeLong(localTimeToNumber(((OffsetDateTime)aValue).toLocalTime()));aEncoder.writeVarint(((OffsetDateTime)aValue).getOffset().getTotalSeconds());},
		aDecoder -> OffsetDateTime.of(numberToLocalDate((int)aDecoder.readInt()), numberToLocalTime(aDecoder.readLong()), ZoneOffset.ofTotalSeconds((int)aDecoder.readVarint()))
	),
	/** type: java.lang.BigDecimal */
	DECIMAL(20,
		(aEncoder, aValue) -> aEncoder.writeDecimal((BigDecimal)aValue),
		aDecoder -> aDecoder.readDecimal()
	),
	CHAR(21,
		(aEncoder, aValue) -> aEncoder.writeVarint((Character)aValue),
		aDecoder -> (char)aDecoder.readVarint()
	),
	ZERO_BYTE(22,
		(aEncoder, aValue) -> {},
		aDecoder -> (byte)0
	),
	ZERO_SHORT(23,
		(aEncoder, aValue) -> {},
		aDecoder -> (short)0
	),
	ZERO_INT(24,
		(aEncoder, aValue) -> {},
		aDecoder -> 0
	),
	ZERO_LONG(25,
		(aEncoder, aValue) -> {},
		aDecoder -> 0L
	),
	ZERO_FLOAT(26,
		(aEncoder, aValue) -> {},
		aDecoder -> 0f
	),
	ZERO_DOUBLE(27,
		(aEncoder, aValue) -> {},
		aDecoder -> 0.0
	),
	EMPTY_STRING(28,
		(aEncoder, aValue) -> {},
		aDecoder -> ""
	),
	COMPACT_STRING(29,
		(aEncoder, aValue) -> aEncoder.writeCompactString(aValue.toString()),
		aDecoder -> aDecoder.readCompactString()
	),
	;


	/**
	 * @return true if all characters in the String provided are 7-bit
	 */
	static boolean isCompactString(String aString)
	{
		for (char c : aString.toCharArray())
		{
			if (c >= 128) return false;
		}
		return true;
	}


	Encoder encoder;
	Decoder decoder;


	private BinaryCodec(int aCode)
	{
		this(aCode, null, null);
	}


	private BinaryCodec(int aCode, Encoder aEncoder, Decoder aDecoder)
	{
		assert aCode == ordinal();

		encoder = aEncoder;
		decoder = aDecoder;
	}


	public static BinaryCodec identify(Object aValue)
	{
		if (aValue == null)
		{
			return NULL;
		}

		Class<? extends Object> cls = aValue.getClass();

		if (Document.class == cls || Document.class.isAssignableFrom(cls)) return DOCUMENT;
		if (Array.class == cls || Array.class.isAssignableFrom(cls)) return ARRAY;
		if (ObjectId.class == cls) return OBJECTID;
//		if (aValue instanceof String v) return v.isEmpty() ? EMPTY_STRING : isCompactString(v) ? COMPACT_STRING : STRING;
		if (aValue instanceof String v) return v.isEmpty() ? EMPTY_STRING : STRING;
		if (aValue instanceof Integer v) return v == 0 ? ZERO_INT : INT;
		if (aValue instanceof Long v) return v == 0 ? ZERO_LONG : LONG;
		if (aValue instanceof Double v) return v == 0 ? ZERO_DOUBLE : DOUBLE;
		if (aValue instanceof Float v) return v == 0 ? ZERO_FLOAT : FLOAT;
		if (aValue instanceof Byte v) return v == 0 ? ZERO_BYTE : BYTE;
		if (aValue instanceof Short v) return v == 0 ? ZERO_SHORT : SHORT;
		if (Boolean.class == cls || Boolean.TYPE == cls) return BOOLEAN;
		if (byte[].class == cls) return BINARY;
		if (UUID.class == cls) return UUID;
		if (BigDecimal.class == cls) return DECIMAL;
		if (OffsetDateTime.class == cls) return OFFSETDATETIME;
		if (LocalDateTime.class == cls) return DATETIME;
		if (LocalDate.class == cls) return DATE;
		if (LocalTime.class == cls) return TIME;
		if (Character.class == cls || Character.TYPE == cls) return CHAR;

		return null;
	}


	@FunctionalInterface
	static interface Encoder
	{
		void encode(BinaryOutputStream aEncoder, Object aValue) throws IOException;
	}


	@FunctionalInterface
	static interface Decoder
	{
		Object decode(BinaryInputStream aDecoder) throws IOException;
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
