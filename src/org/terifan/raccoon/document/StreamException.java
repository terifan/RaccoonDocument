package org.terifan.raccoon.document;


public class StreamException extends RuntimeException
{
	private final static long serialVersionUID = 1L;


	public StreamException(String aMessage)
	{
		super(aMessage);
	}


	public StreamException(String aMessage, Throwable aCause)
	{
		super(aMessage, aCause);
	}
}
