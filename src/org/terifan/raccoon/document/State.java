package org.terifan.raccoon.document;

import java.util.LinkedHashMap;


class State
{
	private State mParent;
	private Object mKey;
	private int mLevel;
	private LinkedHashMap<Object, Object> mMap;


	State(State aParent, Object aKey)
	{
		mMap = new LinkedHashMap<>();
		mParent = aParent;
		mKey = aKey;
		mLevel = mParent == null ? 0 : mParent.mLevel + 1;
	}


	State enter(String aKey)
	{
		print(aKey);

		return new State(this, aKey);
	}


	State enter(Integer aKey)
	{
		print(aKey);

		return new State(this, aKey);
	}


	private void print(Object aKey)
	{
//		System.out.println("... ".repeat(1 + mLevel) + aKey + " ".repeat(Math.max(0, 60 - mLevel * 4 - aKey.toString().length())) + map());
	}


	State exit()
	{
		return mParent;
	}


	void put(Object aKey, Object aValue)
	{
		mMap.put(aKey, aValue);

		print(aValue);
	}


	private String map()
	{
		return (mParent == null ? "" : mParent.map()) + " " + mMap;
	}
}
