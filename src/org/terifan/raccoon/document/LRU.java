package org.terifan.raccoon.document;

import java.util.ArrayList;
import java.util.HashMap;


public class LRU<T>
{
	private HashMap<T, Integer> mEncoder;
	private ArrayList<T> mDecoder;


	public LRU(boolean aEncode)
	{
		if (aEncode)
		{
			mEncoder = new HashMap<>();
		}
		else
		{
			mDecoder = new ArrayList<>();
		}
	}


	public void add(T aValue)
	{
		if (mEncoder != null)
		{
			mEncoder.put(aValue, mEncoder.size());
		}
		else
		{
			mDecoder.add(aValue);
		}
	}


	public int indexOf(T aValue)
	{
		return mEncoder.getOrDefault(aValue, -1);
	}


	public T valueAt(int aIndex)
	{
		return mDecoder.get(aIndex);
	}
}
