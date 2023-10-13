package org.terifan.raccoon.document;

import java.io.Externalizable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.stream.Stream;


public class Array extends KeyValueContainer<Integer, Array> implements Iterable, Externalizable, Cloneable, Comparable<Array>, DocumentEntity
{
	private final static long serialVersionUID = 1L;

	private final ArrayList<Object> mValues;


	public Array()
	{
		mValues = new ArrayList<>();
	}


	@Override
	public <T> T get(Integer aKey)
	{
		return (T)getImpl(aKey);
	}


	public Array addAll(Array aSource)
	{
		mValues.addAll(aSource.mValues);
		return this;
	}


	public <T extends Array> T put(Integer aKey, Object aValue)
	{
		if (!isSupportedType(aValue))
		{
			throw new IllegalArgumentException("Unsupported type: " + aValue.getClass());
		}

		return (T)putImpl(aKey, aValue);
	}


	public <T extends Array> T put(String aPath, Object aValue)
	{
		if (!isSupportedType(aValue))
		{
			throw new IllegalArgumentException("Unsupported type: " + aValue.getClass());
		}

		return (T)putImpl(Integer.valueOf(aPath), aValue);
	}


	@Override
	Object getImpl(Integer aIndex)
	{
		return aIndex >= mValues.size() ? null : mValues.get(aIndex);
	}


	public boolean contains(Object aValue)
	{
		return mValues.contains(aValue);
	}


	/**
	 * Add the item to this Array. If the value provided is an array, list or stream an Array is created.
	 *
	 * @return this Array
	 */
	public Array add(Object aValue)
	{
		if (aValue == null || isSupportedType(aValue))
		{
			mValues.add(aValue);
		}
		else if (aValue.getClass().isArray())
		{
			Array arr = new Array();
			for (int i = 0, len = java.lang.reflect.Array.getLength(aValue); i < len; i++)
			{
				arr.add(java.lang.reflect.Array.get(aValue, i));
			}
			mValues.add(arr);
		}
		else if (aValue instanceof Iterable v)
		{
			Array arr = new Array();
			v.forEach(mValues::add);
			mValues.add(arr);
		}
		else if (aValue instanceof Stream v)
		{
			Array arr = new Array();
			v.forEach(mValues::add);
			mValues.add(arr);
		}
		else
		{
			throw new IllegalArgumentException("Unsupported type: " + aValue.getClass());
		}

		return this;
	}


	/**
	 * Add each item provided, same as calling the <code>add</code> method for each item.
	 *
	 * @return this Array
	 */
	public Array addAll(Object aValue)
	{
		if (aValue.getClass().isArray())
		{
			for (int i = 0; i < java.lang.reflect.Array.getLength(aValue); i++)
			{
				addAll(java.lang.reflect.Array.get(aValue, i));
			}
		}
		else if (aValue instanceof Iterable v)
		{
			for (Object o : v)
			{
				addAll(o);
			}
		}
		else
		{
			add(aValue);
		}

		return this;
	}


	/**
	 * Add each item provided, same as calling the <code>add</code> method for each item.
	 *
	 * @return this Array
	 */
	public Array addAll(Object... aValue)
	{
		for (Object o : aValue)
		{
			addAll(o);
		}

		return this;
	}


	@Override
	Array putImpl(Integer aIndex, Object aValue)
	{
		while (aIndex > mValues.size())
		{
			mValues.add(null);
		}
		if (aIndex == mValues.size())
		{
			mValues.add(aValue);
		}
		else
		{
			mValues.set(aIndex, aValue);
		}
		return this;
	}


	@Override
	public int size()
	{
		return mValues.size();
	}


	@Override
	public Array clear()
	{
		mValues.clear();
		return this;
	}


	@Override
	public Array remove(Integer aIndex)
	{
		mValues.remove((int)aIndex);
		return this;
	}


	@Override
	public Iterator iterator()
	{
		return mValues.iterator();
	}


	public Stream stream()
	{
		return mValues.stream();
	}


	@Override
	public Iterable<Integer> keySet()
	{
		return new Iterable<Integer>()
		{
			int i;

			@Override
			public Iterator<Integer> iterator()
			{
				return new Iterator<Integer>()
				{
					@Override
					public boolean hasNext()
					{
						return i < size();
					}


					@Override
					public Integer next()
					{
						return i++;
					}
				};
			}
		};
	}


