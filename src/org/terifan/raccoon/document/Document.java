package org.terifan.raccoon.document;

import java.io.Externalizable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;


public class Document extends KeyValueCollection<String, Document> implements Externalizable, Cloneable, Comparable<Document>
{
	private final static long serialVersionUID = 1L;

	private final LinkedHashMap<String, Object> mValues;


	public Document()
	{
		mValues = new LinkedHashMap<>();
	}


	@Override
	public <T> T get(String aPath)
	{
		return (T)getImpl(aPath);
	}


	@Override
	public <T> T get(String aPath, T aDefaultValue)
	{
		Object v = getImpl(aPath);
		if (v == null)
		{
			return aDefaultValue;
		}
		return (T)v;
	}


	public <T extends Document> T put(String aPath, Object aValue)
	{
		if (!isSupportedType(aValue))
		{
			throw new IllegalArgumentException("Unsupported type: " + aValue.getClass());
		}

//		if (aPath.startsWith("@"))
//		{
//			putPath(aPath.substring(1), aValue);
//			return (T)this;
//		}

		return (T)putImpl(aPath, aValue);
	}


//	void putPath(String aPath, Object aValue)
//	{
//		int i = aPath.indexOf('/');
//
//		if (i == -1)
//		{
//			put(aPath, aValue);
//			return;
//		}
//
//		String subpath = aPath.substring(0, i);
//
//		KeyValueCollection coll = (KeyValueCollection)getImpl(subpath);
//
//		String next = aPath.substring(i + 1);
//		System.out.println(next);
//
//		if (coll == null)
//		{
//			coll = next.matches("[0-9]+|[0-9]+/.*") ? new Array() : new Document();
//			putImpl(subpath, coll);
//		}
//
//		if (coll instanceof Document)
//		{
//			((Document)coll).putPath(next, aValue);
//		}
//		else
//		{
//			((Array)coll).putPath(next, aValue);
//		}
//	}


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


	public Document putAll(Document aSource)
	{
		aSource.entrySet().forEach(entry -> mValues.put(entry.getKey(), entry.getValue()));
		return this;
	}


	@Override
	public Document removeImpl(String aKey)
	{
		mValues.remove(aKey);
		return this;
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


	public Collection<Object> values()
	{
		return mValues.values();
	}


	public boolean containsKey(String aPath)
	{
		return mValues.containsKey(aPath);
	}


	@Override
	Checksum hashCode(Checksum aChecksum)
	{
		aChecksum.updateInt(861720859 ^ size()); // == "document".hashCode()

		mValues.entrySet().forEach(entry ->
		{
			aChecksum.updateChars(entry.getKey());
			super.hashCode(aChecksum, entry.getValue());
		});

		return aChecksum;
	}


	@Override
	public boolean equals(Object aOther)
	{
		if (aOther instanceof Document)
		{
			return toJson().equals(((Document)aOther).toJson());
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

			if ((value instanceof KeyValueCollection) && (otherValue instanceof KeyValueCollection))
			{
				if (!((KeyValueCollection)value).same(otherValue))
				{
//					System.out.println("Value of key '" + key + "' missmatch: found: " + otherValue + ", expected: " + value);
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
		return new Document().fromByteArray(toByteArray());
	}


	@Override
	public int compareTo(Document aOther)
	{
		ArrayList<String> thisKeys = new ArrayList<>(keySet());
		ArrayList<String> otherKeys = new ArrayList<>(aOther.keySet());

		for (String key : thisKeys.toArray(new String[0]))
		{
			Object a = get(key);
			Object b = aOther.get(key);
			thisKeys.remove(key);
			otherKeys.remove(key);

			int v = b == null ? 1 : ((Comparable)a).compareTo(b);

			if (v != 0)
			{
				return v;
			}
		}

		return otherKeys.isEmpty() ? 0 : -1;
	}


	/**
	 * Decodes the JSON and return a Document, same as using the fromJson instance method.
	 */
	public static Document of(String aJSON)
	{
		return new Document().fromJson(aJSON);
	}


//	@Override
//	protected Result resolveImpl(String aPath)
//	{
//		System.out.println(aPath+" "+getClass());
//
//		int i = aPath.indexOf('/');
//
//		if (i == -1)
//		{
//			return new Result(aPath, this);
//		}
//
//		String subpath = aPath.substring(0, i);
//
//		boolean numeric = subpath.matches("[0-9]+");
//
//		String nextPath = aPath.substring(i + 1);
//		boolean nextNumeric = nextPath.matches("[0-9]+/.*|[0-9]+");
//
//		System.out.println("    "+numeric);
//
//		KeyValueCollection next = (KeyValueCollection)getImpl(subpath);
//
//		if (next == null)
//		{
//			next = nextNumeric ? new Array() : new Document();
//			putImpl(subpath, next);
//		}
//
//		System.out.println("  -    "+nextPath+" "+nextPath.getClass());
//
//		return next.resolveImpl(nextPath);
//	}
}
