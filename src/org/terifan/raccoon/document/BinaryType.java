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
import java.util.Date;
import java.util.UUID;


enum BinaryType
{
	DOCUMENT(),
	ARRAY(),
	NULL(
		(aEncoder, aValue) -> {},
		aDecoder -> null
	),
	BOOLEAN(
		(aEncoder, aValue) -> aEncoder.write((Boolean)aValue?1:0),
		aDecoder -> aDecoder.read()!=0
	),
	BYTE(
		(aEncoder, aValue) -> aEncoder.write(0xff & (Byte)aValue),
		aDecoder -> (byte)aDecoder.read()
	),
	SHORT(
		(aEncoder, aValue) -> aEncoder.writeVarint((Short)aValue),
		aDecoder -> (short)aDecoder.readVarint()
	),
	CHAR(
		(aEncoder, aValue) -> aEncoder.writeVarint((Character)aValue),
		aDecoder -> (char)aDecoder.readVarint()
	),
	INT(
		(aEncoder, aValue) -> aEncoder.writeVarint((Integer)aValue),
		aDecoder -> (int)aDecoder.readVarint()
	),
	LONG(
		(aEncoder, aValue) -> aEncoder.writeVarlong((Long)aValue),
		aDecoder -> aDecoder.readVarlong()
	),
	FLOAT(
		(aEncoder, aValue) -> aEncoder.writeInt(Float.floatToIntBits((Float)aValue)),
		aDecoder -> Float.intBitsToFloat((int)aDecoder.readInt())
	),
	DOUBLE(
		(aEncoder, aValue) -> aEncoder.writeLong(Double.doubleToLongBits((Double)aValue)),
		aDecoder -> Double.longBitsToDouble(aDecoder.readLong())
	),
	STRING(
		(aEncoder, aValue) -> aEncoder.writeString(aValue.toString()),
		aDecoder -> aDecoder.readString()
	),
	BIGDECIMAL(
		(aEncoder, aValue) -> aEncoder.writeBigDecimal((BigDecimal)aValue),
		aDecoder -> aDecoder.readBigDecimal()
	),
	BIGINTEGER(
		(aEncoder, aValue) -> aEncoder.writeBigInteger((BigInteger)aValue),
		aDecoder -> aDecoder.readBigInteger()
	),
	OBJECTID(
		(aEncoder, aValue) -> aEncoder.write(((ObjectId)aValue).toByteArray()),
		aDecoder -> {byte[] buf = new byte[ObjectId.LENGTH];aDecoder.read(buf);return ObjectId.fromByteArray(buf);}
	),
	BINARY(
		(aEncoder, aValue) -> {aEncoder.writeUnsignedVarint(((byte[])aValue).length); aEncoder.write((byte[])aValue);},
		aDecoder -> {byte[] buf = new byte[aDecoder.readUnsignedVarint()]; aDecoder.read(buf); return buf;}
	),
	UUID(
		(aEncoder, aValue) -> {aEncoder.writeLong(((UUID)aValue).getMostSignificantBits());aEncoder.writeLong(((UUID)aValue).getLeastSignificantBits());},
		aDecoder -> new UUID(aDecoder.readLong(), aDecoder.readLong())
	),
	LOCALDATETIME(
		(aEncoder, aValue) -> {aEncoder.writeInt(localDateToNumber(((LocalDateTime)aValue).toLocalDate()));aEncoder.writeLong(localTimeToNumber(((LocalDateTime)aValue).toLocalTime()));},
		aDecoder -> LocalDateTime.of(numberToLocalDate((int)aDecoder.readInt()), numberToLocalTime(aDecoder.readLong()))
	),
	LOCALDATE(
		(aEncoder, aValue) -> aEncoder.writeInt(localDateToNumber((LocalDate)aValue)),
		aDecoder -> numberToLocalDate(aDecoder.readInt())
	),
	LOCALTIME(
		(aEncoder, aValue) -> aEncoder.writeLong(localTimeToNumber((LocalTime)aValue)),
		aDecoder -> numberToLocalTime(aDecoder.readLong())
	),
	OFFSETDATETIME(
		(aEncoder, aValue) -> {aEncoder.writeInt(localDateToNumber(((OffsetDateTime)aValue).toLocalDate()));aEncoder.writeLong(localTimeToNumber(((OffsetDateTime)aValue).toLocalTime()));aEncoder.writeVarint(((OffsetDateTime)aValue).getOffset().getTotalSeconds());},
		aDecoder -> OffsetDateTime.of(numberToLocalDate((int)aDecoder.readInt()), numberToLocalTime(aDecoder.readLong()), ZoneOffset.ofTotalSeconds((int)aDecoder.readVarint()))
	),
	OFFSETTIME(
		(aEncoder, aValue) -> {aEncoder.writeLong(localTimeToNumber(((OffsetTime)aValue).toLocalTime()));aEncoder.writeVarint(((OffsetTime)aValue).getOffset().getTotalSeconds());},
		aDecoder -> OffsetTime.of(numberToLocalTime(aDecoder.readLong()), ZoneOffset.ofTotalSeconds((int)aDecoder.readVarint()))
	),
	ZONEDDATETIME(
		(aEncoder, aValue) -> {
			aEncoder.writeInt(localDateToNumber(((ZonedDateTime)aValue).toLocalDate()));
			aEncoder.writeLong(localTimeToNumber(((ZonedDateTime)aValue).toLocalTime()));
			aEncoder.writeString(((ZonedDateTime)aValue).getZone().getId());
			aEncoder.writeVarint(((ZonedDateTime)aValue).getOffset().getTotalSeconds());
		},
		aDecoder -> ZonedDateTime.ofLocal(LocalDateTime.of(numberToLocalDate((int)aDecoder.readInt()), numberToLocalTime(aDecoder.readLong())), ZoneId.of(aDecoder.readString()), ZoneOffset.ofTotalSeconds((int)aDecoder.readVarint()))
	),
	DURATION(
		(aEncoder, aValue) -> {aEncoder.writeVarlong(((Duration)aValue).getSeconds()); aEncoder.writeUnsignedVarint(((Duration)aValue).getNano());},
		aDecoder -> Duration.ofSeconds(aDecoder.readVarint(), aDecoder.readUnsignedVarlong())
	),
	DATE(
		(aEncoder, aValue) -> aEncoder.writeLong(((Date)aValue).getTime()),
		aDecoder -> new Date(aDecoder.readLong())
	)
	;

