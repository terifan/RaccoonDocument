package org.terifan.raccoon.document;

import java.util.Arrays;
import java.util.HashMap;
import org.terifan.raccoon.document.ObjectId.Key;
import static org.testng.Assert.*;
import org.testng.annotations.Test;


public class ObjectIdNGTest
{
	private static char[] BASE62ENC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
	final static int[] BASE62DEC = new int[128];


	static
	{
		for (int i = 0; i < 62; i++)
		{
			BASE62DEC[BASE62ENC[i]] = i;
		}
	}


	@Test
	public void testSimple()
	{
		Key key = new Key(123);

		ObjectId id1 = ObjectId.fromParts(1, 2, 3);
//		ObjectId id1 = ObjectId.randomId();
		ObjectId id2 = ObjectId.fromParts(id1.timestamp(), id1.session(), id1.sequence());
		ObjectId id3 = ObjectId.fromBase62String(id1.toBase62String());
		ObjectId id4 = ObjectId.fromArmouredString(key, id1.toArmouredString(key));
		ObjectId id5 = ObjectId.fromString(id1.toString());
		ObjectId id6 = ObjectId.fromByteArray(id1.toByteArray());

//		ObjectId id7 = ObjectId.fromBase62String(id1.toArmouredString(key));
//		System.out.println(id7);

		assertEquals(id1, id2);
		assertEquals(id1, id3);
		assertEquals(id1, id4);
		assertEquals(id1, id5);
		assertEquals(id1, id6);

		System.out.println(id1.toString());
		System.out.println(id1.toBase62String());
		System.out.println(id1.toArmouredString(key));
	}


	@Test
	public void testHashCode1()
	{
		ObjectId id = ObjectId.fromParts(1, 2, 3);
		assertEquals(id.hashCode(), Arrays.asList(1, 2, 3).hashCode());
	}


	@Test
	public void testHashCode2()
	{
		ObjectId id = ObjectId.randomId();
		assertEquals(id.hashCode(), Arrays.asList(id.timestamp(), id.session(), id.sequence()).hashCode());
	}


	@Test
	public void testPerfCreateKey()
	{
		// approx 1s runtime
		for (int i = 0; i < 250_000; i++)
		{
			Key key = new Key(i);
		}
	}


	@Test
	public void testPerfToArmouredString()
	{
		// approx 1s runtime
		Key key = new Key(0);
		ObjectId id = ObjectId.randomId();
		for (int i = 0; i < 25_000_000; i++)
		{
			id.toArmouredString(key);
		}
	}


	@Test
	public void testPerfFromArmouredString()
	{
		// approx 1s runtime
		Key key = new Key(0);
		ObjectId id = ObjectId.randomId();
		String s = id.toArmouredString(key);
		for (int i = 0; i < 40_000_000; i++)
		{
			ObjectId.fromArmouredString(key, s);
		}
	}


	@Test
	public void testPerfToBase62String()
	{
		// approx 1s runtime
		ObjectId id = ObjectId.randomId();
		for (int i = 0; i < 40_000_000; i++)
		{
			id.toBase62String();
		}
	}


	@Test
	public void testPerfFromBase62String()
	{
		// approx 1s runtime
		ObjectId id = ObjectId.randomId();
		String s = id.toBase62String();
		for (int i = 0; i < 65_000_000; i++)
		{
			ObjectId.fromBase62String(s);
		}
	}


//	@Test
//	public void testFalsePositivesName()
//	{
//		java.util.Random rnd = new java.util.Random(1);
//		Key key = new Key(0);
//		char[] buf = new char[18];
//		int cnt = 0;
//		for (int i = 0; i < 10_000_000; i++)
//		{
//			for (int j = 0; j < 18; j++)
//			{
//				buf[j] = BASE62ENC[rnd.nextInt(62)];
//			}
//			String name = new String(buf);
//
//			ObjectId oid = ObjectId.fromArmouredString(key, name);
//			if (oid != null)
//			{
//				cnt++;
//			}
//		}
//		assertEquals(cnt, 4332); // 10_000_000 / 13 / 13 / 13
//	}


//	@Test
//	public void testFalsePositivesKey()
//	{
//		java.util.Random rnd = new java.util.Random(1);
//		char[] buf = new char[18];
//		for (int j = 0; j < 18; j++)
//		{
//			buf[j] = BASE62ENC[rnd.nextInt(62)];
//		}
//		String name = new String(buf);
//		int cnt = 0;
//		for (int j = 0; j < 256; j++)
//		{
//			for (int i = 0; i < 256; i++)
//			{
//				Key key = new Key(i, j);
//				ObjectId oid = ObjectId.fromArmouredString(key, name);
//				if (oid != null)
//				{
//					cnt++;
//				}
//			}
//		}
//		assertEquals(cnt, 30); // 65536 / 13 / 13 / 13
//	}


	@Test
	public void testCompare()
	{
		ObjectId A = ObjectId.randomId();
		ObjectId B = ObjectId.randomId();

		byte[] a = A.toByteArray();
		byte[] b = B.toByteArray();

		assertEquals(A.compareTo(B), ObjectId.compare(a, 0, b, 0));
	}


	@Test
	public void testBase62String()
	{
		ObjectId id = ObjectId.fromParts(1, 2, 3);

		String text = id.toBase62String();
		ObjectId restored = ObjectId.fromBase62String(text);

		assertEquals(text, "00000K00000U00000d");
		assertEquals(id.toString(), restored.toString());
	}


	@Test
	public void testArmouredString()
	{
		for (int i = -1; i < 3; i++)
		{
			long k = i == -1 ? 0 : 1L << i;

			Key key = new Key(k);
			for (int j = 0; j < 8; j++)
			{
				ObjectId id = ObjectId.fromParts(0, 0, j);
				Object text = id.toArmouredString(key);
				System.out.print(text + " ");
			}
			System.out.println();
		}

		System.out.println(" ");

		for (int i = 0; i < 4; i++)
		{
			for (int j = 0; j < 8; j++)
			{
				ObjectId id = ObjectId.fromParts(0, i, j);
				Object text = id.toBase62String();
				System.out.print(text + " ");
			}
			System.out.println();
		}
	}


	@Test
	public void testHashCodeCollision()
	{
		HashMap<Integer, Integer> coll = new HashMap<>();
		for (int k = 0; k < 10_000_000; k++)
		{
			int h = ObjectId.randomId().hashCode();
			coll.merge(h, 1, Integer::sum);
		}

		HashMap<Integer, Integer> coll2 = new HashMap<>();
		coll.forEach((k, v) ->
		{
			if (v > 1)
			{
				coll2.merge(v, 1, Integer::sum);
			}
		});

		System.out.println("Collisions: " + coll2);
	}


	@Test
	public void testBase62Simple()
	{
		String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		String text = "00000K00000U00000d";
		int[] p = new int[3];
		int[] c = new int[3];
		for (int i = 0; i < 3; i++)
		{
			long v = 0;
			for (int j = 0; j < 6; j++)
			{
				v *= 62;
				v += BASE62.indexOf(text.charAt(i * 6 + j));
			}
			c[i] = (int)(v % 13);
			p[i] = (int)(v / 13);
		}
		int t = (((31 + p[0]) * 31) + p[1]) * 31 + p[2];
		if (((t) % 13) != c[0] || ((t / 13) % 13) != c[1] || ((t / 13 / 13) % 13) != c[2])
		{
			throw new IllegalStateException("checksum error");
		}
		System.out.println(p[0] + "," + p[1] + "," + p[2]);
	}
}
