package test_serializer.model;

import java.io.Serializable;
import java.util.ArrayList;


public class Order implements Serializable
{
	private final static long serialVersionUID = 1L;

	private Customer mCustomer;
	private Employee mSalesPerson;
	private ArrayList<Product> mProducts;


	public Order()
	{
		mProducts = new ArrayList<>();
	}


	public Order addProduct(Product aProduct)
	{
		mProducts.add(aProduct);
		return this;
	}


	public Customer getCustomer()
	{
		return mCustomer;
	}


	public Order setCustomer(Customer aCustomer)
	{
		mCustomer = aCustomer;
		return this;
	}


	public Employee getSalesPerson()
	{
		return mSalesPerson;
	}


	public Order setSalesPerson(Employee aSalesPerson)
	{
		mSalesPerson = aSalesPerson;
		return this;
	}


	public ArrayList<Product> getProducts()
	{
		return mProducts;
	}


	public Order setProducts(ArrayList<Product> aProducts)
	{
		mProducts = aProducts;
		return this;
	}
}
