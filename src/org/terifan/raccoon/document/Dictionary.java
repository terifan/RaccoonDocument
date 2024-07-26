package org.terifan.raccoon.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map.Entry;


public class Dictionary implements Serializable
{
	private final static long serialVersionUID = 1L;

	private HashMap<Object, Integer> mMap;


	public Dictionary()
	{
		mMap = new HashMap<>();
	}


	public static Dictionary of(Document aTemplate)
	{
		Dictionary dic = new Dictionary();

		dic.add(aTemplate);

		return dic;
	}


	private void add(Document aTemplate)
	{
		for (Entry<String, Object> entry : aTemplate.entrySet())
		{
			add(entry.getKey());

			if (entry.getValue() instanceof Document v)
			{
				add(v);
			}
			else if (entry.getValue() instanceof Array v)
			{
				for (Object o : v)
				{
					if (o instanceof Document w)
					{
						add(w);
					}
				}
			}
		}
	}


	public void add(Object aKey)
	{
		if (!mMap.containsKey(aKey))
		{
//			if (aKey instanceof String v && v.matches("[0-9]*"))
//			{
//				throw new IllegalArgumentException("Keys cannot be numeric: " + aKey);
//			}

			mMap.put(aKey, mMap.size());
		}
	}


	public byte[] toByteArray(Document aDocument) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (BinaryEncoder encoder = new BinaryEncoder(baos, o -> true, this))
		{
			encoder.marshal(aDocument);
		}
		return baos.toByteArray();
	}


	public Document fromByteArray(byte[] aBinaryData)
	{
		Document doc = new Document();

		try
		{
			BinaryDecoder decoder = new BinaryDecoder(new ByteArrayInputStream(aBinaryData), false);
			decoder.mDictionary = this;
			decoder.unmarshal(doc);
		}
		catch (IOException e)
		{
			throw new StreamException(e.toString());
		}

		return doc;
	}


	@Override
	public String toString()
	{
		return mMap.keySet().toString();
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
