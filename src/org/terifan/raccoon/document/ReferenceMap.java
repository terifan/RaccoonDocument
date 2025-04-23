package org.terifan.raccoon.document;

import java.util.ArrayList;


class ReferenceMap
{
	private ArrayList<Collection> mKeys;
	private ArrayList<String> mPaths;


	ReferenceMap()
	{
		mKeys = new ArrayList<>();
		mPaths = new ArrayList<>();
	}


	Collection get(int aIndex)
	{
		return mKeys.get(aIndex);
	}


	void add(Collection aCollection, String aValue)
	{
		mKeys.add(aCollection);
		mPaths.add(aValue);
	}


	void remove(Collection aKey)
	{
		for (int i = 0; i < mKeys.size(); i++)
		{
			if (mKeys.get(i) == aKey)
			{
				mKeys.remove(i);
				mPaths.remove(i);
				return;
			}
		}
	}


	/**
	 * @return true if instance already was registered
	 */
	boolean register(Collection aKey, String aValue)
	{
		if (contains(aKey))
		{
			return true;
		}
		mKeys.add(aKey);
		mPaths.add(aValue);
		return false;
	}


	boolean contains(Collection aKey)
	{
		return indexOf(aKey) != -1;
	}


	int indexOf(Collection aKey)
	{
		for (int i = 0; i < mKeys.size(); i++)
		{
			if (mKeys.get(i) == aKey)
			{
				return i;
			}
		}
		return -1;
	}


	String get(Collection aKey)
	{
		for (int i = 0; i < mKeys.size(); i++)
		{
			if (mKeys.get(i) == aKey)
			{
				return mPaths.get(i);
			}
		}
		return null;
	}


	@Override
	public String toString()
	{
		return "ReferenceMap{" + "size=" + mKeys.size() + '}';
	}
}
