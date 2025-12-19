package org.terifan.raccoon.document;

import java.util.ArrayList;


public class LRU<T>
{
	private final ArrayList<T> mMap;


	public LRU()
	{
		mMap = new ArrayList<>();
	}


	public void add(T aValue)
	{
		mMap.add(0, aValue);
	}


	public int indexOf(T aValue)
	{
		int i = mMap.indexOf(aValue);
		if (i != -1)
		{
			mMap.remove(i);
			mMap.add(0, aValue);
		}
		return i;
	}


	public T valueAt(int aIndex)
	{
		T v = mMap.get(aIndex);
		mMap.remove(v);
		mMap.add(0, v);
		return v;
	}
}
