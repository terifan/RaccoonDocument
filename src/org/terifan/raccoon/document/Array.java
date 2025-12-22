package org.terifan.raccoon.document;

import java.io.Externalizable;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import static org.terifan.raccoon.document.SupportedTypes.assertSupported;


public class Array extends Collection<Integer, Array> implements Iterable<Object>, Externalizable, Cloneable, Comparable<Array>, CollectionEntity
{
	private final static long serialVersionUID = 1L;

	private ArrayList<Object> mValues;


	public Array()
	{
		mValues = new ArrayList<>();
	}


	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(Integer aKey)
	{
		return (T)getImpl(aKey);
	}


	public Array addAll(Array aSource)
	{
		mValues.addAll(aSource.mValues);
		return this;
	}


	public Array addEach(Array aArray)
	{
		for (Object o : ((Array)aArray))
		{
			add(o);
		}
		return this;
	}


	@SuppressWarnings("unchecked")
	public <T extends Array> T put(Integer aKey, Object aValue)
	{
		assertSupported(aValue);
		return (T)putImpl(aKey, aValue);
	}


	@SuppressWarnings("unchecked")
	public <T extends Array> T put(String aPath, Object aValue)
	{
		assertSupported(aValue);
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
	@SuppressWarnings("unchecked")
	public Array add(Object aValue)
	{
		if (SupportedTypes.isSupported(aValue))
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
	@SuppressWarnings("unchecked")
	public Array addAll(Object aValue)
	{
		if (aValue.getClass().isArray())
		{
			for (int i = 0; i < java.lang.reflect.Array.getLength(aValue); i++)
			{
				addAll(java.lang.reflect.Array.get(aValue, i));
			}
		}
		else if (aValue instanceof Iterator v)
		{
			v.forEachRemaining(this::addAll);
		}
		else if (aValue instanceof Iterable v)
		{
			v.forEach(this::addAll);
		}
		else if (aValue instanceof Stream v)
		{
			v.forEach(this::addAll);
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


	/**
	 * Add a value if the condition evaluation return true.
	 *
	 * @return this Array
	 */
	public <V> Array addWithCondition(V aValue, Function<V, Boolean> aCondition)
	{
		if (aCondition.apply(aValue))
		{
			add(aValue);
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


	/**
	 * Remove an element at index
	 *
	 * @return the value at this index
	 */
	@Override
	public Object remove(Integer aIndex)
	{
		return mValues.remove((int)aIndex);
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
	public Set<Integer> keySet()
	{
		return new AbstractSet<Integer>()
		{
			@Override
			public boolean contains(Object aObject)
			{
				if (aObject instanceof Number i)
				{
					return i.longValue() < size();
				}
				return false;
			}


			@Override
			@SuppressWarnings("unchecked")
			public Iterator<Integer> iterator()
			{
				return new Iterator<Integer>()
				{
					int i;


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


			@Override
			public int size()
			{
				return Array.this.size();
			}
		};
	}


	@Override
	MurmurHash3 hashCode(MurmurHash3 aChecksum)
	{
		mValues.forEach(value -> hashCodeUpdate(aChecksum, value));

		return aChecksum;
	}


	@Override
	public boolean equals(Object aOther)
	{
		if (aOther == this)
		{
			return true;
		}
		if (aOther instanceof Array other)
		{
			if (size() != other.size())
			{
				return false;
			}
			for (int key = 0, sz = size(); key < sz; key++)
			{
				Object t = get(key);
				Object o = other.get(key);
				if (t instanceof byte[] v && o instanceof byte[] w)
				{
					if (!Arrays.equals(v, w))
					{
						return false;
					}
				}
				else if (!Objects.equals(t, o))
				{
					return false;
				}
			}
			return true;
		}

		return false;
	}


	/**
	 * Create an array of item provided including primitives and arrays.
	 *
	 * @param aValue an array of objects
	 * @return an array
	 */
	@SuppressWarnings("unchecked")
	public static Array of(Object aValue)
	{
		Array array = new Array();

		if (SupportedTypes.isSupported(aValue))
		{
			array.add(aValue);
		}
		else if (aValue.getClass().isArray())
		{
			for (int i = 0, len = java.lang.reflect.Array.getLength(aValue); i < len; i++)
			{
				Object v = java.lang.reflect.Array.get(aValue, i);

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
		else if (aValue instanceof Iterable v)
		{
			v.forEach(array::addAll);
		}
		else if (aValue instanceof Iterator v)
		{
			v.forEachRemaining(array::addAll);
		}
		else if (aValue instanceof Stream v)
		{
			v.forEach(array::addAll);
		}
		else
		{
			throw new IllegalArgumentException("Unsupported type: " + aValue.getClass());
		}

		return array;
	}


	/**
	 * Create an array of item provided including primitives and arrays.
	 *
	 * @param aValues an array of objects
	 * @return an array
	 */
	@SuppressWarnings("unchecked")
	public static Array of(Object... aValues)
	{
		Array array = new Array();

		for (Object value : aValues)
		{
			if (SupportedTypes.isSupported(value))
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


	@SuppressWarnings("unchecked")
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
		try
		{
			Array copy = (Array)super.clone();

			copy.mValues = (ArrayList<Object>)mValues.clone();

			for (int i = 0, sz = size(); i < sz; i++)
			{
				Object v = get(i);
				if (v instanceof Array c)
				{
					copy.put(i, c.clone());
				}
				else if (v instanceof Document c)
				{
					copy.put(i, c.clone());
				}
			}
			return copy;
		}
		catch (CloneNotSupportedException e)
		{
			throw new Error(e);
		}
	}


	@Override
	@SuppressWarnings("unchecked")
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


	public <T> T[] asArrayOf(Class<T> aClass)
	{
		int sz = size();
		Object arr = java.lang.reflect.Array.newInstance(aClass, sz);
		for (int i = 0; i < sz; i++)
		{
			java.lang.reflect.Array.set(arr, i, get(i));
		}
		return (T[])arr;
	}


	/**
	 * Index/Value iterator
	 */
	@Override
	public void forEach(BiConsumer<Integer, Object> aAction)
	{
		for (int i = 0; i < mValues.size(); i++)
		{
			aAction.accept(i, mValues.get(i));
		}
	}


	public int[] asIntArray()
	{
		int[] values = new int[size()];
		for (int i = 0; i < values.length; i++)
		{
			values[i] = getInt(i);
		}
		return values;
	}


	public long[] asLongArray()
	{
		long[] values = new long[size()];
		for (int i = 0; i < values.length; i++)
		{
			values[i] = getLong(i);
		}
		return values;
	}


	public double[] asDoubleArray()
	{
		double[] values = new double[size()];
		for (int i = 0; i < values.length; i++)
		{
			values[i] = getDouble(i);
		}
		return values;
	}


	public String[] asStringArray()
	{
		String[] values = new String[size()];
		for (int i = 0; i < values.length; i++)
		{
			values[i] = getString(i);
		}
		return values;
	}


	public Document[] asDocumentArray()
	{
		Document[] values = new Document[size()];
		for (int i = 0; i < values.length; i++)
		{
			values[i] = getDocument(i);
		}
		return values;
	}


	public <T> T[] as()
	{
		Document[] values = new Document[size()];
		for (int i = 0; i < values.length; i++)
		{
			values[i] = getDocument(i);
		}
		return (T[])values;
	}


	public Array removeValue(Object aValue)
	{
		for (int i = size(); --i >= 0;)
		{
			if (get(i).equals(aValue))
			{
				remove(i);
			}
		}
		return this;
	}


	@SuppressWarnings("unchecked")
	public <T> T getFirst()
	{
		return (T)mValues.get(0);
//		return (T)mValues.getFirst();
	}


	@SuppressWarnings("unchecked")
	public <T> T removeFirst()
	{
		return (T)mValues.remove(0);
//		return (T)mValues.removeFirst();
	}


	@SuppressWarnings("unchecked")
	public <T> T getLast()
	{
		return (T)mValues.get(mValues.size() - 1);
//		return (T)mValues.getLast();
	}


	@SuppressWarnings("unchecked")
	public <T> T removeLast()
	{
		return (T)mValues.remove(mValues.size() - 1);
//		return (T)mValues.removeLast();
	}


	@Override
	public boolean containsKey(Integer aKey)
	{
		return aKey < size();
	}


	VisitorResult __visit(Visitor aVisitor, String aPath)
	{
		for (Object p : this)
		{
			if (p instanceof Document w)
			{
				if (aVisitor.visit(w.get(aPath)) == VisitorResult.ABORT)
				{
					return VisitorResult.ABORT;
				}
			}
		}
		return VisitorResult.CONTINUE;
	}


	@SuppressWarnings("unchecked")
	@Override
	public Array sort(Comparator aComparator)
	{
		Collections.sort(mValues, aComparator);
		return this;
	}


	@Override
	public VisitorResult visit(String aConsumedPath, String aPath, Visitor aVisitor)
	{
		if (aPath.startsWith("["))
		{
			int i = aPath.indexOf(']');
			String number = aPath.substring(1, i);
			if (number.matches("[0-9]+"))
			{
				String remain = aPath.substring(i + 1).trim();
				aConsumedPath += "[" + number + "]";
				return _visit(aConsumedPath, get(Integer.valueOf(number)), remain, aVisitor);
			}
			return evaluatePathExpression(aConsumedPath, aPath.substring(1), aVisitor);
		}

		for (Object item : this)
		{
			if (_visit(aConsumedPath, item, aPath, aVisitor) == VisitorResult.ABORT)
			{
				return VisitorResult.ABORT;
			}
		}
		return VisitorResult.CONTINUE;
	}


	public Document flatten(String aSeparator)
	{
		return flatten(aSeparator, e -> e);
	}


	@Override
	public Document flatten(String aSeparator, Function<String, String> aFormatter)
	{
		Document dest = new Document();
		flatten(dest, this, "", aSeparator, aFormatter);
		return dest;
	}
}