	Encoder encoder;
	Decoder decoder;


	private BinaryType()
	{
		this(null, null);
	}


	private BinaryType(Encoder aEncoder, Decoder aDecoder)
	{
		encoder = aEncoder;
		decoder = aDecoder;
	}


	public static BinaryType identify(Object aValue)
	{
		if (aValue == null)
		{
			return NULL;
		}

		Class<? extends Object> cls = aValue.getClass();

		if (Document.class == cls || Document.class.isAssignableFrom(cls)) return DOCUMENT;
		if (Array.class == cls || Array.class.isAssignableFrom(cls)) return ARRAY;
		if (ObjectId.class == cls) return OBJECTID;
		if (aValue instanceof String) return STRING;
		if (aValue instanceof Integer) return INT;
		if (aValue instanceof Long) return LONG;
		if (aValue instanceof Double) return DOUBLE;
		if (aValue instanceof Float) return FLOAT;
		if (aValue instanceof Byte) return BYTE;
		if (aValue instanceof Short) return SHORT;
		if (aValue instanceof Boolean) return BOOLEAN;
		if (byte[].class == cls) return BINARY;
		if (UUID.class == cls) return UUID;
		if (BigInteger.class == cls) return BIGINTEGER;
		if (BigDecimal.class == cls) return BIGDECIMAL;
		if (Date.class == cls) return DATE;
		if (OffsetDateTime.class == cls) return OFFSETDATETIME;
		if (OffsetTime.class == cls) return OFFSETTIME;
		if (LocalDateTime.class == cls) return LOCALDATETIME;
		if (ZonedDateTime.class == cls) return ZONEDDATETIME;
		if (Duration.class == cls) return DURATION;
		if (LocalDate.class == cls) return LOCALDATE;
		if (LocalTime.class == cls) return LOCALTIME;
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
