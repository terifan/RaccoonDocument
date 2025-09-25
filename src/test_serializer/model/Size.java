package test_serializer.model;

import java.io.Serializable;


public class Size extends Physical<Size> implements Serializable
{
	public Size()
	{
		super("Size");
	}
}
