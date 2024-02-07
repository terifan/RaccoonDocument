package org.terifan.raccoon.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public class Document extends KeyValueContainer<String, Document> implements Externalizable, Cloneable, Comparable<Document>, DocumentEntity
{
	private final static long serialVersionUID = 1L;
	private final static String ALG_FIELD = "alg";
	private final static LinkedHashMap<String, String> ALGORITHMS = new LinkedHashMap<>()
	{
		{
			put("HS256", "HmacSHA256");
			put("HS384", "HmacSHA384");
			put("HS512", "HmacSHA512");
		}
	};

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
		if (!isSupportedType(aValue))
		{
			throw new IllegalArgumentException("Unsupported type: " + aValue.getClass());
		}

		return (T)putImpl(aKey, aValue);
	}


	public <T extends Document> T putIfAbsent(String aKey, Function<String, Object> aValue)
	{
		if (!containsKey(aKey))
		{
			putImpl(aKey, aValue.apply(aKey));
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


	public <T extends Document> T putAll(Document aSource)
	{
		if (aSource != null)
		{
			aSource.entrySet().forEach(entry -> mValues.put(entry.getKey(), entry.getValue()));
		}
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


	/**
	 * Return an encoded signed string representation of this Document. The format is identical to a JWT token.
	 *
	 * @param aSecret secret passphrase used when signing the message
	 */
	public String toSignedString(byte[] aSecret)
	{
		return toSignedString(aSecret, new Document().put(ALG_FIELD, ALGORITHMS.firstEntry().getKey()));
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
			aHeader = new Document().putAll(aHeader).putIfAbsent(ALG_FIELD, k -> ALGORITHMS.firstEntry().getKey());

			byte[] headerBytes = aHeader.toJson(true).getBytes(StandardCharsets.UTF_8);
			byte[] payloadBytes = toJson(true).getBytes(StandardCharsets.UTF_8);

			Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
			byte[] header = encoder.encodeToString(headerBytes).getBytes(StandardCharsets.UTF_8);
			byte[] payload = encoder.encodeToString(payloadBytes).getBytes(StandardCharsets.UTF_8);

			String impl = ALGORITHMS.get(aHeader.getString(ALG_FIELD));
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
	 * @param aMessage a three part base64 encoded and signed message
	 * @param aSecret secret passphrase used when signing the message
	 * @return this document with the content of the message decoded
	 */
	public Document fromSignedString(byte[] aSecret, String aMessage) throws IOException
	{
		return fromSignedString(aSecret, aMessage, null);
	}


	/**
	 * Decode an encoded signed string representation of a Document.
	 *
	 * @param aMessage a three part base64 encoded and signed message
	 * @param aSecret secret passphrase used when signing the message
	 * @param aHeader if not null then the header of the signed message will be returned in this Document
	 * @return this document with the content of the message decoded
	 */
	public Document fromSignedString(byte[] aSecret, String aMessage, Document aHeader) throws IOException
	{
		try
		{
			if (!aMessage.matches("[0-9A-Za-z\\-\\_]{0,}\\.[0-9A-Za-z\\-\\_]{0,}\\.[0-9A-Za-z\\-\\_]{0,}"))
			{
				throw new IllegalArgumentException("Message not formatted correctly.");
			}

			String[] chunks = aMessage.split("\\.");
			byte[] headerBytes = Base64.getUrlDecoder().decode(chunks[0]);
			byte[] payloadBytes = Base64.getUrlDecoder().decode(chunks[1]);

			Document header = new Document().fromJson(new String(headerBytes, StandardCharsets.UTF_8));
			String alg = ALGORITHMS.get(header.getString(ALG_FIELD));

			if (alg == null)
			{
				throw new IllegalArgumentException("Unsupported algorithm: " + alg);
			}

			Mac mac = Mac.getInstance(alg);
			mac.init(new SecretKeySpec(aSecret, alg));
			mac.update((chunks[0] + "." + chunks[1]).getBytes());
			byte[] sign = mac.doFinal();

			if (!chunks[2].equals(Base64.getUrlEncoder().withoutPadding().encodeToString(sign)))
			{
				throw new IOException("Signature missmatch");
			}

			if (aHeader != null)
			{
				aHeader.putAll(new Document().fromJson(new String(headerBytes, StandardCharsets.UTF_8)));
			}

			return fromJson(new String(payloadBytes, StandardCharsets.UTF_8));
		}
		catch (InvalidKeyException | NoSuchAlgorithmException e)
		{
			throw new IllegalStateException(e);
		}
	}


	/**
	 * Return a signed compressed binary representation of this Document.
	 *
	 * @param aSecret secret passphrase used when signing the message
	 */
	public byte[] toSignedBinary(byte[] aSecret) throws IOException
	{
		return toSignedBinary(aSecret, new Document().put(ALG_FIELD, ALGORITHMS.firstEntry().getKey()));
	}


	/**
	 * Return a signed compressed binary representation of this Document.
	 *
	 * note: if the header is missing an "alg" field one will be added. The supported algorithms are "HS256", "HS384", "HS512".
	 *
	 * @param aSecret secret passphrase used when signing the message
	 * @param aHeader an optional custom header document.
	 */
	public byte[] toSignedBinary(byte[] aSecret, Document aHeader) throws IOException
	{
		try
		{
			aHeader = new Document().putAll(aHeader).putIfAbsent(ALG_FIELD, k -> ALGORITHMS.firstEntry().getKey());

			byte[] header = aHeader.toByteArray();
			byte[] payload = toByteArray();

			String impl = ALGORITHMS.get(aHeader.getString(ALG_FIELD));
			Mac mac = Mac.getInstance(impl);
			mac.init(new SecretKeySpec(aSecret, impl));
			mac.update(header);
			mac.update(payload);
			byte[] sign = mac.doFinal();

			return Array.of(compress(header), compress(payload), sign).toByteArray();
		}
		catch (InvalidKeyException | NoSuchAlgorithmException e)
		{
			throw new IllegalStateException(e);
		}
	}


	/**
	 * Decode a signed binary representation of a Document.
	 *
	 * @param aMessage a three part base64 encoded and signed message
	 * @param aSecret secret passphrase used when signing the message
	 * @return this document with the content of the message decoded
	 */
	public Document fromSignedBinary(byte[] aSecret, byte[] aMessage) throws IOException
	{
		return fromSignedBinary(aSecret, aMessage, null);
	}


	/**
	 * Decode a signed binary representation of a Document and retrieving the header.
	 *
	 * @param aMessage a three part base64 encoded and signed message
	 * @param aSecret secret passphrase used when signing the message
	 * @param aHeader if not null then the header of the signed message will be returned in this Document
	 * @return this document with the content of the message decoded
	 */
	public Document fromSignedBinary(byte[] aSecret, byte[] aMessage, Document aHeader) throws IOException
	{
		try
		{
			Array chunks = new Array().fromByteArray(aMessage);

			if (chunks.size() != 3)
			{
				throw new IllegalArgumentException("Expected exactly three entries in the message (array).");
			}

			byte[] headerBytes = decompress(chunks.getBinary(0));
			byte[] payloadBytes = decompress(chunks.getBinary(1));
			Document header = new Document().fromByteArray(headerBytes);

			String alg = ALGORITHMS.get(header.getString(ALG_FIELD));

			if (alg == null)
			{
				throw new IllegalArgumentException("Unsupported algorithm: " + alg);
			}

			Mac mac = Mac.getInstance(alg);
			mac.init(new SecretKeySpec(aSecret, alg));
			mac.update(headerBytes);
			mac.update(payloadBytes);
			byte[] sign = mac.doFinal();

			if (!Arrays.equals(chunks.getBinary(2), sign))
			{
				throw new IOException("Signature missmatch");
			}

			if (aHeader != null)
			{
				aHeader.putAll(header);
			}

			return fromByteArray(payloadBytes);
		}
		catch (InvalidKeyException | NoSuchAlgorithmException e)
		{
			throw new IllegalStateException(e);
		}
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


	private byte[] compress(byte[] aData) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (DeflaterOutputStream iis = new DeflaterOutputStream(baos))
		{
			iis.write(aData);
		}
		return baos.toByteArray();
	}


	private byte[] decompress(byte[] aData) throws IOException
	{
		try (InflaterInputStream iis = new InflaterInputStream(new ByteArrayInputStream(aData)))
		{
			return iis.readAllBytes();
		}
	}
}
