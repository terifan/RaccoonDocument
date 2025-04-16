package org.terifan.raccoon.document;

import java.util.ArrayList;


class ReferenceMap
{
	private ArrayList<Collection> mList;


	ReferenceMap()
	{
		mList = new ArrayList<>();
	}


	Collection get(int aIndex)
	{
		return mList.get(aIndex);
	}


	void add(Collection aCollection)
	{
		mList.add(aCollection);
	}


	void remove(Collection aCollection)
	{
		for (int i = 0; i < mList.size(); i++)
		{
			if (mList.get(i) == aCollection)
			{
				mList.remove(i);
				return;
			}
		}
	}


	/**
	 * @return true if instance already was registered
	 */
	boolean register(Collection aCollection)
	{
		if (contains(aCollection))
		{
			return true;
		}
		mList.add(aCollection);
		return false;
	}


	boolean contains(Collection aCollection)
	{
		return indexOf(aCollection) != -1;
	}


	int indexOf(Collection aCollection)
	{
		for (int i = 0; i < mList.size(); i++)
		{
			if (mList.get(i) == aCollection)
			{
				return i;
			}
		}
		return -1;
	}


	@Override
	public String toString()
	{
		return "ReferenceMap{" + "size=" + mList.size() + '}';
	}
}
