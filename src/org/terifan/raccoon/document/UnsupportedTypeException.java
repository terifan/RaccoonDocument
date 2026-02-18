package org.terifan.raccoon.document;


public class UnsupportedTypeException extends IllegalArgumentException
{
	private final static long serialVersionUID = 1L;


	public UnsupportedTypeException(String aMessage)
	{
		super(aMessage);
	}
}
