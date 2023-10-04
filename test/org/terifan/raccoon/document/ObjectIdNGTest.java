package org.terifan.raccoon.document;

import org.terifan.raccoon.document.ObjectId.Key;
import static org.testng.Assert.*;
import org.testng.annotations.Test;


public class ObjectIdNGTest
{
	@Test
	public void testFalsePositives()
	{
		char[] BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
		Key key = new Key(new byte[32]);

		int cnt = 0;
		java.util.Random rnd = new java.util.Random(1);
		char[] name = new char[18];
		for (int i = 0; i < 10_000_000; i++)
		{
			for (int j = 0; j < 18; j++)
			{
				name[j] = BASE62[rnd.nextInt(62)];
			}
			ObjectId oid = ObjectId.fromArmouredString(key, new String(name));
			if (oid != null)
			{
				cnt++;
			}
		}
//		System.out.println(cnt);
//		assertTrue(cnt < 5000);
		assertEquals(cnt, 4295);
	}
}
