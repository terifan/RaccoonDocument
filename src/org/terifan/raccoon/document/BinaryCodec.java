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


enum BinaryCodec
{
	TERMINATOR(0),
	DOCUMENT(1,
		(aEncoder, aPath, aValue) -> aEncoder.writeDocument((Document)aValue, aPath),
		(aDecoder, aPath, aState) -> aDecoder.readDocument(aPath, new Document(), aState)
	),
	ARRAY(2,
		(aEncoder, aPath, aValue) -> aEncoder.writeArray((Array)aValue, aPath),
		(aDecoder, aPath, aState) -> aDecoder.readArray(aPath, new Array(), aState)
	),
	REFERENCE(3,
		(aEncoder, aPath, aValue) -> {System.out.println(aValue);aEncoder.writeVarint((int)aValue);},
		(aDecoder, aPath, aState) -> (int)aDecoder.readVarint()
	),
	/** type: org.terifan.raccoon.document.ObjectId */
	OBJECTID(4,
		(aEncoder, aPath, aValue) -> aEncoder.writeBytes(((ObjectId)aValue).toByteArray()),
		(aDecoder, aPath, aState) -> {return aState.isSkip() ? aDecoder.skipBytes(ObjectId.LENGTH) : ObjectId.fromByteArray(aDecoder.readBytes(new byte[ObjectId.LENGTH]));}
	),
	STRING(5,
		(aEncoder, aPath, aValue) -> aEncoder.writeUnsignedVarint(aValue.toString().length()).writeUTF(aValue.toString()),
		(aDecoder, aPath, aState) -> aDecoder.readUTF((int)aDecoder.readUnsignedVarint())
	),
	INT(6,
		(aEncoder, aPath, aValue) -> aEncoder.writeVarint((Integer)aValue),
		(aDecoder, aPath, aState) -> (int)aDecoder.readVarint()
	),
	DOUBLE(7,
		(aEncoder, aPath, aValue) -> aEncoder.writeLong(Double.doubleToLongBits((Double)aValue)),
		(aDecoder, aPath, aState) -> Double.longBitsToDouble(aDecoder.readLong())
	),
	BOOLEAN(8,
		(aEncoder, aPath, aValue) -> aEncoder.writeVarint((Boolean)aValue ? 1 : 0),
		(aDecoder, aPath, aState) -> aDecoder.readVarint() == 1
	),
	NULL(9,
		(aEncoder, aPath, aValue) -> {},
		(aDecoder, aPath, aState) -> null
	),
	BYTE(10,
		(aEncoder, aPath, aValue) -> aEncoder.writeByte(0xff & (Byte)aValue),
		(aDecoder, aPath, aState) -> (byte)aDecoder.readByte()
	),
	SHORT(11,
		(aEncoder, aPath, aValue) -> aEncoder.writeVarint((Short)aValue),
		(aDecoder, aPath, aState) -> (short)aDecoder.readVarint()
	),
	LONG(12,
		(aEncoder, aPath, aValue) -> aEncoder.writeVarint((Long)aValue),
		(aDecoder, aPath, aState) -> aDecoder.readVarint()
	),
	FLOAT(13,
		(aEncoder, aPath, aValue) -> aEncoder.writeInt(Float.floatToIntBits((Float)aValue)),
		(aDecoder, aPath, aState) -> Float.intBitsToFloat((int)aDecoder.readInt())
	),
	/** type: byte[] */
	BINARY(14,
		(aEncoder, aPath, aValue) -> aEncoder.writeUnsignedVarint(((byte[])aValue).length).writeBytes((byte[])aValue),
		(aDecoder, aPath, aState) -> aDecoder.readBytes(new byte[(int)aDecoder.readUnsignedVarint()])
	),
	/** type: java.util.UUID */
	UUID(15,
		(aEncoder, aPath, aValue) -> aEncoder.writeLong(((UUID)aValue).getMostSignificantBits()).writeLong(((UUID)aValue).getLeastSignificantBits()),
		(aDecoder, aPath, aState) -> new java.util.UUID(aDecoder.readLong(), aDecoder.readLong())
	),
	/** type: java.time.LocalDateTime */
	DATETIME(16,
		(aEncoder, aPath, aValue) -> aEncoder.writeInt(localDateToNumber(((LocalDateTime)aValue).toLocalDate())).writeLong(localTimeToNumber(((LocalDateTime)aValue).toLocalTime())),
		(aDecoder, aPath, aState) -> LocalDateTime.of(numberToLocalDate((int)aDecoder.readInt()), numberToLocalTime(aDecoder.readLong()))
	),
	/** type: java.time.LocalDate */
	DATE(17,
		(aEncoder, aPath, aValue) -> aEncoder.writeInt(localDateToNumber((LocalDate)aValue)),
		(aDecoder, aPath, aState) -> numberToLocalDate(aDecoder.readInt())
	),
	/** type: java.time.LocalTime */
	TIME(18,
		(aEncoder, aPath, aValue) -> aEncoder.writeLong(localTimeToNumber((LocalTime)aValue)),
		(aDecoder, aPath, aState) -> numberToLocalTime(aDecoder.readLong())
	),
	/** type: java.time.OffsetDateTime */
	OFFSETDATETIME(19,
		(aEncoder, aPath, aValue) -> aEncoder.writeInt(localDateToNumber(((OffsetDateTime)aValue).toLocalDate())).writeLong(localTimeToNumber(((OffsetDateTime)aValue).toLocalTime())).writeVarint(((OffsetDateTime)aValue).getOffset().getTotalSeconds()),
		(aDecoder, aPath, aState) -> OffsetDateTime.of(numberToLocalDate((int)aDecoder.readInt()), numberToLocalTime(aDecoder.readLong()), ZoneOffset.ofTotalSeconds((int)aDecoder.readVarint()))
	),
	/** type: java.lang.BigDecimal */
	DECIMAL(20,
		(aEncoder, aPath, aValue) -> writeDecimal(aEncoder, (BigDecimal)aValue),
		(aDecoder, aPath, aState) -> readDecimal(aDecoder)
	),
	CHAR(21,
		(aEncoder, aPath, aValue) -> aEncoder.writeVarint((Character)aValue),
		(aDecoder, aPath, aState) -> (char)aDecoder.readVarint()
	),
	ZERO_INT(22,
		(aEncoder, aPath, aValue) -> {},
		(aDecoder, aPath, aState) -> 0
	),
	ZERO_LONG(23,
		(aEncoder, aPath, aValue) -> {},
		(aDecoder, aPath, aState) -> 0L
	),
	ZERO_DOUBLE(24,
		(aEncoder, aPath, aValue) -> {},
		(aDecoder, aPath, aState) -> 0.0
	),
	ZERO_FLOAT(25,
		(aEncoder, aPath, aValue) -> {},
		(aDecoder, aPath, aState) -> 0f
	),
	ZERO_BYTE(26,
		(aEncoder, aPath, aValue) -> {},
		(aDecoder, aPath, aState) -> (byte)0
	),
	ZERO_SHORT(27,
		(aEncoder, aPath, aValue) -> {},
		(aDecoder, aPath, aState) -> (short)0
	)
//	/** fixed size encoding of a Short value */
//	FIXEDSHORT(22,
//		(aEncoder, aValue) -> aEncoder.writeShort((Short)aValue),
//		(aDecoder, aPath, aState) -> (short)aDecoder.readShort()
//	),
//	/** fixed size encoding of a Integer value */
//	FIXEDINT(23,
//		(aEncoder, aValue) -> aEncoder.writeInt((Integer)aValue),
//		(aDecoder, aPath, aState) -> (int)aDecoder.readInt()
//	),
//	/** fixed size encoding of a char value */
//	FIXEDCHAR(24,
//		(aEncoder, aValue) -> aEncoder.writeShort((short)(char)(Character)aValue),
//		(aDecoder, aPath, aState) -> (char)aDecoder.readShort()
//	),
//	/** fixed size encoding of a Long value */
//	FIXEDLONG(25,
//		(aEncoder, aValue) -> aEncoder.writeLong((Long)aValue),
//		(aDecoder, aPath, aState) -> aDecoder.readLong()
//	),
//	/** variable length encoding of a Float value */
//	VARFLOAT(26,
//		(aEncoder, aValue) -> aEncoder.writeVarint(Float.floatToIntBits((Float)aValue)),
//		(aDecoder, aPath, aState) -> Float.intBitsToFloat((int)aDecoder.readVarint())
//	),
//	/** variable length encoding of a Double value */
//	VARDOUBLE(27,
//		(aEncoder, aValue) -> aEncoder.writeVarint(Long.reverseBytes(Double.doubleToLongBits((Double)aValue))),
//		(aDecoder, aPath, aState) -> Double.longBitsToDouble(Long.reverseBytes(aDecoder.readVarint()))
//	),
//	REF(28,
//		(aEncoder, aValue) -> {},
//		(aDecoder, aPath, aState) -> null
//	),
//	REFVALUE(29,
//		(aEncoder, aValue) -> {},
//		(aDecoder, aPath, aState) -> null
//	)
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
		if (String.class == cls) return STRING;
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
	static interface VariableSize
	{
		boolean test(Object aValue);
	}


	@FunctionalInterface
	static interface Encoder
	{
		void encode(BinaryEncoder aEncoder, Path aPath, Object aValue) throws IOException;
	}


	@FunctionalInterface
	static interface Decoder
	{
		Object decode(BinaryDecoder aDecoder, Path aPath, VisitorResult aState) throws IOException;
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