	@Override
	MurmurHash3 hashCode(MurmurHash3 aChecksum)
	{
		aChecksum.updateInt(93090393 ^ size()); // == "array".hashCode()

		mValues.forEach(value -> super.hashCode(aChecksum, value));

		return aChecksum;
	}


	@Override
	public boolean equals(Object aOther)
	{
		if (aOther instanceof Array v)
		{
			return mValues.equals(v.mValues);
		}

		return false;
	}


	/**
	 * Order independent equals comparison.
	 */
	@Override
	public boolean same(Array aOther)
	{
		if (!(aOther instanceof Array))
		{
			return false;
		}
		if (aOther.size() != mValues.size())
		{
//			System.out.println("Different number of entries in provided Array: found: " + aOther.size() + ", expected: " + size());
			return false;
		}

		for (int i = 0; i < mValues.size(); i++)
		{
			Object value = getImpl(i);
			Object otherValue = aOther.getImpl(i);

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
				return false;
			}
		}

		return true;
	}


	/**
	 * Create an array of item provided including primitives and arrays.
	 *
	 * @param aValues an array of objects
	 * @return an array
	 */
	public static Array of(Object... aValues)
	{
		Array array = new Array();

		for (Object value : aValues)
		{
			if (value == null || isSupportedType(value))
			{
				array.add(value);
			}
			else if (value.getClass().isArray())
			{
				for (int i = 0, len = java.lang.reflect.Array.getLength(value); i < len; i++)
				{
					Object v = java.lang.reflect.Array.get(value, i);

					if (v == null || !v.getClass().isArray())
					{
						array.mValues.add(v);
					}
					else
					{
						array.mValues.add(of(v));
					}
				}
			}
			else if (value instanceof Iterable v)
			{
				v.forEach(array::add);
			}
			else if (value instanceof Stream v)
			{
				v.forEach(array::add);
			}
			else
			{
				throw new IllegalArgumentException("Unsupported type: " + value.getClass());
			}
		}

		return array;
	}


	public <T> Stream<T> stream(Class<T> aType)
	{
		return (Stream<T>)mValues.stream();
	}


	public Object[] values()
	{
		return mValues.toArray();
	}


	/**
	 * Performs a deep clone of this Array and all it's values.
	 */
	@Override
	public Array clone()
	{
		return new Array().fromByteArray(toByteArray());
	}


	@Override
	public int compareTo(Array aOther)
	{
		for (int i = 0, sz = Math.min(size(), aOther.size()); i < sz; i++)
		{
			Object o = aOther.get(i);
			Comparable t = get(i);
			if (t == null)
			{
				if (o == null)
				{
					continue;
				}
				return -1;
			}

			if (!t.getClass().isAssignableFrom(o.getClass()))
			{
				return (t.getClass().getSimpleName() + ":" + t.toString()).compareTo(o.getClass().getSimpleName() + ":" + o.toString());
			}

			int v = t.compareTo(o);
			if (v != 0)
			{
				return v;
			}
		}

		if (size() < aOther.size())
		{
			return -1;
		}
		if (size() > aOther.size())
		{
			return 1;
		}

		return 0;
	}


	public <T> Iterable<T> iterable(Class<T> aClass)
	{
		return new Iterable<T>()
		{
			int i;

			@Override
			public Iterator<T> iterator()
			{
				return new Iterator<T>()
				{
					@Override
					public boolean hasNext()
					{
						return i < size();
					}


					@Override
					public T next()
					{
						return get(i++);
					}
				};
			}
		};
	}


	public void forEach(BiConsumer<Integer, Object> aAction)
	{
		for (int i = 0; i < mValues.size(); i++)
		{
			aAction.accept(i, mValues.get(i));
		}
	}


	public int[] asInts()
	{
		int[] values = new int[size()];
		for (int i = 0; i < size(); i++)
		{
			values[i] = getInt(i);
		}
		return values;
	}


	public long[] asLongs()
	{
		long[] values = new long[size()];
		for (int i = 0; i < size(); i++)
		{
			values[i] = getLong(i);
		}
		return values;
	}


	public Array removeValue(Object aValue)
	{
		for (int i = 0; i < size(); i++)
		{
			if (get(i).equals(aValue))
			{
				remove(i);
			}
		}
		return this;
	}
}
