package test_serializer.model;

import java.io.Serializable;


public abstract class Physical<T extends Feature> extends Feature<T> implements Serializable
{
	private String mName;


	public Physical(String aName)
	{
		this.mName = aName;
	}
}
