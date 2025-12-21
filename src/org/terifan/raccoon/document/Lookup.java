package org.terifan.raccoon.document;

import java.util.ArrayList;


public class Lookup<T>
{
	private final ArrayList<T> mMap;


	public Lookup()
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
		return i;
	}


	public T valueAt(int aIndex)
	{
		T v = mMap.get(aIndex);
		return v;
	}
}
