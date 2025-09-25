package test_serializer.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


public class Product implements Serializable
{
	private final static long serialVersionUID = 1L;

	private String mName;
	private Price mPrice;
	private Category mCategory;
	private ArrayList<Feature> mFeatures;
	private HashMap<Object, Object> mProperties;


	public Product()
	{
		mFeatures = new ArrayList();
		mProperties = new HashMap();
	}


	public HashMap<Object, Object> getProperties()
	{
		return mProperties;
	}


	public Product setProperties(HashMap<Object, Object> aProperties)
	{
		mProperties = aProperties;
		return this;
	}


	public Product setProperty(Object aKey, Object aValue)
	{
		mProperties.put(aKey, aValue);
		return this;
	}


	public ArrayList<Feature> getFeatures()
	{
		return mFeatures;
	}


	public Product setFeatures(ArrayList<Feature> aFeatures)
	{
		mFeatures = aFeatures;
		return this;
	}


	public Product addFeature(Feature... aFeatures)
	{
		mFeatures.addAll(Arrays.asList(aFeatures));
		return this;
	}


	public String getName()
	{
		return mName;
	}


	public Product setName(String aName)
	{
		mName = aName;
		return this;
	}


	public Price getPrice()
	{
		return mPrice;
	}


	public Product setPrice(Price aPrice)
	{
		mPrice = aPrice;
		return this;
	}


	public Category getCategory()
	{
		return mCategory;
	}


	public Product setCategory(Category aCategory)
	{
		mCategory = aCategory;
		return this;
	}
}
