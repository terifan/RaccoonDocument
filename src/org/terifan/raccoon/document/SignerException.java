package org.terifan.raccoon.document;


public class SignerException extends RuntimeException
{
	private final static long serialVersionUID = 1L;


	public SignerException(String aMessage)
	{
		super(aMessage);
	}


	public SignerException(String aMessage, Throwable aCause)
	{
		super(aMessage, aCause);
	}
}
