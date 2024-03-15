package org.terifan.raccoon.document;

import java.util.Arrays;
import java.util.Comparator;


/**
 * SortedMap vs TreeMap
 *  + 50-75% of less memory
 *  + 15% less CPU
 *  + fewer garbage collect events
 *  - much higher memory bandwidth
 *  - 50-100% slower
 */
public class SortedMap<K extends Comparable, V>
{
	private final static int STEP = 8;

	private Comparator mComparator;
	private Comparable[] mKeys;
	private Object[] mValues;
	private int mSize;


	public SortedMap()
	{
		this(Comparator.naturalOrder());
	}


	public SortedMap(Comparator aComparator)
	{
		clear();
		mComparator = aComparator;
	}


	public V get(K aKey)
	{
		int i = binarySearch(mKeys, 0, mSize, aKey);

		if (i < 0)
		{
			return null;
		}

		return (V)mValues[i];
	}


	public void put(K aKey, V aValue)
	{
		int i = binarySearch(mKeys, 0, mSize, aKey);

		if (i >= 0)
		{
			mValues[i] = aValue;
		}
		else
		{
			if (mSize == mKeys.length)
			{
				mKeys = Arrays.copyOfRange(mKeys, 0, mSize + STEP);
				mValues = Arrays.copyOfRange(mValues, 0, mSize + STEP);
			}

			i = -i - 1;

			System.arraycopy(mKeys, i, mKeys, i + 1, mSize - i);
			System.arraycopy(mValues, i, mValues, i + 1, mSize - i);
			mKeys[i] = aKey;
			mValues[i] = aValue;
			mSize++;
		}
	}


	public void remove(K aKey)
	{
		int i = binarySearch(mKeys, 0, mSize, aKey);

		if (i >= 0)
		{
			mSize--;
			System.arraycopy(mKeys, i + 1, mKeys, i, mSize - i);
			System.arraycopy(mValues, i + 1, mValues, i, mSize - i);
			mKeys[mSize] = null;
			mValues[mSize] = null;

			if (mSize == mKeys.length - STEP)
			{
				mKeys = Arrays.copyOfRange(mKeys, 0, mSize + STEP);
				mValues = Arrays.copyOfRange(mValues, 0, mSize + STEP);
			}
		}
	}


	public int size()
	{
		return mSize;
	}


	public void clear()
	{
		mSize = 0;
		mKeys = new Comparable[STEP];
		mValues = new Object[STEP];
	}


	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("{");
		for (int i = 0; i < mSize; i++)
		{
			if (i > 0)
			{
				sb.append(", ");
			}
			sb.append(mKeys[i] + "=" + mValues[i]);
		}
		return sb.append("}").toString();
	}


	private int binarySearch(Comparable[] aArray, int aFromIndex, int aToIndex, Comparable aKey)
	{
		int low = aFromIndex;
		int high = aToIndex - 1;

		while (low <= high)
		{
			int mid = (low + high) >>> 1;
			int cmp = mComparator.compare(aArray[mid], aKey);

			if (cmp < 0)
			{
				low = mid + 1;
			}
			else if (cmp > 0)
			{
				high = mid - 1;
			}
			else
			{
				return mid; // key found
			}
		}
		return -(low + 1);  // key not found.
	}
}
