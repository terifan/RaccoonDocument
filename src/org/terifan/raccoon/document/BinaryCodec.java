package org.terifan.raccoon.document;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;


public enum BinaryCodec
{
	DOCUMENT(0),
//		(aEncoder, aValue) -> aEncoder.writeDocument((Document)aValue),
//		aDecoder -> aDecoder.readDocument(new Document())
//	),
	ARRAY(1),
//		(aEncoder, aValue) -> aEncoder.writeArray((Array)aValue),
//		aDecoder -> aDecoder.readArray(new Array())
//	),
	REFERENCE(2,
		(aEncoder, aValue) -> aEncoder.writeUnsignedVarint((int)aValue),
		aDecoder -> (int)aDecoder.readVarint()
	),
	STRING(3,
		(aEncoder, aValue) -> aEncoder.writeString(aValue.toString()),
		aDecoder -> aDecoder.readString()
	),
	INT(4,
		(aEncoder, aValue) -> aEncoder.writeVarint((Integer)aValue),
		aDecoder -> (int)aDecoder.readVarint()
	),
	LONG(5,
		(aEncoder, aValue) -> aEncoder.writeVarint((Long)aValue),
		aDecoder -> aDecoder.readVarint()
	),
	DOUBLE(6,
		(aEncoder, aValue) -> aEncoder.writeLong(Double.doubleToLongBits((Double)aValue)),
		aDecoder -> Double.longBitsToDouble(aDecoder.readLong())
	),
	FLOAT(7,
		(aEncoder, aValue) -> aEncoder.writeInt(Float.floatToIntBits((Float)aValue)),
		aDecoder -> Float.intBitsToFloat((int)aDecoder.readInt())
	),
	BOOLEAN(8,
		(aEncoder, aValue) -> aEncoder.writeVarint((Boolean)aValue ? 1 : 0),
		aDecoder -> aDecoder.readVarint() == 1
	),
	NULL(9,
		(aEncoder, aValue) -> {},
		aDecoder -> null
	),
	EMPTY_STRING(10,
		(aEncoder, aValue) -> {},
		aDecoder -> ""
	),
	ZERO_INT(11,
		(aEncoder, aValue) -> {},
		aDecoder -> 0
	),
	ZERO_LONG(12,
		(aEncoder, aValue) -> {},
		aDecoder -> 0L
	),
	ZERO_DOUBLE(13,
		(aEncoder, aValue) -> {},
		aDecoder -> 0.0
	),
	ZERO_FLOAT(14,
		(aEncoder, aValue) -> {},
		aDecoder -> 0f
	),
	ZERO_BYTE(15,
		(aEncoder, aValue) -> {},
		aDecoder -> (byte)0
	),
	ZERO_SHORT(22,
		(aEncoder, aValue) -> {},
		aDecoder -> (short)0
	),
	/** type: org.terifan.raccoon.document.ObjectId */
	OBJECTID(3,
		(aEncoder, aValue) -> aEncoder.write(((ObjectId)aValue).toByteArray()),
		aDecoder -> {byte[] buf = new byte[ObjectId.LENGTH];aDecoder.read(buf);return ObjectId.fromByteArray(buf);}
	),
	BYTE(9,
		(aEncoder, aValue) -> aEncoder.write(0xff & (Byte)aValue),
		aDecoder -> (byte)aDecoder.read()
	),
	SHORT(10,
		(aEncoder, aValue) -> aEncoder.writeVarint((Short)aValue),
		aDecoder -> (short)aDecoder.readVarint()
	),
	/** type: byte[] */
	BINARY(13,
		(aEncoder, aValue) -> {aEncoder.writeUnsignedVarint(((byte[])aValue).length); aEncoder.write((byte[])aValue);},
		aDecoder -> {byte[] buf = new byte[(int)aDecoder.readUnsignedVarint()]; aDecoder.read(buf); return buf;}
	),
	/** type: java.util.UUID */
	UUID(14,
		(aEncoder, aValue) -> {aEncoder.writeLong(((UUID)aValue).getMostSignificantBits());aEncoder.writeLong(((UUID)aValue).getLeastSignificantBits());},
		aDecoder -> new UUID(aDecoder.readLong(), aDecoder.readLong())
	),
	/** type: java.time.LocalDateTime */
	DATETIME(15,
		(aEncoder, aValue) -> {aEncoder.writeInt(localDateToNumber(((LocalDateTime)aValue).toLocalDate()));aEncoder.writeLong(localTimeToNumber(((LocalDateTime)aValue).toLocalTime()));},
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
		(aEncoder, aValue) -> {aEncoder.writeInt(localDateToNumber(((OffsetDateTime)aValue).toLocalDate()));aEncoder.writeLong(localTimeToNumber(((OffsetDateTime)aValue).toLocalTime()));aEncoder.writeVarint(((OffsetDateTime)aValue).getOffset().getTotalSeconds());},
		aDecoder -> OffsetDateTime.of(numberToLocalDate((int)aDecoder.readInt()), numberToLocalTime(aDecoder.readLong()), ZoneOffset.ofTotalSeconds((int)aDecoder.readVarint()))
	),
	/** type: java.time.OffsetTime */
	OFFSETTIME(18,
		(aEncoder, aValue) -> {aEncoder.writeLong(localTimeToNumber(((OffsetTime)aValue).toLocalTime()));aEncoder.writeVarint(((OffsetTime)aValue).getOffset().getTotalSeconds());},
		aDecoder -> OffsetTime.of(numberToLocalTime(aDecoder.readLong()), ZoneOffset.ofTotalSeconds((int)aDecoder.readVarint()))
	),
	/** type: java.time.Duration */
	DURATION(18,
		(aEncoder, aValue) -> {aEncoder.writeVarint(((Duration)aValue).getSeconds()); aEncoder.writeUnsignedVarint(((Duration)aValue).getNano());},
		aDecoder -> Duration.ofSeconds(aDecoder.readVarint(), aDecoder.readUnsignedVarint())
	),
	/** type: java.time.ZonedDateTime */
	ZONEDDATETIME(18,
		(aEncoder, aValue) -> {
			aEncoder.writeInt(localDateToNumber(((ZonedDateTime)aValue).toLocalDate()));
			aEncoder.writeLong(localTimeToNumber(((ZonedDateTime)aValue).toLocalTime()));
			aEncoder.writeString(((ZonedDateTime)aValue).getZone().getId());
			aEncoder.writeVarint(((ZonedDateTime)aValue).getOffset().getTotalSeconds());
		},
		aDecoder -> ZonedDateTime.ofLocal(LocalDateTime.of(numberToLocalDate((int)aDecoder.readInt()), numberToLocalTime(aDecoder.readLong())), ZoneId.of(aDecoder.readString()), ZoneOffset.ofTotalSeconds((int)aDecoder.readVarint()))
	),
	BIGDECIMAL(19,
		(aEncoder, aValue) -> aEncoder.writeBigDecimal((BigDecimal)aValue),
		aDecoder -> aDecoder.readBigDecimal()
	),
	BIGINTEGER(20,
		(aEncoder, aValue) -> aEncoder.writeBigInteger((BigInteger)aValue),
		aDecoder -> aDecoder.readBigInteger()
	),
	CHAR(21,
		(aEncoder, aValue) -> aEncoder.writeVarint((Character)aValue),
		aDecoder -> (char)aDecoder.readVarint()
	),
	STRING_REFERENCE(22,
		(aEncoder, aValue) -> {},
		aDecoder -> null
	),
	;


	Encoder encoder;
	Decoder decoder;


	private BinaryCodec(int aCode)
	{
		this(aCode, null, null);
	}


	private BinaryCodec(int aCode, Encoder aEncoder, Decoder aDecoder)
	{
//		assert aCode == ordinal();

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
		if (BigInteger.class == cls) return BIGINTEGER;
		if (BigDecimal.class == cls) return BIGDECIMAL;
		if (OffsetDateTime.class == cls) return OFFSETDATETIME;
		if (OffsetTime.class == cls) return OFFSETTIME;
		if (LocalDateTime.class == cls) return DATETIME;
		if (ZonedDateTime.class == cls) return ZONEDDATETIME;
		if (Duration.class == cls) return DURATION;
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
