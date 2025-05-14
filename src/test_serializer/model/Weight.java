package test_serializer.model;

import java.io.Serializable;


public class Weight extends Physical<Weight> implements Serializable
{
	private String mUnit;

	// overrides Feature field
	private Object mValue;


	public Weight()
	{
		super("Weight");

		mValue = 700;
		mUnit = "kg";
	}
}
