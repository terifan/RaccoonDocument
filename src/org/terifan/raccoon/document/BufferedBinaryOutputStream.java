package org.terifan.raccoon.document;

import java.io.ByteArrayOutputStream;


public class BufferedBinaryOutputStream extends BinaryOutputStream
{
	public BufferedBinaryOutputStream()
	{
		super(new ByteArrayOutputStream());
	}


	public byte[] finish()
	{
		return ((ByteArrayOutputStream)mOutputStream).toByteArray();
	}
}
