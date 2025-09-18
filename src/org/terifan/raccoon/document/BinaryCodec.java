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
		(aEncoder, aState, aValue) -> aEncoder.writeDocument((Document)aValue, aState),
		(aDecoder, aState, aResult) -> aDecoder.readDocument(aState, new Document(), aResult)
	),
	ARRAY(2,
		(aEncoder, aState, aValue) -> aEncoder.writeArray((Array)aValue, aState),
		(aDecoder, aState, aResult) -> aDecoder.readArray(aState, new Array(), aResult)
	),
	STRING(3,
		(aEncoder, aState, aValue) -> aEncoder.writeVarint((int)aValue),
		(aDecoder, aState, aResult) -> (int)aDecoder.readVarint()
	),
	/** type: org.terifan.raccoon.document.ObjectId */
	OBJECTID(4,
		(aEncoder, aState, aValue) -> aEncoder.writeBytes(((ObjectId)aValue).toByteArray()),
		(aDecoder, aState, aResult) -> {return aResult.isSkip() ? aDecoder.skipBytes(ObjectId.LENGTH) : ObjectId.fromByteArray(aDecoder.readBytes(new byte[ObjectId.LENGTH]));}
	),
	LITERAL(5,
		(aEncoder, aState, aValue) -> aEncoder.writeUnsignedVarint(aValue.toString().length()).writeUTF(aValue.toString()),
		(aDecoder, aState, aResult) -> aDecoder.readUTF((int)aDecoder.readUnsignedVarint())
	),
	INT(6,
		(aEncoder, aState, aValue) -> aEncoder.writeVarint((Integer)aValue),
		(aDecoder, aState, aResult) -> (int)aDecoder.readVarint()
	),
	DOUBLE(7,
		(aEncoder, aState, aValue) -> aEncoder.writeLong(Double.doubleToLongBits((Double)aValue)),
		(aDecoder, aState, aResult) -> Double.longBitsToDouble(aDecoder.readLong())
	),
	BOOLEAN_TRUE(8,
		(aEncoder, aState, aValue) -> {},
		(aDecoder, aState, aResult) -> true
	),
	BOOLEAN_FALSE(9,
		(aEncoder, aState, aValue) -> {},
		(aDecoder, aState, aResult) -> false
	),
	NULL(10,
		(aEncoder, aState, aValue) -> {},
		(aDecoder, aState, aResult) -> null
	),
	BYTE(11,
		(aEncoder, aState, aValue) -> aEncoder.writeByte(0xff & (Byte)aValue),
		(aDecoder, aState, aResult) -> (byte)aDecoder.readByte()
	),
	SHORT(12,
		(aEncoder, aState, aValue) -> aEncoder.writeVarint((Short)aValue),
		(aDecoder, aState, aResult) -> (short)aDecoder.readVarint()
	),
	LONG(13,
		(aEncoder, aState, aValue) -> aEncoder.writeVarint((Long)aValue),
		(aDecoder, aState, aResult) -> aDecoder.readVarint()
	),
	FLOAT(14,
		(aEncoder, aState, aValue) -> aEncoder.writeInt(Float.floatToIntBits((Float)aValue)),
		(aDecoder, aState, aResult) -> Float.intBitsToFloat((int)aDecoder.readInt())
	),
	/** type: byte[] */
	BINARY(15,
		(aEncoder, aState, aValue) -> aEncoder.writeUnsignedVarint(((byte[])aValue).length).writeBytes((byte[])aValue),
		(aDecoder, aState, aResult) -> aDecoder.readBytes(new byte[(int)aDecoder.readUnsignedVarint()])
	),
	/** type: java.util.UUID */
	UUID(16,
		(aEncoder, aState, aValue) -> aEncoder.writeLong(((UUID)aValue).getMostSignificantBits()).writeLong(((UUID)aValue).getLeastSignificantBits()),
		(aDecoder, aState, aResult) -> new java.util.UUID(aDecoder.readLong(), aDecoder.readLong())
	),
	/** type: java.time.LocalDateTime */
	DATETIME(17,
		(aEncoder, aState, aValue) -> aEncoder.writeInt(localDateToNumber(((LocalDateTime)aValue).toLocalDate())).writeLong(localTimeToNumber(((LocalDateTime)aValue).toLocalTime())),
		(aDecoder, aState, aResult) -> LocalDateTime.of(numberToLocalDate((int)aDecoder.readInt()), numberToLocalTime(aDecoder.readLong()))
	),
	/** type: java.time.LocalDate */
	DATE(18,
		(aEncoder, aState, aValue) -> aEncoder.writeInt(localDateToNumber((LocalDate)aValue)),
		(aDecoder, aState, aResult) -> numberToLocalDate(aDecoder.readInt())
	),
	/** type: java.time.LocalTime */
	TIME(19,
		(aEncoder, aState, aValue) -> aEncoder.writeLong(localTimeToNumber((LocalTime)aValue)),
		(aDecoder, aState, aResult) -> numberToLocalTime(aDecoder.readLong())
	),
	/** type: java.time.OffsetDateTime */
	OFFSETDATETIME(20,
		(aEncoder, aState, aValue) -> aEncoder.writeInt(localDateToNumber(((OffsetDateTime)aValue).toLocalDate())).writeLong(localTimeToNumber(((OffsetDateTime)aValue).toLocalTime())).writeVarint(((OffsetDateTime)aValue).getOffset().getTotalSeconds()),
		(aDecoder, aState, aResult) -> OffsetDateTime.of(numberToLocalDate((int)aDecoder.readInt()), numberToLocalTime(aDecoder.readLong()), ZoneOffset.ofTotalSeconds((int)aDecoder.readVarint()))
	),
	/** type: java.lang.BigDecimal */
	DECIMAL(21,
		(aEncoder, aState, aValue) -> writeDecimal(aEncoder, (BigDecimal)aValue),
		(aDecoder, aState, aResult) -> readDecimal(aDecoder)
	),
	CHAR(22,
		(aEncoder, aState, aValue) -> aEncoder.writeVarint((Character)aValue),
		(aDecoder, aState, aResult) -> (char)aDecoder.readVarint()
	),
	ZERO_INT(23,
		(aEncoder, aState, aValue) -> {},
		(aDecoder, aState, aResult) -> 0
	),
	ZERO_LONG(24,
		(aEncoder, aState, aValue) -> {},
		(aDecoder, aState, aResult) -> 0L
	),
	ZERO_DOUBLE(25,
		(aEncoder, aState, aValue) -> {},
		(aDecoder, aState, aResult) -> 0.0
	),
	ZERO_FLOAT(26,
		(aEncoder, aState, aValue) -> {},
		(aDecoder, aState, aResult) -> 0f
	),
	ZERO_BYTE(27,
		(aEncoder, aState, aValue) -> {},
		(aDecoder, aState, aResult) -> (byte)0
	),
	ZERO_SHORT(28,
		(aEncoder, aState, aValue) -> {},
		(aDecoder, aState, aResult) -> (short)0
	),
	EMPTY_STRING(29,
		(aEncoder, aState, aValue) -> {},
		(aDecoder, aState, aResult) -> ""
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
		if (String.class == cls && aValue.toString().isEmpty()) return EMPTY_STRING;
		if (String.class == cls) return STRING;
		if (aValue instanceof Integer v) return v == 0 ? ZERO_INT : INT;
		if (aValue instanceof Long v) return v == 0 ? ZERO_LONG : LONG;
		if (aValue instanceof Double v) return v == 0 ? ZERO_DOUBLE : DOUBLE;
		if (aValue instanceof Float v) return v == 0 ? ZERO_FLOAT : FLOAT;
		if (aValue instanceof Byte v) return v == 0 ? ZERO_BYTE : BYTE;
		if (aValue instanceof Short v) return v == 0 ? ZERO_SHORT : SHORT;
		if (aValue == Boolean.TRUE) return BOOLEAN_TRUE;
		if (aValue == Boolean.FALSE) return BOOLEAN_FALSE;
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
		void encode(BinaryEncoder aEncoder, State aState, Object aValue) throws IOException;
	}


	@FunctionalInterface
	static interface Decoder
	{
		Object decode(BinaryDecoder aDecoder, State aState, VisitorResult aResult) throws IOException;
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
