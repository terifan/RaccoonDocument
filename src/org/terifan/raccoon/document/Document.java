package org.terifan.raccoon.document;

import java.io.Externalizable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import static org.terifan.raccoon.document.SupportedTypes.assertSupported;


public class Document extends Collection<String, Document> implements Externalizable, Cloneable, Comparable<Document>, DocumentEntity
{
	private final static long serialVersionUID = 1L;

	private LinkedHashMap<String, Object> mValues;


	public Document()
	{
		mValues = new LinkedHashMap<>();
	}


	public Document(Object aIdentity)
	{
		this();
		put("_id", aIdentity);
	}


	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(String aKey)
	{
		return (T)getImpl(aKey);
	}


	@Override
	@SuppressWarnings(
		{
			"unchecked", "unchecked"
		})
	public <T> T get(String aKey, T aDefaultValue)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return aDefaultValue;
		}
		if (aDefaultValue != null && v instanceof Number w && aDefaultValue.getClass() != v.getClass())
		{
			if (aDefaultValue instanceof Integer)
			{
				return (T)(Integer)w.intValue();
			}
			if (aDefaultValue instanceof Long)
			{
				return (T)(Long)w.longValue();
			}
			if (aDefaultValue instanceof Double)
			{
				return (T)(Double)w.doubleValue();
			}
			if (aDefaultValue instanceof Float)
			{
				return (T)(Float)w.floatValue();
			}
			if (aDefaultValue instanceof String)
			{
				return (T)v.toString();
			}
			if (aDefaultValue instanceof Short)
			{
				return (T)(Short)w.shortValue();
			}
			if (aDefaultValue instanceof Byte)
			{
				return (T)(Byte)w.byteValue();
			}
		}
		if (aDefaultValue instanceof Boolean && !(v instanceof Boolean))
		{
			return (T)Boolean.valueOf(v.toString());
		}
		return (T)v;
	}


	@SuppressWarnings("unchecked")
	public <T extends Document> T put(String aKey, Object aValue)
	{
		assertSupported(aValue);
		return (T)putImpl(aKey, aValue);
	}


	@Override
	Document putImpl(String aKey, Object aValue)
	{
		if (aKey == null)
		{
			throw new IllegalArgumentException("Keys cannot be null.");
		}

		mValues.put(aKey, aValue);

		return this;
	}


	@Override
	Object getImpl(String aKey)
	{
		return mValues.get(aKey);
	}


	@SuppressWarnings("unchecked")
	public <T extends Document> T putAll(Document aSource)
	{
		if (aSource != null)
		{
			aSource.entrySet().forEach(entry -> put(entry.getKey(), entry.getValue()));
		}
		return (T)this;
	}


	/**
	 * Remove an element with a key
	 *
	 * @return the value stored with that key or null if no value
	 */
	@Override
	public Object remove(String aKey)
	{
		return mValues.remove(aKey);
	}


	@Override
	public Document clear()
	{
		mValues.clear();
		return this;
	}


	@Override
	public int size()
	{
		return mValues.size();
	}


	@Override
	public Set<String> keySet()
	{
		return mValues.keySet();
	}


	public Set<Entry<String, Object>> entrySet()
	{
		return mValues.entrySet();
	}


	public java.util.Collection<Object> values()
	{
		return mValues.values();
	}


	@Override
	public boolean containsKey(String aKey)
	{
		return mValues.containsKey(aKey);
	}


	@SuppressWarnings("unchecked")
	public <T> T getFirst()
	{
		return (T)mValues.firstEntry();
	}


	@SuppressWarnings("unchecked")
	public <T> T removeFirst()
	{
		return (T)mValues.remove(mValues.firstEntry().getKey());
	}


	@SuppressWarnings("unchecked")
	public <T> T getLast()
	{
		return (T)mValues.lastEntry();
	}


	@SuppressWarnings("unchecked")
	public <T> T removeLast()
	{
		return (T)mValues.remove(mValues.lastEntry().getKey());
	}


	public Array keys()
	{
		return Array.of(keySet());
	}


	@SuppressWarnings("unchecked")
	public Document map(Function<String, String> aMapper)
	{
		LinkedHashMap<String, Object> tmp = new LinkedHashMap<>();
		for (Entry<String, Object> entry : mValues.entrySet())
		{
			tmp.put(aMapper.apply(entry.getKey()).toString(), entry.getValue());
		}
		mValues = tmp;
		return this;
	}


	@Override
	MurmurHash3 hashCode(MurmurHash3 aChecksum, ReferenceMap aLinkedList)
	{
		aChecksum.updateInt("document".hashCode());
		aChecksum.updateInt(size());

		if (aLinkedList.contains(this))
		{
			aChecksum.updateInt(aLinkedList.indexOf(this));
			return aChecksum;
		}

		aLinkedList.add(this, null);

		mValues.entrySet().forEach(entry ->
		{
			aChecksum.updateUTF8(entry.getKey());
			super.hashCode(aChecksum, entry.getValue(), aLinkedList);
		});

		aLinkedList.remove(this);

		return aChecksum;
	}


	@Override
	public boolean equals(Object aOther)
	{
		if (aOther instanceof Document v)
		{
			return toJson().equals(v.toJson());
		}

		return false;
	}


	/**
	 * Order independent equals comparison.
	 */
	@Override
	public boolean same(Document aOther)
	{
		if (!(aOther instanceof Document))
		{
			return false;
		}
		if (aOther.size() != mValues.size())
		{
//			System.out.println("Different number of entries: found: " + aOther.size() + ", expected: " + size());
			return false;
		}

		HashSet<String> otherKeys = new HashSet<>(aOther.keySet());

		for (String key : keySet())
		{
			Object value = get(key);
			Object otherValue = aOther.get(key);

			if ((value instanceof Array v1) && (otherValue instanceof Array v2))
			{
				if (!v1.same(v2))
				{
					return false;
				}
			}
			else if ((value instanceof Document v1) && (otherValue instanceof Document v2))
			{
				if (!v1.same(v2))
				{
					return false;
				}
			}
			else if (!value.equals(otherValue))
			{
//				System.out.println("Value of key '" + key + "' missmatch: found: " + otherValue + ", expected: " + value);
				return false;
			}
			otherKeys.remove(key);
		}

		return true;
	}


	/** 
	 * Key/Value iterator
	 */
	@Override
	public void forEach(BiConsumer<String, Object> aAction)
	{
		mValues.forEach(aAction);
	}


	/**
	 * Performs a deep clone of this Document and all it's values.
	 */
	@Override
	public Document clone()
	{
		try
		{
			Document doc = (Document)super.clone();

			// ???
			doc.mValues = new LinkedHashMap<>();
			return doc.fromByteArray(toByteArray());
		}
		catch (CloneNotSupportedException e)
		{
			throw new Error(e);
		}
	}


	@Override
	@SuppressWarnings("unchecked")
	public int compareTo(Document aOther)
	{
		ArrayList<String> thisKeys = new ArrayList<>(keySet());
		ArrayList<String> othrKeys = new ArrayList<>(aOther.keySet());

		for (String key : thisKeys.toArray(String[]::new))
		{
			Comparable a = get(key);
			Comparable b = aOther.get(key);
			thisKeys.remove(key);
			othrKeys.remove(key);

			if (b == null)
			{
				return 1;
			}

			int v = a.compareTo(b);
			if (v != 0)
			{
				return v;
			}
		}

		return othrKeys.isEmpty() ? 0 : -1;
	}


	/**
	 * Decodes the JSON and return a Document, same as using the fromJson instance method.
	 */
	public static Document of(String aJSON)
	{
		if (!aJSON.startsWith("{"))
		{
			aJSON = "{" + aJSON + "}";
		}
		return new Document().fromJson(aJSON);
	}


	public static Document of(String aFormat, Object... aParameters)
	{
		StringBuilder sb = new StringBuilder();
		String remaining = aFormat;

		for (Object o : aParameters)
		{
			int i = remaining.indexOf('$');

			while (i != -1 && remaining.length() > i + 1 && remaining.charAt(i + 1) == '$')
			{
				sb.append(remaining.substring(0, i + 1));
				remaining = remaining.substring(i + 2);
				i = remaining.indexOf('$');
			}

			if (i == -1)
			{
				throw new IllegalArgumentException("More parameters than placeholders: " + aFormat);
			}

			sb.append(remaining.substring(0, i));

			if (o instanceof String s)
			{
				o = "\"" + SupportedTypes.escapeString(s) + "\"";
			}
			else if (SupportedTypes.isExtendedType(o))
			{
				o = SupportedTypes.encode(o, true);
			}

			sb.append(o);

			remaining = remaining.substring(i + 1);
		}

		return of(sb.toString() + remaining.replace("$$", "$"));
	}


	/**
	 * Puts the value for the key specified, appends the value to an existing array, or create an array if a value already exists.
	 * <pre>
	 * doc = new Document();
	 *   {}
	 * doc.append("name", "bob");
	 *   {"name": "bob"}
	 * doc.append("name", "cindy");
	 *   {"name": ["bob", "cindy"]}
	 * </pre>
	 */
	public Document append(String aKey, Object... aValue)
	{
		for (Object a : aValue)
		{
			Object existing = get(aKey);
			if (existing instanceof Array v)
			{
				v.add(a);
			}
			else if (existing != null)
			{
				put(aKey, Array.of(existing, a));
			}
			else
			{
				put(aKey, a);
			}
		}
		return this;
	}


	/**
	 * Update the stored value, incrementing it with the amount provided. Stored value is cast to long.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Document> T increment(String aKey, long aAmount)
	{
		Object value = mValues.get(aKey);

		if (value == null || "".equals(value))
		{
			value = 0L;
		}
		else if (value instanceof String v)
		{
			value = Long.valueOf(v);
		}

		if (value instanceof Number v)
		{
			value = v.longValue() + aAmount;
		}
		else
		{
			throw new IllegalArgumentException("The value of the key specified must be an long value (String and Number are cast to long).");
		}

		mValues.put(aKey, value);
		return (T)this;
	}


	@Override
	public Document sort(Comparator<String> aComparator)
	{
		TreeMap<String, Object> tmp = new TreeMap<>(aComparator);
		tmp.putAll(mValues);
		mValues = new LinkedHashMap<>(tmp);
		return this;
	}


	@SuppressWarnings("unchecked")
	public Document merge(Document aOther, BiFunction aMergeFunction)
	{
		if (isEmpty())
		{
			mValues.putAll(aOther.mValues);
		}
		else
		{
			for (Entry<String, Object> entry : aOther.entrySet())
			{
				if (mValues.containsKey(entry.getKey()))
				{
					mValues.merge(entry.getKey(), entry.getValue(), aMergeFunction);
				}
				else
				{
					mValues.put(entry.getKey(), entry.getValue());
				}
			}
		}

		return this;
	}


//	void merge(String aKey, Object aValue, BiFunction aMerger)
//	{
//		put(aKey, aMerger.apply(get(aKey), aValue));
//	}
	public Document flatten(String aSeparator)
	{
		return flatten(aSeparator, e -> e);
	}


	@Override
	public Document flatten(String aSeparator, Function<String, String> aFormatter)
	{
		Document dest = new Document();
		flatten(dest, this, "", aSeparator, aFormatter);
		clear();
		putAll(dest);
		return this;
	}


	public Map<String, Object> copyTo(Map<String, Object> aMap)
	{
		for (Entry<String, Object> entry : entrySet())
		{
			aMap.put(entry.getKey(), entry.getValue());
		}
		return aMap;
	}
}
