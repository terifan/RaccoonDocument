package test_serializer.model;

import java.io.Serializable;


public class Price_V1 implements Serializable
{
	private final static long serialVersionUID = 1L;

	private double mPrice;
	private String mCurrency;


	public Price_V1()
	{
	}


	public double getPrice()
	{
		return mPrice;
	}


	public Price_V1 setPrice(double aPrice)
	{
		mPrice = aPrice;
		return this;
	}


	public String getCurrency()
	{
		return mCurrency;
	}


	public Price_V1 setCurrency(String aCurrency)
	{
		mCurrency = aCurrency;
		return this;
	}
}
