package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.InputStream;
import org.terifan.raccoon.document.BinaryDecoder.Visitor;


public class BinaryWalker
{
	private InputStream mInputStream;


	public BinaryWalker(InputStream aInputStream)
	{
		mInputStream = aInputStream;
	}


	public void visit(Visitor aVisitor) throws IOException
	{
		BinaryDecoder decoder = new BinaryDecoder(mInputStream, aVisitor);
		decoder.unmarshal();
	}


	public void visit(String[] aString, Visitor aVisitor)
	{
	}


	public void visit(String[] aString, int aMaxDepth, Visitor aVisitor)
	{
	}
}
