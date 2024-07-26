package org.terifan.raccoon.document;

import java.io.Externalizable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;


public class Document extends KeyValueContainer<String, Document> implements Externalizable, Cloneable, Comparable<Document>, DocumentEntity
{
	private final static long serialVersionUID = 1L;

	/**
	 * Comparator for ordering keys. "_id" will always be the lowest key followed with keys with an underscore prefix and remaining normal
	 * order. E.g. order of keys: [_id, _alpha, 0, A, a]
	 */
	public final static Comparator<String> COMPARATOR = (p, q) ->
	{
		boolean S = "_id".equals(p);
		boolean T = "_id".equals(q);
		if (S || T)
		{
			return S && !T ? -1 : T && !S ? 1 : 0;
		}
		boolean P = !p.isEmpty() && p.charAt(0) == '_';
		boolean Q = !q.isEmpty() && q.charAt(0) == '_';
		return P && !Q ? -1 : Q && !P ? 1 : p.compareTo(q);
	};

	private TreeMap<String, Object> mValues;
//	private final SortedMap<String, Object> mValues;


	public Document()
	{
		mValues = new TreeMap<>(COMPARATOR);
//		mValues = new SortedMap<>(COMPARATOR);
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
	@SuppressWarnings("unchecked")
	public <T> T get(String aKey, T aDefaultValue)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return aDefaultValue;
		}
		return (T)v;
	}


	@SuppressWarnings("unchecked")
	public <T extends Document> T put(String aKey, Object aValue)
	{
		if (!SupportedTypes.isSupported(aValue))
		{
			throw new IllegalArgumentException("Unsupported type: " + aValue.getClass());
		}

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
			aSource.entrySet().forEach(entry -> mValues.put(entry.getKey(), entry.getValue()));
		}
		return (T)this;
	}


	/**
	 * Remove an element with a key
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
	public ArrayList<String> keySet()
	{
		return new ArrayList<>(mValues.keySet());
	}


	public Set<Entry<String, Object>> entrySet()
	{
		return mValues.entrySet();
	}


	public Collection<Object> values()
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


	@Override
	MurmurHash3 hashCode(MurmurHash3 aChecksum)
	{
		aChecksum.updateInt(861720859 ^ size()); // == "document".hashCode()

		mValues.entrySet().forEach(entry ->
		{
			aChecksum.updateUTF8(entry.getKey());
			super.hashCode(aChecksum, entry.getValue());
		});

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


	public void forEach(BiConsumer<? super String, ? super Object> aAction)
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
			doc.mValues = new TreeMap<>();
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


	@SuppressWarnings("unchecked")
	public <T extends Document> T increment(String aKey)
	{
		Object v = mValues.get(aKey);
		if (v == null)
		{
			v = 1;
		}
		else if (v instanceof Integer w)
		{
			v = w == Integer.MAX_VALUE ? (long)w + 1 : w + 1;
		}
		else if (v instanceof Long w)
		{
			v = w + 1;
		}
		else if (v instanceof Short w)
		{
			v = w == Short.MAX_VALUE ? (int)w + 1 : w + 1;
		}
		else if (v instanceof Byte w)
		{
			v = w == Byte.MAX_VALUE ? (short)w + 1 : w + 1;
		}
		else if (v instanceof Double w)
		{
			v = w + 1;
		}
		else if (v instanceof Float w)
		{
			v = w + 1;
		}
		else
		{
			throw new IllegalArgumentException("Unsupported type");
		}
		mValues.put(aKey, v);
		return (T)this;
	}
}
