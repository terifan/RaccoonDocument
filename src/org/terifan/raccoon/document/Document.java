package org.terifan.raccoon.document;

import java.io.Externalizable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public class Document extends KeyValueContainer<String, Document> implements Externalizable, Cloneable, Comparable<Document>, DocumentEntity
{
	private final static long serialVersionUID = 1L;
	private final static String SIGNATURE_ALG = "HmacSHA256";
	private final static String SIGNATURE_KEY = "~s";

//	/**
//	 * Comparator for ordering keys. "_id" will always be the lowest key followed with keys with an underscore prefix and remaining normal order.
//	 * E.g. order of keys: [_id, _alpha, 0, A, a]
//	 */
//	final static Comparator<String> COMPARATOR = (p, q) ->
//	{
//		boolean S = "_id".equals(p);
//		boolean T = "_id".equals(q);
//		if (S || T) return S && !T ? -1 : T && !S ? 1 : 0;
//		boolean P = !p.isEmpty() && p.charAt(0) == '_';
//		boolean Q = !q.isEmpty() && q.charAt(0) == '_';
//		return P && !Q ? -1 : Q && !P ? 1 : p.compareTo(q);
//	};

	private final LinkedHashMap<String, Object> mValues;


	public Document()
	{
//		mValues = new LinkedHashMap<>(COMPARATOR);
		mValues = new LinkedHashMap<>();
	}


	@Override
	public <T> T get(String aKey)
	{
		return (T)getImpl(aKey);
	}


	@Override
	public <T> T get(String aKey, T aDefaultValue)
	{
		Object v = getImpl(aKey);
		if (v == null)
		{
			return aDefaultValue;
		}
		return (T)v;
	}


	public <T extends Document> T put(String aKey, Object aValue)
	{
		if (!isSupportedType(aValue))
		{
			throw new IllegalArgumentException("Unsupported type: " + aValue.getClass());
		}

		return (T)putImpl(aKey, aValue);
	}


	public <T extends Document> T putIfAbsent(String aKey, Supplier<Object> aValue)
	{
		if (!containsKey(aKey))
		{
			putImpl(aKey, aValue.get());
		}
		return (T)this;
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


	public <T> T putAll(Document aSource)
	{
		aSource.entrySet().forEach(entry -> mValues.put(entry.getKey(), entry.getValue()));
		return (T)this;
	}


	@Override
	public Document remove(String aKey)
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


	public boolean containsKey(String aKey)
	{
		return mValues.containsKey(aKey);
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
		return new Document().fromByteArray(toByteArray());
	}


	@Override
	public int compareTo(Document aOther)
	{
		ArrayList<String> thisKeys = new ArrayList<>(keySet());
		ArrayList<String> otherKeys = new ArrayList<>(aOther.keySet());

		for (String key : thisKeys.toArray(String[]::new))
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


	public Document sign(String aSecret)
	{
		try
		{
			mValues.remove(SIGNATURE_KEY);

			SecretKeySpec secretKeySpec = new SecretKeySpec(aSecret.getBytes(), SIGNATURE_ALG);
			Mac mac = Mac.getInstance(SIGNATURE_ALG);
			mac.init(secretKeySpec);
			byte[] sign = mac.doFinal(toByteArray());

			mValues.put(SIGNATURE_KEY, sign);

			return this;
		}
		catch (InvalidKeyException | NoSuchAlgorithmException e)
		{
			throw new IllegalStateException(e);
		}
	}


	public boolean verifty(String aSecret)
	{
		try
		{
			byte[] provided = (byte[])mValues.remove(SIGNATURE_KEY);

			if (provided == null)
			{
				throw new IllegalArgumentException("Document has no embedded signature.");
			}

			SecretKeySpec secretKeySpec = new SecretKeySpec(aSecret.getBytes(), SIGNATURE_ALG);
			Mac mac = Mac.getInstance(SIGNATURE_ALG);
			mac.init(secretKeySpec);
			byte[] control = mac.doFinal(toByteArray());

			mValues.put(SIGNATURE_KEY, provided);

			return Arrays.equals(control, provided);
		}
		catch (InvalidKeyException | NoSuchAlgorithmException e)
		{
			throw new IllegalStateException(e);
		}
	}


	public byte[] getSignature()
	{
		return (byte[])mValues.get(SIGNATURE_KEY);
	}
}
