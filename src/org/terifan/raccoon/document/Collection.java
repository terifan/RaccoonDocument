package org.terifan.raccoon.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;


public abstract class Collection<K, R> implements Externalizable, Serializable
{
	private final static long serialVersionUID = 1L;


	public final Serializer serialize()
	{
		return new Serializer();
	}


	public final Deserializer deserialize()
	{
		return new Deserializer();
	}


	private int indexOf(String aExpression, String... aTokens)
	{
		int i = -1;
		for (String s : aTokens)
		{
			int j = aExpression.indexOf(s);
			if (j != -1 && (i == -1 || j < i))
			{
				i = j;
			}
		}
		return i;
	}


	public class Serializer
	{
		public SerializerJson asJson()
		{
			return new SerializerJson();
		}


		public SerializerBinary asBinary()
		{
			return new SerializerBinary();
		}
	}


	public class SerializerJson
	{
		private boolean mIndent;
		private boolean mTypes;


		public SerializerJson withIndents(boolean b)
		{
			mIndent = b;
			return this;
		}


		public SerializerJson withTypes(boolean b)
		{
			mTypes = b;
			return this;
		}


		public void to(OutputStream aOutputStream) throws IOException
		{
			if (mTypes)
			{
				aOutputStream.write(Collection.this.toTypedJson(!mIndent).getBytes(StandardCharsets.UTF_8));
			}
			else
			{
				aOutputStream.write(Collection.this.toJson(!mIndent).getBytes(StandardCharsets.UTF_8));
			}
		}


		public void to(Appendable aAppendable)
		{
			if (mTypes)
			{
				Collection.this.toTypedJson(aAppendable, !mIndent);
			}
			else
			{
				Collection.this.toJson(aAppendable, !mIndent);
			}
		}


		public byte[] toByteArray()
		{
			return null;
		}


		@Override
		public String toString()
		{
			return "";
		}
	}


	public class SerializerTypedJson
	{
		public void to(OutputStream aOutputStream)
		{
		}
	}


	public class SerializerBinary
	{
		public void to(OutputStream aOutputStream)
		{
		}
	}


	public class Deserializer
	{
		public DeserializerJson asJson()
		{
			return new DeserializerJson();
		}


		public DeserializerBinary asBinary()
		{
			return new DeserializerBinary();
		}
	}


	public class DeserializerBinary
	{
		public void from(InputStream aInputStream)
		{
		}
	}


	public static class DeserializerJson
	{
		public void from(Reader aReader)
		{
		}
	}


	Collection()
	{
	}


	public abstract <T> T get(K aKey);


	public abstract int size();


	public abstract R clear();


	public abstract boolean same(R aOther);


	public abstract Set<K> keySet();


	public abstract boolean containsKey(K aKey);


	abstract Object getImpl(K aKey);


	abstract R putImpl(K aKey, Object aValue);


	/**
	 * Remove an element
	 *
	 * @return the old value
	 */
	abstract Object remove(K aKey);


	abstract MurmurHash3 hashCode(MurmurHash3 aChecksum, ReferenceMap aLinkedList);


