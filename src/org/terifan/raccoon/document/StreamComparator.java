package org.terifan.raccoon.document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;


public class StreamComparator
{
	public int compare(InputStream aInputStreamA, InputStream aInputStreamB) throws IOException
	{
		Object inA = new BinaryDecoder(aInputStreamA).unmarshal();
		Object inB = new BinaryDecoder(aInputStreamB).unmarshal();

		if (inA instanceof Document)
		{
			Comparable a = (Document)inA;
			Comparable b = (Document)inB;
			return a.compareTo(b);
		}

		return ((Array)inA).compareTo((Array)inB);
	}


	public int compareId(InputStream aInputStreamA, InputStream aInputStreamB) throws IOException
	{
		Object inA = new BinaryDecoder(aInputStreamA).unmarshal();
		Object inB = new BinaryDecoder(aInputStreamB).unmarshal();

		if (inA instanceof Document)
		{
			Comparable a = ((Document)inA).get("_id");
			Comparable b = ((Document)inB).get("_id");
			return a.compareTo(b);
		}

		return ((Array)inA).compareTo((Array)inB);
	}


	public static void main(String ... args)
	{
		try
		{
			Document doc1 = Document.of("_id:1,name:bob,age:37");
			Document doc2 = Document.of("_id:2,name:eve,age:29");
			Document doc3 = Document.of("_id:1,name:eve,age:37");
			byte[] bin1 = doc1.toByteArray();
			byte[] bin2 = doc2.toByteArray();
			byte[] bin3 = doc3.toByteArray();

			System.out.println(new StreamComparator().compare(new ByteArrayInputStream(bin1), new ByteArrayInputStream(bin3)));
			System.out.println(new StreamComparator().compare(new ByteArrayInputStream(bin3), new ByteArrayInputStream(bin1)));

			System.out.println(new StreamComparator().compareId(new ByteArrayInputStream(bin1), new ByteArrayInputStream(bin2)));
			System.out.println(new StreamComparator().compareId(new ByteArrayInputStream(bin2), new ByteArrayInputStream(bin1)));

			Document doc11 = Document.of("_id:[1,bob,[1,2,3]],name:bob,age:37");
			Document doc12 = Document.of("_id:[1,bob,[1,2,4]],name:bob,age:37");
			byte[] bin11 = doc11.toByteArray();
			byte[] bin12 = doc12.toByteArray();

			System.out.println(new StreamComparator().compareId(new ByteArrayInputStream(bin11), new ByteArrayInputStream(bin12)));
			System.out.println(new StreamComparator().compareId(new ByteArrayInputStream(bin12), new ByteArrayInputStream(bin11)));
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
