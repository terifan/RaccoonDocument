package org.terifan.raccoon.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Objects;
import org.terifan.raccoon.document.BinaryDecoder.Path;
import static org.terifan.raccoon.document.BinaryEncoder.VERSION;
import org.terifan.raccoon.document.BinaryDecoder.Token;


public class Dictionary implements Externalizable
{
	private final static long serialVersionUID = 1L;

	private HashMap<Object, Integer> mMap;


	public Dictionary()
	{
		mMap = new HashMap<>();
	}


	public int size()
	{
		return mMap.size();
	}


	public static Dictionary of(Collection aTemplate)
	{
		Dictionary dic = new Dictionary();

		HashMap<String, Integer> values = new HashMap<>();

		dic.add(aTemplate, values);

//		for (Entry<String,Integer> entry : values.entrySet())
//		{
//			if (entry.getValue() >= 3 && entry.getKey().matches("[a-zA-Z_0-9]{3,}"))
//			{
//				dic.add(entry.getKey());
//				System.out.println(entry.getKey());
//			}
//		}
		return dic;
	}


	@SuppressWarnings("unchecked")
	private void add(Collection aTemplate, HashMap<String, Integer> aValues)
	{
		for (Object key : aTemplate.keySet())
		{
			add(key);

			switch (aTemplate.get(key))
			{
				case null -> {}
				case Document v ->
					add(v, aValues);
				case Array v ->
				{
					for (Object o : v)
					{
						if (o instanceof Document w)
						{
							add(w, aValues);
						}
					}
				}
				default -> {}
			}
		}
	}


	public void addAll(Object... aKeys)
	{
		for (Object o : aKeys)
		{
			add(o);
		}
	}


	public void add(Object aKey)
	{
		if (!mMap.containsKey(aKey))
		{
			if (BinaryCodec.identify(aKey) == null)
			{
				throw new IllegalArgumentException("Unsupported type");
			}

			mMap.put(aKey, mMap.size());
		}
	}


	public byte[] toByteArray(Collection aCollection) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (BinaryEncoder encoder = new BinaryEncoder(baos, o -> true, this))
		{
			encoder.marshal(aCollection);
		}
		return baos.toByteArray();
	}


	@SuppressWarnings("unchecked")
	public <T extends Collection> T fromByteArray(byte[] aBinaryData)
	{
		try
		{
			BinaryDecoder decoder = new BinaryDecoder(new ByteArrayInputStream(aBinaryData), null);
			decoder.mDictionary = this;
			return (T)decoder.unmarshal();
		}
		catch (IOException e)
		{
			throw new StreamException(e.toString());
		}
	}


	@Override
	public String toString()
	{
		return mMap.keySet().toString();
	}


	@Override
	public void writeExternal(ObjectOutput aOut) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BinaryEncoder encoder = new BinaryEncoder(baos, k -> true);
		encoder.standalone();

		for (Entry<Object, Integer> entry : mMap.entrySet())
		{
			Object value = entry.getKey();
			BinaryCodec type = BinaryCodec.identify(value);
			encoder.writeToken(type, entry.getValue());
			type.encoder.encode(encoder, value);
		}

		encoder.terminate();
		encoder.close();

		byte[] data = baos.toByteArray();
		aOut.writeInt(data.length);
		aOut.write(data);
	}


	@Override
	public void readExternal(ObjectInput aIn) throws IOException, ClassNotFoundException
	{
		byte[] data = new byte[aIn.readInt()];
		aIn.readFully(data);

		BinaryDecoder decoder = new BinaryDecoder(new ByteArrayInputStream(data), null);
		Token token = decoder.readToken();

		if (token.type != BinaryCodec.DICTIONARY || token.value != VERSION)
		{
			throw new StreamException("Unsupported stream encoding version: " + token.value);
		}

		Path path = new Path();

		while ((token = decoder.readToken()).type != BinaryCodec.TERMINATOR)
		{
			mMap.put(token.type.decoder.decode(decoder, path, BinaryDecoder.VisitorResult.CONTINUE), token.value);
		}

		if (token.value != token.checksum)
		{
			throw new StreamException("Checksum error in data stream");
		}
	}


	public byte[] writeExternal() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(baos))
		{
			oos.writeObject(this);
		}
		return baos.toByteArray();
	}


	public void readExternal(byte[] aBuffer) throws IOException
	{
		try (ObjectInputStream oos = new ObjectInputStream(new ByteArrayInputStream(aBuffer)))
		{
			mMap = ((Dictionary)oos.readObject()).mMap;
		}
		catch (ClassNotFoundException e)
		{
			throw new IOException(e);
		}
	}


	@Override
	public int hashCode()
	{
		int hash = 3;
		hash = 97 * hash + Objects.hashCode(this.mMap);
		return hash;
	}


	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		final Dictionary other = (Dictionary)obj;
		return Objects.equals(this.mMap, other.mMap);
	}


	Integer get(String aKey)
	{
		return mMap.get(aKey);
	}


	Integer encode(Object aValue)
	{
		return mMap.get(aValue);
	}


	Object decode(long aIndex)
	{
		for (Entry<Object, Integer> en : mMap.entrySet())
		{
			if (en.getValue() == aIndex)
			{
				return en.getKey();
			}
		}
		return null;
	}
}
