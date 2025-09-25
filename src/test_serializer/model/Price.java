package test_serializer.model;

import java.io.Serializable;
import java.util.Currency;


public class Price implements Serializable
{
	private final static long serialVersionUID = 2L;

	private double mValue;
	private Currency mCurrency;


	public Price()
	{
	}


	public double getValue()
	{
		return mValue;
	}


	public Price setValue(double aValue)
	{
		mValue = aValue;
		return this;
	}


	public Currency getCurrency()
	{
		return mCurrency;
	}


	public Price setCurrency(Currency aCurrency)
	{
		mCurrency = aCurrency;
		return this;
	}
}
