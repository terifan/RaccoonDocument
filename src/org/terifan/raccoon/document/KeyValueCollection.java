package org.terifan.raccoon.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;


abstract class KeyValueCollection<K, R> implements Externalizable, Serializable
{
	private final static long serialVersionUID = 1L;


	KeyValueCollection()
	{
	}


	public abstract <T> T get(K aKey);


	public <T> T get(K aKey, T aDefaultValue)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return aDefaultValue;
		}
		return (T)v;
	}


	public <T> T get(K aKey, Function<K, T> aDefaultValue)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return aDefaultValue.apply(aKey);
		}
		return (T)v;
	}


	public <T> T get(K aKey, Supplier<T> aDefaultValue)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return aDefaultValue.get();
		}
		return (T)v;
	}


	public Boolean getBoolean(K aKey)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return null;
		}
		if (v instanceof Boolean)
		{
			return (Boolean)v;
		}
		if (v instanceof String)
		{
			return Boolean.valueOf((String)v);
		}
		if (v instanceof Number)
		{
			return ((Number)v).longValue() != 0;
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
		if (v instanceof Number)
		{
			return ((Number)v).byteValue();
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
		if (v instanceof Number)
		{
			return ((Number)v).shortValue();
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
		if (v instanceof Number)
		{
			return ((Number)v).intValue();
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
		if (v instanceof Number)
		{
			return ((Number)v).longValue();
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
		if (v instanceof Number)
		{
			return ((Number)v).floatValue();
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
		if (v instanceof Number)
		{
			return ((Number)v).doubleValue();
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
		if (v instanceof LocalDate)
		{
			return (LocalDate)v;
		}
		if (v instanceof String)
		{
			return LocalDate.parse((String)v);
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
		if (v instanceof LocalTime)
		{
			return (LocalTime)v;
		}
		if (v instanceof String)
		{
			return LocalTime.parse((String)v);
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
		if (v instanceof LocalDateTime)
		{
			return (LocalDateTime)v;
		}
		if (v instanceof String)
		{
			return LocalDateTime.parse((String)v);
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
		if (v instanceof OffsetDateTime)
		{
			return (OffsetDateTime)v;
		}
		if (v instanceof String)
		{
			return OffsetDateTime.parse((String)v);
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
		if (v instanceof UUID)
		{
			return (UUID)v;
		}
		if (v instanceof String)
		{
			String s = (String)v;
			if (s.length() == 36 && s.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
			{
				return UUID.fromString(s);
			}
		}
		if (v instanceof Array)
		{
			Array a = (Array)v;
			if (a.size() == 2 && (a.getImpl(0) instanceof Long) && (a.getImpl(1) instanceof Long))
			{
				return new UUID(a.getLong(0), a.getLong(1));
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
		if (v instanceof ObjectId)
		{
			return (ObjectId)v;
		}
		if (v instanceof String)
		{
			String s = (String)v;
			if (s.length() == 24 && s.matches("[0-9a-fA-F]{24}"))
			{
				return ObjectId.fromString(s);
			}
		}
		if (v instanceof Array)
		{
			Array a = (Array)v;
			if (a.size() == 3)
			{
				return ObjectId.fromParts(a.getInt(0), a.getInt(1), a.getInt(2));
			}
			if (a.size() == ObjectId.LENGTH)
			{
				return ObjectId.fromBytes(getBinary(aKey));
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
		if (v instanceof Number)
		{
			return (Number)v;
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
		if (v instanceof BigDecimal)
		{
			return (BigDecimal)v;
		}
		if (v instanceof String)
		{
			return new BigDecimal((String)v);
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


	public Array getOrCreateArray(K aKey)
	{
		Array v = (Array)getImpl(aKey);
		if (v == null)
		{
			v = new Array();
			putImpl(aKey, v);
		}
		return v;
	}


	public Document getOrCreateDocument(K aKey)
	{
		Document v = (Document)getImpl(aKey);
		if (v == null)
		{
			v = new Document();
			putImpl(aKey, v);
		}
		return v;
	}


	public byte[] getBinary(K aKey)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return null;
		}
		if (v instanceof byte[])
		{
			return (byte[])v;
		}
		if (v instanceof String)
		{
			String s = (String)v;

			if (s.matches("[a-zA-Z0-9\\-\\=\\\\].*"))
			{
				return Base64.getDecoder().decode(s);
			}

			return s.getBytes();
		}
		if (v instanceof Array)
		{
			Array a = (Array)v;
			byte[] tmp = new byte[a.size()];
			for (int i = 0; i < tmp.length; i++)
			{
				tmp[i] = (Byte)a.get(i);
			}
			return tmp;
		}

		throw new IllegalArgumentException("Unsupported format: " + v.getClass());
	}


	public boolean isNull(K aKey)
	{
		return getImpl(aKey) == null;
	}


	/**
	 * find("name")
	 * find("7")
	 * find("name/7")
	 * find("people/7/name")
	 * find("people/[name=bob]/age")
	 */
	public <T extends Object> T findFirst(String aPath)
	{
		int i = aPath.indexOf('/');

		if (i == -1)
		{
			if (aPath.matches("[0-9]*"))
			{
				return ((Array)this).get(Integer.valueOf(aPath));
			}
			return ((Document)this).get(aPath);
		}
		if (i == 0)
		{
			return findFirst(aPath.substring(1));
		}

		if (aPath.startsWith("[") && aPath.contains("]") && aPath.contains("="))
		{
			String key = aPath.substring(1, aPath.indexOf('='));
			String value = aPath.substring(aPath.indexOf('=') + 1, aPath.indexOf(']'));
			for (Object o : (Array)this)
			{
				if (o instanceof Document)
				{
					Document doc = (Document)o;
					if (value.equals(doc.get(key)))
					{
						T result = doc.findFirst(aPath.substring(aPath.indexOf(']') + 1));
						if (result != null)
						{
							return (T)result;
						}
					}
				}
			}
			return null;
		}

		String path = aPath.substring(0, i);
		String remain = aPath.substring(i + 1);

		KeyValueCollection tmp;
		if (path.matches("[0-9]*"))
		{
			tmp = ((Array)this).get(Integer.valueOf(path));
		}
		else
		{
			tmp = ((Document)this).get(path);
		}
		return (T)tmp.findFirst(remain);
	}


	public Array findMany(String aPath)
	{
		Array result = new Array();
		findMany(aPath, result);
		return result;
	}


	protected <T> void findMany(String aPath, Array aResult)
	{
		int i = aPath.indexOf('/');

		if (aPath.equals("*"))
		{
			if (this instanceof Array)
			{
				aResult.addAll((Array)this);
			}
			else
			{
				aResult.addAll(((Document)this).values());
			}
			return;
		}
		if (i == -1)
		{
			if (aPath.matches("[0-9]*"))
			{
				aResult.add(((Array)this).get(Integer.valueOf(aPath)));
			}
			else if (this instanceof Array)
			{
				((Array)this).forEach(p -> aResult.add(((Document)p).get(aPath)));
			}
			else
			{
				aResult.add(((Document)this).get(aPath));
			}
			return;
		}
		if (i == 0)
		{
			findMany(aPath.substring(1), aResult);
			return;
		}

		if (aPath.startsWith("[") && aPath.contains("]") && aPath.contains("="))
		{
			String key = aPath.substring(1, aPath.indexOf('='));
			String value = aPath.substring(aPath.indexOf('=') + 1, aPath.indexOf(']'));
			for (Object o : (Array)this)
			{
				if (o instanceof Document)
				{
					Document doc = (Document)o;
					if (value.equals(doc.get(key)))
					{
						doc.findMany(aPath.substring(aPath.indexOf(']') + 1), aResult);
					}
				}
			}
			return;
		}

		String path = aPath.substring(0, i);
		String remain = aPath.substring(i + 1);

		if (this instanceof Array)
		{
			if (path.matches("[0-9]*"))
			{
				((KeyValueCollection)((Array)this).get(Integer.valueOf(path))).findMany(remain, aResult);
			}
			else
			{
				((Array)this).forEach(p -> ((KeyValueCollection)((Document)p).get(path)).findMany(remain, aResult));
			}
		}
		else
		{
			KeyValueCollection collection = (KeyValueCollection)((Document)this).get(path);
			if (collection != null)
			{
				collection.findMany(remain, aResult);
			}
		}
	}


	@Override
	public int hashCode()
	{
		return hashCode(new MurmurHash3(0)).getValue();
	}


	void hashCode(MurmurHash3 aChecksum, Object aValue)
	{
		if (aValue instanceof KeyValueCollection)
		{
			((KeyValueCollection)aValue).hashCode(aChecksum);
		}
		else if (aValue instanceof CharSequence)
		{
			aChecksum.updateUTF8((CharSequence)aValue);
		}
		else if (aValue instanceof byte[])
		{
			byte[] buf = (byte[])aValue;
			aChecksum.updateBytes(buf, 0, buf.length);
		}
		else
		{
			aChecksum.updateInt(Objects.hashCode(aValue));
		}
	}


	public static boolean isSupportedType(Object aValue)
	{
		return SupportedTypes.identify(aValue) != null;
	}


	/**
	 * Return a typed JSON, with indentations and line-breaks, of this instance.
	 */
	@Override
	public String toString()
	{
		return new JSONEncoder().marshal(this, false, true, false);
	}


	public R fromJson(String aJson)
	{
		return fromJson(aJson, false);
	}


	/**
	 * @param aRestoreByteShortValues if true low numeric values will be unmarshalled as either Byte or Short; [default] if false Integer.
	 */
	public R fromJson(String aJson, boolean aRestoreByteShortValues)
	{
		return (R)new JSONDecoder().setRestoreByteShortValues(aRestoreByteShortValues).unmarshal(aJson, this);
	}


	public String toJson()
	{
		return new JSONEncoder().marshal(this, true, false, false);
	}


	/**
	 * Encodes this instance into a JSON while retaining some type information.
	 */
	public String toTypedJson()
	{
		return new JSONEncoder().marshal(this, true, true, false);
	}


	/**
	 * Decodes a binary encoded Document/Array.
	 */
	public R fromByteArray(byte[] aBinaryData)
	{
		try (BinaryDecoder decoder = new BinaryDecoder(new ByteArrayInputStream(aBinaryData)))
		{
			decoder.unmarshal(this);
		}
		catch (IOException e)
		{
			throw new StreamException(e.toString());
		}
		return (R)this;
	}


	/**
	 * Return a binary representation of this object.
	 */
	public byte[] toByteArray()
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (BinaryEncoder encoder = new BinaryEncoder(baos))
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
	public R readFrom(InputStream aInputStream) throws IOException
	{
		try (BinaryDecoder decoder = new BinaryDecoder(aInputStream))
		{
			decoder.unmarshal(this);
		}
		return (R)this;
	}


	/**
	 * Write a binary encoded representation of this object to the stream provided.
	 */
	public void writeTo(OutputStream aOutputStream) throws IOException
	{
		try (BinaryEncoder encoder = new BinaryEncoder(aOutputStream))
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

		try (BinaryDecoder decoder = new BinaryDecoder(in))
		{
			decoder.unmarshal(this);
		}
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

		try (BinaryEncoder encoder = new BinaryEncoder(tmp))
		{
			encoder.marshal(this);
		}
	}


	public boolean isEmpty()
	{
		return size() == 0;
	}


	/**
	 * Recursively visits all child elements removing empty Documents or Arrays, or null values.
	 *
	 * @return this Document
	 */
	public R reduce()
	{
		ArrayList<K> keySet = new ArrayList<>();
		for (K key : keySet())
		{
			keySet.add(key);
		}

		for (int i = size(); --i >= 0;)
		{
			K key = keySet.get(i);
			Object value = get(key);
			if (value == null)
			{
				remove(key);
			}
			else if (value instanceof KeyValueCollection)
			{
				KeyValueCollection coll = (KeyValueCollection)value;
				coll.reduce();
				if (coll.isEmpty())
				{
					remove(key);
				}
			}
		}

		return (R)this;
	}


	abstract Object getImpl(K aKey);


	abstract R putImpl(K aKey, Object aValue);


	abstract R remove(K aKey);


	abstract MurmurHash3 hashCode(MurmurHash3 aChecksum);


	public abstract int size();


	public abstract R clear();


	public abstract boolean same(R aOther);


	public abstract Iterable<K> keySet();
}
