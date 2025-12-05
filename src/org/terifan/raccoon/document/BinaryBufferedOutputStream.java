package org.terifan.raccoon.document;

import java.io.ByteArrayOutputStream;


public class BinaryBufferedOutputStream extends BinaryOutputStream
{
	public BinaryBufferedOutputStream()
	{
		super(new ByteArrayOutputStream());
	}


	public byte[] finish()
	{
		return ((ByteArrayOutputStream)mOutputStream).toByteArray();
	}
}
