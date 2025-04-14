package org.terifan.raccoon.document;

import java.nio.ByteBuffer;
import static org.testng.Assert.*;
import org.testng.annotations.Test;


public class CollectionNGTest
{
	@Test
	public void testSomeMethod()
	{
		Collection col1 = Collection.parseJson("[1,2,3]");
		Collection col2 = Collection.parseJson("{a:1,b:true,c:3.14,d:hello}");

		Array col3 = Collection.parseByteArray(col1.toByteArray());
		Document col4 = Collection.parseByteArray(ByteBuffer.wrap(col2.toByteArray()));

		assertEquals(col1.toJson(), "[1,2,3]");
		assertEquals(col2.toJson(), "{\"a\":1,\"b\":true,\"c\":3.14,\"d\":\"hello\"}");
		assertEquals(col3.toJson(), "[1,2,3]");
		assertEquals(col4.toJson(), "{\"a\":1,\"b\":true,\"c\":3.14,\"d\":\"hello\"}");
	}


	@Test
	public void testSomeMethod2()
	{
	}
}
