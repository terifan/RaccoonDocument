package org.terifan.raccoon.document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.terifan.raccoon.document.BinaryIterator.Value;


public class StreamComparator
{
	public int compare(InputStream aInputStreamA, InputStream aInputStreamB) throws IOException
	{
		Object inA = new BinaryDecoder(aInputStreamA).unmarshal();
		Object inB = new BinaryDecoder(aInputStreamB).unmarshal();

		if (inA instanceof Document v1 && inB instanceof Document v2)
		{
			return v1.compareTo(v2);
		}
		if (inA instanceof Array v1 && inB instanceof Array v2)
		{
			return v1.compareTo(v2);
		}

		return ((Array)inA).compareTo((Array)inB);
	}


	public int compareId(InputStream aInputStreamA, InputStream aInputStreamB) throws IOException
	{
//		Object inA = new BinaryDecoder(aInputStreamA).unmarshal();
//		Object inB = new BinaryDecoder(aInputStreamB).unmarshal();
//
//		if (inA instanceof Document)
//		{
//			Comparable a = ((Document)inA).get("_id");
//			Comparable b = ((Document)inB).get("_id");
//			return a.compareTo(b);
//		}
//
//		return ((Array)inA).compareTo((Array)inB);

		BinaryIterator it = new BinaryIterator(aInputStreamA);

		while (it.hasNext())
		{
			Value v = it.next();

			System.out.println(v);
		}

		return 0;
	}


	public static void main(String... args)
	{
		try
		{
			Document doc1 = Document.of("_id:[1,bob]");
			Document doc2 = Document.of("_id:[1,bob,[1,2,3]],name:bob,age:38");
			byte[] bin1 = doc1.toByteArray();
			byte[] bin2 = doc2.toByteArray();

			new StreamComparator().compareId(new ByteArrayInputStream(bin1), new ByteArrayInputStream(bin2));

//			Document doc1 = Document.of("_id:1,name:bob,age:37");
//			Document doc2 = Document.of("_id:2,name:eve,age:29");
//			Document doc3 = Document.of("_id:1,name:eve,age:37");
//			byte[] bin1 = doc1.toByteArray();
//			byte[] bin2 = doc2.toByteArray();
//			byte[] bin3 = doc3.toByteArray();
//
////			System.out.println(new StreamComparator().compare(new ByteArrayInputStream(bin1), new ByteArrayInputStream(bin3)));
////			System.out.println(new StreamComparator().compare(new ByteArrayInputStream(bin3), new ByteArrayInputStream(bin1)));
//
//			new StreamComparator().compareId(new ByteArrayInputStream(bin1), new ByteArrayInputStream(bin2));
//			System.out.println(new StreamComparator().compareId(new ByteArrayInputStream(bin1), new ByteArrayInputStream(bin2)));
//			System.out.println(new StreamComparator().compareId(new ByteArrayInputStream(bin2), new ByteArrayInputStream(bin1)));
//
//			Document doc11 = Document.of("_id:[1,bob,[1,2,3]],name:bob,age:37");
//			Document doc12 = Document.of("_id:[1,bob,[1,2,4]],name:bob,age:37");
//			byte[] bin11 = doc11.toByteArray();
//			byte[] bin12 = doc12.toByteArray();
//
//			System.out.println(new StreamComparator().compareId(new ByteArrayInputStream(bin11), new ByteArrayInputStream(bin12)));
//			System.out.println(new StreamComparator().compareId(new ByteArrayInputStream(bin12), new ByteArrayInputStream(bin11)));
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
