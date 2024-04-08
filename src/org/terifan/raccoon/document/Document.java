package org.terifan.raccoon.document;

import java.io.Externalizable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


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

	private final TreeMap<String, Object> mValues;
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


	public <T> T getFirst()
	{
		return (T)mValues.firstEntry();
	}


	public <T> T removeFirst()
	{
		return (T)mValues.remove(mValues.firstEntry().getKey());
	}


	public <T> T getLast()
	{
		return (T)mValues.lastEntry();
	}


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

			if (o instanceof String)
			{
				o = "\"" + SupportedTypes.escapeString(o.toString()) + "\"";
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
	 * Return an encoded signed string representation of this Document. The format is identical to a JWT token.
	 *
	 * @param aSecret secret passphrase used when signing the message
	 */
	public String toSignedString(byte[] aSecret)
	{
		return toSignedString(aSecret, new Document().put(ALGORITHM_FIELD, DEFAULT_HASH_ALGORITHM));
	}


	/**
	 * Return an encoded signed string representation of this Document. The format is identical to a JWT token.
	 *
	 * <code>
	 * String jwt = doc.toSignedString("1234", Document.of("typ:JWT,alg:HS512"));
	 * </code>
	 *
	 * note: if the header is missing an "alg" field one will be added. The supported algorithms are "HS256", "HS384", "HS512".
	 *
	 * @param aSecret secret passphrase used when signing the message
	 * @param aHeader a custom header document.
	 */
	public String toSignedString(byte[] aSecret, Document aHeader)
	{
		try
		{
			aHeader = new Document().putAll(aHeader).putIfAbsent(ALGORITHM_FIELD, k -> DEFAULT_HASH_ALGORITHM);

			byte[] headerBytes = aHeader.toJson(true).getBytes(StandardCharsets.UTF_8);
			byte[] payloadBytes = toJson(true).getBytes(StandardCharsets.UTF_8);

			Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
			byte[] header = encoder.encodeToString(headerBytes).getBytes(StandardCharsets.UTF_8);
			byte[] payload = encoder.encodeToString(payloadBytes).getBytes(StandardCharsets.UTF_8);

			String impl = HASH_ALGORITHMS.get(aHeader.getString(ALGORITHM_FIELD));
			Mac mac = Mac.getInstance(impl);
			mac.init(new SecretKeySpec(aSecret, impl));
			mac.update(header);
			mac.update((byte)'.');
			mac.update(payload);
			byte[] sign = mac.doFinal();

			return new String(header) + "." + new String(payload) + "." + encoder.encodeToString(sign);
		}
		catch (InvalidKeyException | NoSuchAlgorithmException e)
		{
			throw new IllegalStateException(e);
		}
	}


	/**
	 * Decode an encoded signed string representation of a Document.
	 *
	 * @param aSecret secret used when signing the message
	 * @param aMessage a three part base64 encoded and signed message
	 * @return this document with the content of the message decoded
	 */
	public Document fromSignedString(byte[] aSecret, String aMessage) throws IOException
	{
		return fromSignedString(aSecret, aMessage, null);
	}


	/**
	 * Decode an encoded signed string representation of a Document.
	 *
	 * @param aSecret secret used when signing the message
	 * @param aMessage a three part base64 encoded and signed message
	 * @param aDecodedHeader if not null then the header of the signed message will be returned in this Document
	 * @return this document with the content of the message decoded
	 */
	public Document fromSignedString(byte[] aSecret, String aMessage, Document aDecodedHeader) throws IOException
	{
		return fromSignedString(header -> aSecret, aMessage, aDecodedHeader);
	}


	/**
	 * Decode an encoded signed string representation of a Document.
	 *
	 * @param aSecretProvider Function returning the secret used when signing the message. The function receive the decoded header of the message.
	 * @param aMessage a three part base64 encoded and signed message
	 * @param aDecodedHeader if not null then the header of the signed message will be returned in this Document
	 * @return this document with the content of the message decoded
	 * @throws IOException on signature mismatch
	 */
	public Document fromSignedString(Function<Document, byte[]> aSecretProvider, String aMessage, Document aDecodedHeader) throws IOException
	{
		if (!aMessage.matches("[0-9A-Za-z\\-\\_]{0,}\\.[0-9A-Za-z\\-\\_]{0,}\\.[0-9A-Za-z\\-\\_]{0,}"))
		{
			throw new IllegalArgumentException("Message not formatted correctly.");
		}

		Decoder decoder = Base64.getUrlDecoder();

		int i = aMessage.indexOf('.');
		int j = aMessage.lastIndexOf('.');
		String messageHeader = aMessage.substring(0, i);
		String messagePayload = aMessage.substring(i + 1, j);
		String messageSignature = aMessage.substring(j + 1);

		Document header = new Document().fromJson(new String(decoder.decode(messageHeader), StandardCharsets.UTF_8));

		String alg = HASH_ALGORITHMS.get(header.get(ALGORITHM_FIELD, DEFAULT_HASH_ALGORITHM));

		if (alg == null)
		{
			throw new IllegalArgumentException("Unsupported algorithm: " + alg);
		}

		try
		{
			Mac mac = Mac.getInstance(alg);
			mac.init(new SecretKeySpec(aSecretProvider.apply(header), alg));
			mac.update(messageHeader.getBytes());
			mac.update((byte)'.');
			mac.update(messagePayload.getBytes());
			byte[] sign = mac.doFinal();

			if (!Arrays.equals(decoder.decode(messageSignature), sign))
			{
				throw new IOException("Signature mismatch");
			}
		}
		catch (InvalidKeyException | NoSuchAlgorithmException e)
		{
			throw new IOException(e);
		}

		if (aDecodedHeader != null)
		{
			aDecodedHeader.putAll(header);
		}

		return fromJson(new String(decoder.decode(messagePayload), StandardCharsets.UTF_8));
	}


	public <T extends Document> T increment(String aKey)
	{
		Object v = switch (mValues.get(aKey))
		{
			case null ->
				1;
			case Integer w ->
				w == Integer.MAX_VALUE ? (long)w + 1 : w + 1;
			case Short w ->
				w == Short.MAX_VALUE ? (int)w + 1 : w + 1;
			case Byte w ->
				w == Byte.MAX_VALUE ? (short)w + 1 : w + 1;
			case Double w ->
				w + 1;
			case Float w ->
				w + 1;
			default ->
				throw new IllegalArgumentException("Unsupported type");
		};
		mValues.put(aKey, v);
		return (T)this;
	}
}
