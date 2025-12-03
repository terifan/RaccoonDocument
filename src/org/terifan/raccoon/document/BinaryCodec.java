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
import org.terifan.raccoon.document.BinaryDecoder.VisitorResult;


public enum BinaryCodec
{
	TERMINATOR(0),
	DOCUMENT(1,
		(aEncoder, aValue) -> {throw new IllegalStateException();},
		(aDecoder, aState) -> {throw new IllegalStateException();}
//		(aEncoder, aValue) -> aEncoder.writeDocument((Document)aValue),
//		(aDecoder, aState) -> aDecoder.readDocument(new Document(), aState)
	),
	ARRAY(2,
		(aEncoder, aValue) -> {throw new IllegalStateException();},
		(aDecoder, aState) -> {throw new IllegalStateException();}
//		(aEncoder, aValue) -> aEncoder.writeArray((Array)aValue),
//		(aDecoder, aState) -> aDecoder.readArray(new Array(), aState)
	),
	REFERENCE(3,
		(aEncoder, aValue) -> aEncoder.writeUnsignedVarint((int)aValue),
		(aDecoder, aState) -> (int)aDecoder.readVarint()
	),
	/** type: org.terifan.raccoon.document.ObjectId */
	OBJECTID(4,
		(aEncoder, aValue) -> aEncoder.writeBytes(((ObjectId)aValue).toByteArray()),
		(aDecoder, aState) -> {return aState.isSkip() ? aDecoder.skipBytes(ObjectId.LENGTH) : ObjectId.fromByteArray(aDecoder.readBytes(new byte[ObjectId.LENGTH]));}
	),
	STRING(5,
		(aEncoder, aValue) -> aEncoder.writeUnsignedVarint((int)aValue),
		(aDecoder, aState) -> (int)aDecoder.readUnsignedVarint()
//		(aEncoder, aValue) -> aEncoder.writeUnsignedVarint(aValue.toString().length()).writeUTF(aValue.toString()),
//		(aDecoder, aState) -> aDecoder.readUTF((int)aDecoder.readUnsignedVarint())
	),
	INT(6,
		(aEncoder, aValue) -> aEncoder.writeVarint((Integer)aValue),
		(aDecoder, aState) -> (int)aDecoder.readVarint()
	),
	DOUBLE(7,
		(aEncoder, aValue) -> aEncoder.writeLong(Double.doubleToLongBits((Double)aValue)),
		(aDecoder, aState) -> Double.longBitsToDouble(aDecoder.readLong())
	),
	BOOLEAN(8,
		(aEncoder, aValue) -> aEncoder.writeVarint((Boolean)aValue ? 1 : 0),
		(aDecoder, aState) -> aDecoder.readVarint() == 1
	),
	NULL(9,
		(aEncoder, aValue) -> {},
		(aDecoder, aState) -> null
	),
	BYTE(10,
		(aEncoder, aValue) -> aEncoder.writeByte(0xff & (Byte)aValue),
		(aDecoder, aState) -> (byte)aDecoder.readByte()
	),
	SHORT(11,
		(aEncoder, aValue) -> aEncoder.writeVarint((Short)aValue),
		(aDecoder, aState) -> (short)aDecoder.readVarint()
	),
	LONG(12,
		(aEncoder, aValue) -> aEncoder.writeVarint((Long)aValue),
		(aDecoder, aState) -> aDecoder.readVarint()
	),
	FLOAT(13,
		(aEncoder, aValue) -> aEncoder.writeInt(Float.floatToIntBits((Float)aValue)),
		(aDecoder, aState) -> Float.intBitsToFloat((int)aDecoder.readInt())
	),
	/** type: byte[] */
	BINARY(14,
		(aEncoder, aValue) -> {aEncoder.writeUnsignedVarint(((byte[])aValue).length); aEncoder.writeBytes((byte[])aValue);},
		(aDecoder, aState) -> aDecoder.readBytes(new byte[(int)aDecoder.readUnsignedVarint()])
	),
	/** type: java.util.UUID */
	UUID(15,
		(aEncoder, aValue) -> {aEncoder.writeLong(((UUID)aValue).getMostSignificantBits());aEncoder.writeLong(((UUID)aValue).getLeastSignificantBits());},
		(aDecoder, aState) -> new java.util.UUID(aDecoder.readLong(), aDecoder.readLong())
	),
	/** type: java.time.LocalDateTime */
	DATETIME(16,
		(aEncoder, aValue) -> {aEncoder.writeInt(localDateToNumber(((LocalDateTime)aValue).toLocalDate()));aEncoder.writeLong(localTimeToNumber(((LocalDateTime)aValue).toLocalTime()));},
		(aDecoder, aState) -> LocalDateTime.of(numberToLocalDate((int)aDecoder.readInt()), numberToLocalTime(aDecoder.readLong()))
	),
	/** type: java.time.LocalDate */
	DATE(17,
		(aEncoder, aValue) -> aEncoder.writeInt(localDateToNumber((LocalDate)aValue)),
		(aDecoder, aState) -> numberToLocalDate(aDecoder.readInt())
	),
	/** type: java.time.LocalTime */
	TIME(18,
		(aEncoder, aValue) -> aEncoder.writeLong(localTimeToNumber((LocalTime)aValue)),
		(aDecoder, aState) -> numberToLocalTime(aDecoder.readLong())
	),
	/** type: java.time.OffsetDateTime */
	OFFSETDATETIME(19,
		(aEncoder, aValue) -> {aEncoder.writeInt(localDateToNumber(((OffsetDateTime)aValue).toLocalDate()));aEncoder.writeLong(localTimeToNumber(((OffsetDateTime)aValue).toLocalTime()));aEncoder.writeVarint(((OffsetDateTime)aValue).getOffset().getTotalSeconds());},
		(aDecoder, aState) -> OffsetDateTime.of(numberToLocalDate((int)aDecoder.readInt()), numberToLocalTime(aDecoder.readLong()), ZoneOffset.ofTotalSeconds((int)aDecoder.readVarint()))
	),
	/** type: java.lang.BigDecimal */
	DECIMAL(20,
		(aEncoder, aValue) -> aEncoder.writeDecimal((BigDecimal)aValue),
		(aDecoder, aState) -> aDecoder.readDecimal()
	),
	CHAR(21,
		(aEncoder, aValue) -> aEncoder.writeVarint((Character)aValue),
		(aDecoder, aState) -> (char)aDecoder.readVarint()
	),
	ZERO_INT(22,
		(aEncoder, aValue) -> {},
		(aDecoder, aState) -> 0
	),
	ZERO_LONG(23,
		(aEncoder, aValue) -> {},
		(aDecoder, aState) -> 0L
	),
	ZERO_DOUBLE(24,
		(aEncoder, aValue) -> {},
		(aDecoder, aState) -> 0.0
	),
	ZERO_FLOAT(25,
		(aEncoder, aValue) -> {},
		(aDecoder, aState) -> 0f
	),
	ZERO_BYTE(26,
		(aEncoder, aValue) -> {},
		(aDecoder, aState) -> (byte)0
	),
	ZERO_SHORT(27,
		(aEncoder, aValue) -> {},
		(aDecoder, aState) -> (short)0
	),
	EMPTY_STRING(28,
		(aEncoder, aValue) -> {},
		(aDecoder, aState) -> ""
	)
	;


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
		if (aValue instanceof String s) return s.isEmpty() ? EMPTY_STRING : STRING;
		if (aValue instanceof Integer v) return v == 0 ? ZERO_INT : INT;
		if (aValue instanceof Long v) return v == 0 ? ZERO_LONG : LONG;
		if (aValue instanceof Double v) return v == 0 ? ZERO_DOUBLE : DOUBLE;
		if (aValue instanceof Float v) return v == 0 ? ZERO_FLOAT : FLOAT;
		if (aValue instanceof Byte v) return v == 0 ? ZERO_BYTE : BYTE;
		if (aValue instanceof Short v) return v == 0 ? ZERO_SHORT : SHORT;
		if (Boolean.class == cls || Boolean.TYPE == cls) return BOOLEAN;
		if (byte[].class == cls) return BINARY;
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
	static interface Encoder
	{
		void encode(BinaryOutputStream aEncoder, Object aValue) throws IOException;
	}


	@FunctionalInterface
	static interface Decoder
	{
		Object decode(BinaryInputStream aDecoder, VisitorResult aVisitorResult) throws IOException;
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
