package test_serializer.model;

import java.io.Serializable;


public abstract class Feature<T extends Feature> implements Serializable
{
	private final static long serialVersionUID = 1L;
	private Object mValue;


	public Object getValue()
	{
		return mValue;
	}


	public T setValue(Object aValue)
	{
		mValue = aValue;
		return (T)this;
	}
}
