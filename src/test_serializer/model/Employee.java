package test_serializer.model;

import java.io.Serializable;


public class Employee extends Person<Employee> implements Serializable
{
	private final static long serialVersionUID = 1L;

	private long mEmployeeNumber;


	public Employee()
	{
	}


	public long getEmployeeNumber()
	{
		return mEmployeeNumber;
	}


	public Employee setEmployeeNumber(long aEmployeeNumber)
	{
		mEmployeeNumber = aEmployeeNumber;
		return this;
	}
}
