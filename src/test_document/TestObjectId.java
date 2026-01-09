package test_document;

import static java.lang.Integer.rotateLeft;
import static java.lang.Long.rotateRight;
import org.terifan.raccoon.document.ObjectId;


public class TestObjectId
{
	public static void main(String... args)
	{
		try
		{
			test1();
//			test2();
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	public static void test2()
	{
		ObjectId id = ObjectId.randomId();

		System.out.println(id.toArmouredString(new ObjectId.Key()));
		System.out.println(id.toArmouredString(new ObjectId.Key(0)));
		System.out.println(id.toArmouredString(new ObjectId.Key(0,0)));
		System.out.println(id.toArmouredString(new ObjectId.Key(0,0,0)));
	}


	public static void test1()
	{
		try
		{
			int[] tweak =
			{
				0x0A9F75ED, 0xB0399D91, 0x848542E3, 0x3B5E35EB, 0xC32B695A, 0xE0F51840, 0xBBAA11B9, 0xDBE7792E, 0x7A61E325
			};

			int[] SHIFTS =
			{
				12, 52, 30, 3, 22, 43
			};

			long sum = 0;
			for (long k : new long[]{0})
			{
				for (int s : SHIFTS)
				{
					System.out.println("%016x".formatted(rotateRight(0xff, s)));

					sum ^= rotateRight(k, s);

					for (int i = 0; i < tweak.length; i++)
					{
						sum = (sum * 0x5DEECE66DL + 0xBL) & 0x0000FFFFFFFFFFFFL;
						tweak[i] ^= (int)(sum >>> 16);
					}
				}
			}

			int L1 = 9;
			int L2 = 13;
			int L3 = 5;
			int R1 = 7;
			int R2 = 25;
			int R3 = 10;

			for (int n = 0; n < 20; n++)
			{
				int a = n;
				int b = 0;
				int c = 0;

				a ^= tweak[0];
				b ^= tweak[1];
				c ^= tweak[2];
				for (int i = 0; i < 3; i++)
				{
					a -= b ^ rotateLeft(c, L1);
					b -= c ^ rotateLeft(a, L2);
					c -= a ^ rotateLeft(b, L3);
					a ^= tweak[3 + i];
					a += b ^ rotateLeft(c, R1);
					b += c ^ rotateLeft(a, R2);
					c += a ^ rotateLeft(b, R3);
				}
				a ^= tweak[6];
				b ^= tweak[7];
				c ^= tweak[8];

				System.out.println(String.format("%32s", Integer.toBinaryString(a)).replace(' ', '0') + "\t" + String.format("%32s", Integer.toBinaryString(b)).replace(' ', '0') + "\t" + String.format("%32s", Integer.toBinaryString(c)).replace(' ', '0'));
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