	@SuppressWarnings("unchecked")
	public <T> T get(K aKey, T aDefaultValue)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return aDefaultValue;
		}
		return (T)v;
	}


	@SuppressWarnings("unchecked")
	public <T> T get(K aKey, Function<K, T> aDefaultValue)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return aDefaultValue.apply(aKey);
		}
		return (T)v;
	}


	@SuppressWarnings("unchecked")
	public <T> T get(K aKey, Supplier<T> aDefaultValue)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return aDefaultValue.get();
		}
		return (T)v;
	}


	@SuppressWarnings("unchecked")
	public <T> T computeIfAbsent(K aKey, Function<K, T> aSupplier)
	{
		T v = (T)getImpl(aKey);
		if (v == null)
		{
			v = aSupplier.apply(aKey);
			putImpl(aKey, v);
		}
		return v;
	}


	public Boolean getBoolean(K aKey)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return null;
		}
		if (v instanceof Boolean w)
		{
			return w;
		}
		if (v instanceof String w)
		{
			return Boolean.valueOf(w);
		}
		if (v instanceof Number w)
		{
			return w.longValue() != 0;
		}
		throw new IllegalArgumentException("Value of key " + aKey + " cannot be cast on a Boolean");
	}


	public Byte getByte(K aKey)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return null;
		}
		if (v instanceof Number w)
		{
			return w.byteValue();
		}
		throw new IllegalArgumentException("Value of key " + aKey + " cannot be cast on a Byte: " + v.getClass());
	}


	public Short getShort(K aKey)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return null;
		}
		if (v instanceof Number w)
		{
			return w.shortValue();
		}
		throw new IllegalArgumentException("Value of key " + aKey + " cannot be cast on a Short");
	}


	public Integer getInt(K aKey)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return null;
		}
		if (v instanceof Number w)
		{
			return w.intValue();
		}
		throw new IllegalArgumentException("Value of key " + aKey + " cannot be cast on an Integer");
	}


	public Long getLong(K aKey)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return null;
		}
		if (v instanceof Number w)
		{
			return w.longValue();
		}
		throw new IllegalArgumentException("Value of key " + aKey + " cannot be cast on a Long");
	}


	public Float getFloat(K aKey)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return null;
		}
		if (v instanceof Number w)
		{
			return w.floatValue();
		}
		throw new IllegalArgumentException("Value of key " + aKey + " (" + v.getClass().getSimpleName() + ") cannot be cast on a Double");
	}


	public Double getDouble(K aKey)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return null;
		}
		if (v instanceof Number w)
		{
			return w.doubleValue();
		}
		throw new IllegalArgumentException("Value of key " + aKey + " (" + v.getClass().getSimpleName() + ") cannot be cast on a Double");
	}


	public String getString(K aKey)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return null;
		}
		return v.toString();
	}


	public LocalDate getDate(K aKey)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return null;
		}
		if (v instanceof LocalDate w)
		{
			return w;
		}
		if (v instanceof String w)
		{
			return LocalDate.parse(w);
		}
		throw new IllegalArgumentException("Value of key " + aKey + " (" + v.getClass().getSimpleName() + ") cannot be cast on a LocalDate");
	}


	public LocalTime getTime(K aKey)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return null;
		}
		if (v instanceof LocalTime w)
		{
			return w;
		}
		if (v instanceof String w)
		{
			return LocalTime.parse(w);
		}
		throw new IllegalArgumentException("Value of key " + aKey + " (" + v.getClass().getSimpleName() + ") cannot be cast on a LocalTime");
	}


	public LocalDateTime getDateTime(K aKey)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return null;
		}
		if (v instanceof LocalDateTime w)
		{
			return w;
		}
		if (v instanceof String w)
		{
			return LocalDateTime.parse(w);
		}
		throw new IllegalArgumentException("Value of key " + aKey + " (" + v.getClass().getSimpleName() + ") cannot be cast on a LocalDateTime");
	}


	public OffsetDateTime getOffsetDateTime(K aKey)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return null;
		}
		if (v instanceof OffsetDateTime w)
		{
			return w;
		}
		if (v instanceof String w)
		{
			return OffsetDateTime.parse(w);
		}
		throw new IllegalArgumentException("Value of key " + aKey + " (" + v.getClass().getSimpleName() + ") cannot be cast on a OffsetDateTime");
	}


	public UUID getUUID(K aKey)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return null;
		}
		if (v instanceof UUID w)
		{
			return w;
		}
		if (v instanceof String w)
		{
			if (w.length() == 36 && w.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
			{
				return UUID.fromString(w);
			}
		}
		if (v instanceof Array w)
		{
			if (w.size() == 2 && (w.getImpl(0) instanceof Long) && (w.getImpl(1) instanceof Long))
			{
				return new UUID(w.getLong(0), w.getLong(1));
			}
		}
		throw new IllegalArgumentException("Value of key " + aKey + " (" + v.getClass().getSimpleName() + ") cannot be cast on a UUID");
	}


	public ObjectId getObjectId(K aKey)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return null;
		}
		if (v instanceof ObjectId w)
		{
			return w;
		}
		if (v instanceof String w)
		{
			if (w.length() == 24 && w.matches("[0-9a-fA-F]{24}"))
			{
				return ObjectId.fromString(w);
			}
		}
		if (v instanceof Array w)
		{
			if (w.size() == 3)
			{
				return ObjectId.fromParts(w.getInt(0), w.getInt(1), w.getInt(2));
			}
			if (w.size() == ObjectId.LENGTH)
			{
				return ObjectId.fromByteArray(getBinary(aKey));
			}
		}
		throw new IllegalArgumentException("Value of key " + aKey + " (" + v.getClass().getSimpleName() + ") cannot be cast on a ObjectId");
	}


	public Number getNumber(K aKey)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return null;
		}
		if (v instanceof Number w)
		{
			return w;
		}
		throw new IllegalArgumentException("Value of key " + aKey + " (" + v.getClass().getSimpleName() + ") cannot be cast on a Number");
	}


	public BigDecimal getDecimal(K aKey)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return null;
		}
		if (v instanceof BigDecimal w)
		{
			return w;
		}
		if (v instanceof String w)
		{
			return new BigDecimal(w);
		}
		throw new IllegalArgumentException("Value of key " + aKey + " (" + v.getClass().getSimpleName() + ") cannot be cast on a BigDecimal");
	}


	public Array getArray(K aKey)
	{
		return (Array)getImpl(aKey);
	}


	public Document getDocument(K aKey)
	{
		return (Document)getImpl(aKey);
	}


	public Document[] getDocuments(K aKey)
	{
		Object o = getImpl(aKey);
		if (o instanceof Array v)
		{
			return v.asArrayOf(Document.class);
		}
		if (o instanceof Document v)
		{
			return new Document[]
			{
				v
			};
		}
		return null;
	}


	public Array[] getArrays(K aKey)
	{
		return getArray(aKey).asArrayOf(Array.class);
	}


	public byte[] getBinary(K aKey)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return null;
		}
		if (v instanceof byte[] bs)
		{
			return bs;
		}
		if (v instanceof String s)
		{
			if (s.matches("[a-zA-Z0-9\\-\\=\\\\].*"))
			{
				return Base64.getDecoder().decode(s);
			}
			return s.getBytes();
		}
		if (v instanceof Array arr)
		{
			byte[] tmp = new byte[arr.size()];
			for (int i = 0; i < tmp.length; i++)
			{
				tmp[i] = (Byte)arr.get(i);
			}
			return tmp;
		}

		throw new IllegalArgumentException("Unsupported format: " + v.getClass());
	}


	/**
	 * Gets a LocalDateTime object as milliseconds since the epoch of 1970-01-01T00:00:00Z
	 */
	public long getEpochMillis(K aKey)
	{
		Object o = get(aKey);

		if (o instanceof OffsetDateTime v)
		{
			o = v.toLocalDateTime();
		}

		if (o instanceof LocalDateTime v)
		{
			return v.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		}
		if (o instanceof LocalDate v)
		{
			return v.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		}

		throw new IllegalArgumentException("Unsupported value, could not convert to epoc time.");
	}


	/**
	 * Gets a LocalDateTime object as milliseconds since the epoch of 1970-01-01T00:00:00Z wrapped in a {@link java.util.Date} object
	 */
	public Date getEpochTime(K aKey)
	{
		return new Date(getEpochMillis(aKey));
	}


	/**
	 * Puts a LocalDateTime object as Instant with local time zone
	 */
	@SuppressWarnings("unchecked")
	public <T extends Document> T putEpochTime(K aKey, Instant aInstant)
	{
		return (T)putImpl(aKey, LocalDateTime.ofInstant(aInstant, ZoneId.systemDefault()));
	}


	/**
	 * Puts a LocalDateTime object as milliseconds since the epoch of 1970-01-01T00:00:00Z
	 */
	@SuppressWarnings("unchecked")
	public <T extends Document> T putEpochTime(K aKey, long aTimeMillis)
	{
		return (T)putEpochTime(aKey, Instant.ofEpochMilli(aTimeMillis));
	}


	/**
	 * Puts a LocalDateTime object as milliseconds since the epoch of 1970-01-01T00:00:00Z wrapped in a {@link java.util.Date} object
	 */
	@SuppressWarnings("unchecked")
	public <T extends Document> T putEpochTime(K aKey, Date aDateTime)
	{
		return (T)putImpl(aKey, LocalDateTime.ofInstant(aDateTime.toInstant(), ZoneId.systemDefault()));
	}


	public boolean isNull(K aKey)
	{
		return getImpl(aKey) == null;
	}


	public Document collate(String aPath)
	{
		Document result = new Document();
		Visitor visitor = (parent, v) ->
		{
			result.get(v.toString(), k -> new Array()).add(v);
			return true;
		};
		visit(aPath, visitor);
		return result;
	}


	public double sum(String aPath)
	{
		AtomicReference<Number> sum = new AtomicReference<>(0.0);
		Visitor visitor = (parent, v) ->
		{
			if (!(v instanceof Number))
			{
				v = Double.valueOf(v.toString());
			}
			sum.accumulateAndGet((Number)v, (a, b) -> a.doubleValue() + b.doubleValue());
			return true;
		};
		visit(aPath, visitor);
		return sum.get().doubleValue();
	}


	public int count(String aPath)
	{
		AtomicInteger count = new AtomicInteger();
		Visitor visitor = (parent, o) ->
		{
			count.incrementAndGet();
			return true;
		};
		visit(aPath, visitor);
		return count.get();
	}


	public Array findMany(String aPath)
	{
		try (Logger lg = enter("findMany", aPath))
		{
			Array result = new Array();
			Visitor v = (parent, o) ->
			{
				log(o);
				result.add(o);
				return true;
			};
			visit(aPath, v);
			return result;
		}
	}


	@SuppressWarnings("unchecked")
	public <T> T findFirst(String aPath)
	{
		AtomicReference<T> result = new AtomicReference<>();
		Visitor v = (parent, o) ->
		{
			if (!result.compareAndSet(null, (T)o))
			{
				throw new IllegalStateException();
			}
			return false;
		};
		visit(aPath, v);
		return result.get();
	}


	public interface Visitor
	{
		boolean visit(Collection aParent, Object aValue);
	}


	public abstract boolean visit(String aPath, Visitor aVisitor);


	@Override
	public int hashCode()
	{
		return hashCode(new MurmurHash3(0), new ReferenceMap()).getValue();
	}


	void hashCode(MurmurHash3 aChecksum, Object aValue, ReferenceMap aLinkedList)
	{
		if (aValue instanceof Collection v)
		{
			v.hashCode(aChecksum, aLinkedList);
		}
		else if (aValue instanceof CharSequence v)
		{
			aChecksum.updateUTF8(v);
		}
		else if (aValue instanceof byte[] v)
		{
			aChecksum.updateBytes(v);
		}
		else
		{
			aChecksum.updateInt(Objects.hashCode(aValue));
		}
	}


	/**
	 * Return a typed JSON, with indentations and line-breaks, of this instance.
	 */
	@Override
	public String toString()
	{
		return new JSONEncoder(true, true, false, false).marshal(this, new StringBuilder()).toString();
	}


	/**
	 * Decodes a JSON (version 1 &amp; 2) into a Document or Array.
	 */
	public R fromJson(String aJson)
	{
		if (!aJson.startsWith("{"))
		{
			aJson = "{" + aJson + "}";
		}
		return (R)new JSONDecoder(false, false).unmarshal(new StringReader(aJson), this);
	}


	@SuppressWarnings("unchecked")
	public R fromJson(Reader aJson)
	{
		return (R)new JSONDecoder(false, false).unmarshal(aJson, this);
	}


	/**
	 * Decodes a JSON (version 1 &amp; 2) into a Document or Array.
	 */
	public static <T extends Collection> T parseJson(String aJson)
	{
		if (!aJson.startsWith("{"))
		{
			aJson = "{" + aJson + "}";
		}
		return new JSONDecoder(false, false).unmarshal(new StringReader(aJson), null);
	}


	@SuppressWarnings("unchecked")
	public static <T extends Collection> T parseJson(Reader aJson)
	{
		return (T)new JSONDecoder(false, false).unmarshal(aJson, null);
	}


	/**
	 * Encodes this instance into a compact JSON.
	 *
	 * @return a compact JSON representation of this object
	 */
	public String toJson()
	{
		return new JSONEncoder(true, false, false, false).marshal(this, new StringBuilder()).toString();
	}


	/**
	 * Encodes this instance into a JSON while retaining some type information.
	 */
	public String toTypedJson()
	{
		return new JSONEncoder(true, true, false, false).marshal(this, new StringBuilder()).toString();
	}


	/**
	 * Encodes this instance into a JSON.
	 *
	 * @param aCompact a compact JSON will not include line breaks and indentations.
	 * @return a JSON representation of this object
	 */
	public String toJson(boolean aCompact)
	{
		return new JSONEncoder(aCompact, false, false, false).marshal(this, new StringBuilder()).toString();
	}


	public String toJson(boolean aCompact, boolean aApostrophes)
	{
		return new JSONEncoder(aCompact, false, aApostrophes, false).marshal(this, new StringBuilder()).toString();
	}


	/**
	 * Encodes this instance into a JSON while retaining some type information.
	 */
	public String toTypedJson(boolean aCompact)
	{
		return new JSONEncoder(aCompact, true, false, false).marshal(this, new StringBuilder()).toString();
	}


	/**
	 * Encodes this instance into a JSON.
	 *
	 * @param aCompact a compact JSON will not include line breaks and indentations.
	 * @return a JSON representation of this object
	 */
	public Appendable toJson(Appendable aAppendable)
	{
		return new JSONEncoder(true, false, false, false).marshal(this, aAppendable);
	}


	/**
	 * Encodes this instance into a JSON while retaining some type information.
	 */
	public Appendable toTypedJson(Appendable aAppendable)
	{
		return new JSONEncoder(true, true, false, false).marshal(this, aAppendable);
	}


	/**
	 * Encodes this instance into a JSON.
	 *
	 * @param aCompact a compact JSON will not include line breaks and indentations.
	 * @return a JSON representation of this object
	 */
	public Appendable toJson(Appendable aAppendable, boolean aCompact)
	{
		return new JSONEncoder(aCompact, false, false, false).marshal(this, aAppendable);
	}


	/**
	 * Encodes this instance into a JSON while retaining some type information.
	 */
	public Appendable toTypedJson(Appendable aAppendable, boolean aCompact)
	{
		return new JSONEncoder(aCompact, true, false, false).marshal(this, aAppendable);
	}


	/**
	 * Encodes this instance into a YML.
	 *
	 * @return a YML representation of this object
	 */
	public String toYml()
	{
		return new YMLEncoder().marshal(this);
	}


	/**
	 * Decodes a binary encoded Document/Array.
	 */
	@SuppressWarnings("unchecked")
	public R fromByteArray(ByteBuffer aBinaryData)
	{
		try
		{
			BinaryDecoder decoder = new BinaryDecoder(new ByteBufferInputStream(aBinaryData));
			decoder.unmarshal(this);
		}
		catch (IOException e)
		{
			throw new StreamException(e.toString());
		}
		return (R)this;
	}


	/**
	 * Decodes a binary encoded Document/Array.
	 */
	@SuppressWarnings("unchecked")
	public R fromByteArray(byte[] aBinaryData)
	{
		try
		{
			BinaryDecoder decoder = new BinaryDecoder(new ByteArrayInputStream(aBinaryData));
			decoder.unmarshal(this);
		}
		catch (IOException e)
		{
			throw new StreamException(e.toString());
		}
		return (R)this;
	}


	/**
	 * Decodes a binary encoded Document/Array.
	 */
	@SuppressWarnings("unchecked")
	public R fromByteArray(byte[] aBinaryData, int aOffset, int aLength)
	{
		try
		{
			BinaryDecoder decoder = new BinaryDecoder(new ByteArrayInputStream(aBinaryData, aOffset, aLength));
			decoder.unmarshal(this);
		}
		catch (IOException e)
		{
			throw new StreamException(e.toString());
		}
		return (R)this;
	}


	/**
	 * Decodes a binary encoded Document/Array.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Collection> T parseByteArray(ByteBuffer aBinaryData)
	{
		try
		{
			BinaryDecoder decoder = new BinaryDecoder(new ByteBufferInputStream(aBinaryData));
			return (T)decoder.unmarshal();
		}
		catch (IOException e)
		{
			throw new StreamException(e.toString());
		}
	}


	/**
	 * Decodes a JSON (version 1 &amp; 2) into a Document or Array.
	 */
	public static <T extends Collection> T parseByteArray(byte[] aBinaryData)
	{
		return parseByteArray(aBinaryData, 0, aBinaryData.length);
	}


	/**
	 * Decodes a JSON (version 1 &amp; 2) into a Document or Array.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Collection> T parseByteArray(byte[] aBinaryData, int aOffset, int aLength)
	{
		try
		{
			BinaryDecoder decoder = new BinaryDecoder(new ByteArrayInputStream(aBinaryData, aOffset, aLength));
			return (T)decoder.unmarshal();
		}
		catch (IOException e)
		{
			throw new StreamException(e.toString());
		}
	}


	/**
	 * Return a binary representation of this object.
	 */
	public byte[] toByteArray()
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (BinaryEncoder encoder = new BinaryEncoder(baos, k -> true))
		{
			encoder.marshal(this);
		}
		catch (IOException e)
		{
			throw new StreamException(e.toString());
		}
		return baos.toByteArray();
	}


	/**
	 * Return a binary representation of this object.
	 */
	public byte[] toByteArray(Function<Path, Boolean> aFilter)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (BinaryEncoder encoder = new BinaryEncoder(baos, aFilter))
		{
			encoder.marshal(this);
		}
		catch (IOException e)
		{
			throw new StreamException(e.toString());
		}
		return baos.toByteArray();
	}


	/**
	 * Read a binary encoded representation of an object from the stream provided.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Collection> T readFrom(InputStream aInputStream) throws IOException
	{
		try
		{
			BinaryDecoder decoder = new BinaryDecoder(aInputStream);
			return (T)decoder.unmarshal();
		}
		catch (IOException e)
		{
			throw new StreamException(e.toString(), e);
		}
	}


	/**
	 * Write a binary encoded representation of this object to the stream provided.
	 */
	public void writeTo(OutputStream aOutputStream) throws IOException
	{
		try (BinaryEncoder encoder = new BinaryEncoder(aOutputStream, k -> true))
		{
			encoder.marshal(this);
		}
	}


	/**
	 * Read a binary representation of this Object from the ObjectInput provided.
	 */
	@Override
	public void readExternal(ObjectInput aObjectInput) throws IOException, ClassNotFoundException
	{
		InputStream in = new InputStream()
		{
			@Override
			public int read() throws IOException
			{
				return aObjectInput.read();
			}


			@Override
			public int read(byte[] aBuffer, int aOffset, int aLength) throws IOException
			{
				return aObjectInput.read(aBuffer, aOffset, aLength);
			}
		};

		BinaryDecoder decoder = new BinaryDecoder(in);
		decoder.unmarshal(this);
	}


	/**
	 * Write a binary representation of this Object to the ObjectOutput provided.
	 */
	@Override
	public void writeExternal(ObjectOutput aObjectOutput) throws IOException
	{
		OutputStream tmp = new OutputStream()
		{
			@Override
			public void write(int aByte) throws IOException
			{
				aObjectOutput.write(aByte);
			}


			@Override
			public void write(byte[] aBuffer, int aOffset, int aLength) throws IOException
			{
				aObjectOutput.write(aBuffer, aOffset, aLength);
			}
		};

		try (BinaryEncoder encoder = new BinaryEncoder(tmp, k -> true))
		{
			encoder.marshal(this);
		}
	}


	public boolean isEmpty()
	{
		return size() == 0;
	}


	/**
	 * Recursively visits all child elements removing empty Documents/Arrays and null values and replacing equal Documents/Arrays with
	 * shared instances.
	 *
	 * @return this Document
	 */
	@SuppressWarnings("unchecked")
	public R reduce()
	{
		HashMap<Collection, Collection> references = new HashMap<>();
		reduce(references);
		return (R)this;
	}


	private void reduce(HashMap<Collection, Collection> aReferences)
	{
		ArrayList<K> keySet = new ArrayList<>(keySet());

		for (int i = size(); --i >= 0;)
		{
			K key = keySet.get(i);
			Object value = get(key);
			if (value == null)
			{
				remove(key);
			}
			else if (value instanceof Collection v)
			{
				if (aReferences.containsKey(v))
				{
					Collection tmp = aReferences.get(v);
					if (tmp != null)
					{
						putImpl(key, tmp);
					}
				}
				else
				{
					aReferences.put(v, null); // prevent cycles

					v.reduce(aReferences);

					if (v.isEmpty())
					{
						aReferences.remove(v);
						remove(key);
					}
					else
					{
						aReferences.put(v, v);
					}
				}
			}
		}
	}


	private static class ByteBufferInputStream extends InputStream
	{
		private final ByteBuffer mBuffer;


		public ByteBufferInputStream(ByteBuffer aBuffer)
		{
			mBuffer = aBuffer;
		}


		@Override
		public int read() throws IOException
		{
			return 0xff & mBuffer.get();
		}
	}


	/**
	 * Put a value if the condition evaluation return true.
	 *
	 * @param aCondition evaluates aValue and returning true if it is accepted
	 * @return this container
	 */
	@SuppressWarnings("unchecked")
	public <V> R putWithCondition(K aKey, V aValue, Function<V, Boolean> aCondition)
	{
		if (aCondition.apply(aValue))
		{
			putImpl(aKey, aValue);
		}
		return (R)this;
	}


	/**
	 * Put the value produced via the Supplier if the condition evaluation return true.
	 *
	 * @param aCondition evaluates aKey and returning true if it is accepted
	 * @param aSupplier produces the value after evaluation to be added
	 * @return this container
	 */
	@SuppressWarnings("unchecked")
	public R putWhenCondition(K aKey, Function<K, Boolean> aCondition, Function<K, Object> aSupplier)
	{
		if (aCondition.apply(aKey))
		{
			putImpl(aKey, aSupplier.apply(aKey));
		}
		return (R)this;
	}


	/**
	 * Puts the value produced via aSupplier if the key doesn't already exist
	 */
	@SuppressWarnings("unchecked")
	public R putIfAbsent(K aKey, Function<K, Object> aSupplier)
	{
		if (!containsKey(aKey))
		{
			putImpl(aKey, aSupplier.apply(aKey));
		}
		return (R)this;
	}

	private static int mLogIndent;


	static class Logger implements AutoCloseable
	{
		int i = -1;


		@Override
		public void close()
		{
			mLogIndent += i;
			i = 0;
		}
	}


	static Logger enter(String aSection, Object aText)
	{
		System.out.printf("%12s %s %s%n", aSection, "... ".repeat(mLogIndent), aText);
		mLogIndent++;
		return new Logger();
	}


	static void log(Object aText)
	{
		System.out.printf("%12s %s %s%n", "", "... ".repeat(mLogIndent), aText);
	}


	boolean _visit(Object aItem, String aRemain, Visitor aVisitor)
	{
		if (aItem != null)
		{
			if (aItem instanceof Collection c)
			{
				return !c.visit(aRemain, aVisitor);
			}
			else
			{
				return !aVisitor.visit(this, aItem);
			}
		}
		return false;
	}


	String evaluate(String aPath)
	{
		try (Logger lg = enter("eval", aPath+" "+this))
		{
			String path = aPath;

			do
			{
				int offset = path.indexOf('=');
				String key = path.substring(0, offset).trim();
				path = path.substring(offset + 1).trim();

				Object value;
				if (path.startsWith("'"))
				{
					int indexOf = path.indexOf("'", 1);
					value = path.substring(1, indexOf);
					path = path.substring(indexOf + 1);
				}
				else
				{
					int indexOf = indexOf(path, "]", " ", "&", "|", "(", ")");
					value = path.substring(0, indexOf);
					path = path.substring(indexOf);
					if ("true".equals(value))
					{
						value = true;
					}
					else if ("false".equals(value))
					{
						value = false;
					}
					else if (value.toString().matches("[0-9]*"))
					{
						value = Long.valueOf(value.toString());
					}
					else if (value.toString().matches("[+\\\\-][0-9].[0-9]*"))
					{
						value = Double.valueOf(value.toString());
					}
				}
				path = path.trim();

				if (!findMatch(key, value))
				{
					return null;
				}

				if (path.startsWith("&&"))
				{
					path = path.trim().substring(2).trim();
				}
			}
			while (!path.startsWith("]"));
			path = path.substring(1);

			return path;
		}
	}


	boolean evaluatePathExpression(String aPath, Visitor aResult)
	{
		try (Logger lg = enter("eval", aPath+" "+this))
		{
			String path = aPath;

			do
			{
				int offset = path.indexOf('=');
				String key = path.substring(1, offset).trim();
				path = path.substring(offset + 1).trim();

				Object value;
				if (path.startsWith("'"))
				{
					int indexOf = path.indexOf("'", 1);
					value = path.substring(1, indexOf);
					path = path.substring(indexOf + 1);
				}
				else
				{
					int indexOf = indexOf(path, "]", " ", "&", "|", "(", ")");
					value = path.substring(0, indexOf);
					path = path.substring(indexOf);
					if ("true".equals(value))
					{
						value = true;
					}
					else if ("false".equals(value))
					{
						value = false;
					}
					else if (value.toString().matches("[0-9]*"))
					{
						value = Long.valueOf(value.toString());
					}
					else if (value.toString().matches("[+\\\\-][0-9].[0-9]*"))
					{
						value = Double.valueOf(value.toString());
					}
				}
				path = path.trim();

				if (!findMatch(key, value))
				{
					return false;
				}

//				log("eval", "success");
				if (path.startsWith("&&"))
				{
					path = path.trim();
				}
			}
			while (!path.startsWith("]"));
			path = path.substring(1);

			return visit(path, aResult);
		}
	}


	private boolean findMatch(String aKey, Object aExpression)
	{
		try (Logger lg = enter("findMatch", aKey + " == " + aExpression))
		{
			int _i = 0;
			for (Object o : findMany(aKey))
			{
				log("item "+(_i++)+": "+o);
				if (o == null)
				{
					return aExpression == null;
				}
				if (o instanceof Boolean v)
				{
					return aExpression.equals(v);
				}
				return aExpression.toString().equalsIgnoreCase(o.toString());
			}
			return false;
		}
	}
}
