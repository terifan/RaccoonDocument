package org.terifan.raccoon.document;

import java.io.IOException;
import static org.testng.Assert.*;
import org.testng.annotations.Test;


public class MarshallableNGTest
{
	@Test
	public void testSomeMethodB() throws IOException
	{
		int a = 77;
		String b = "str";

		MyObject obj = new MyObject().setA(a).setB(b);

		byte[] data = obj.marshalByteArray();

		MyObject newobj = new MyObject();
		newobj.unmarshalFromByteArray(data);

		assertEquals(newobj.a, a);
		assertEquals(newobj.b, b);
	}


	private static class MyObject implements Marshallable
	{
		int a;
		String b;
		@Override
		public Document marshal()
		{
			return new Document().put("a", a).put("b", b);
		}
		@Override
		public void unmarshal(DocumentEntity aDocumentEntity)
		{
			Document doc = (Document)aDocumentEntity;
			a = doc.get("a");
			b = doc.get("b");
		}
		public MyObject setA(int aA)
		{
			this.a = aA;
			return this;
		}
		public MyObject setB(String aB)
		{
			this.b = aB;
			return this;
		}
	}
}
