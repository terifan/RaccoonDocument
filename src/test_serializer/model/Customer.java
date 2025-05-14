package test_serializer.model;

import java.io.Serializable;
import java.util.ArrayList;
import org.terifan.raccoon.document.Document;


public class Customer extends Person<Customer> implements Serializable
{
	private long mCustomerNumber;
	private double mDiscountPercent;
	private Address mInvoiceAddress;
	private Address mDeliveryAddress;
	private ArrayList<Order> mOrders;


	public Customer()
	{
		mOrders = new ArrayList<>();
	}


	public static class Address implements Serializable
	{
		private final static long serialVersionUID = 1L;
		public Document mAddress;


		public Address(Document aAddress)
		{
			mAddress = aAddress;
		}
	}


	public long getCustomerNumber()
	{
		return mCustomerNumber;
	}


	public Customer setCustomerNumber(long aCustomerNumber)
	{
		mCustomerNumber = aCustomerNumber;
		return this;
	}


	public Address getInvoiceAddress()
	{
		return mInvoiceAddress;
	}


	public Customer setInvoiceAddress(Address aInvoiceAddress)
	{
		mInvoiceAddress = aInvoiceAddress;
		return this;
	}


	public Address getDeliveryAddress()
	{
		return mDeliveryAddress;
	}


	public Customer setDeliveryAddress(Address aDeliveryAddress)
	{
		mDeliveryAddress = aDeliveryAddress;
		return this;
	}


	public double getDiscountPercent()
	{
		return mDiscountPercent;
	}


	public Customer setDiscountPercent(double aDiscountPercent)
	{
		mDiscountPercent = aDiscountPercent;
		return this;
	}


	public ArrayList<Order> getOrders()
	{
		return mOrders;
	}


	public Customer setOrders(ArrayList<Order> aOrders)
	{
		mOrders = aOrders;
		return this;
	}
}
