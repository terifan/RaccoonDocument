package org.terifan.raccoon.document;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;


public class MurmurHash3NGTest
{
	@Test
	public void testMixingInput()
	{
		for (int loop2 = 1; loop2 < 5; loop2++)
		{
			for (int loop1 = 1; loop1 < 20; loop1++)
			{
				MurmurHash3 chk = new MurmurHash3(0);

				byte[] buf = new byte[loop1];

				int i = 0;
				for (int j = 0; j < loop1 % 5; j++)
				{
					chk.updateByte(0xff & buf[i++]);
				}

				for (; i + 3 < buf.length - 7;)
				{
					int d = 0xff & buf[i++];
					int c = 0xff & buf[i++];
					int b = 0xff & buf[i++];
					int a = 0xff & buf[i++];
					chk.updateInt((d << 24) | (c << 16) | (b << 8) | a);
				}

				if (i + loop2 + 5 < buf.length)
				{
					chk.updateBytes(buf, i, loop2 + 5);
					i += loop2 + 5;
				}

				for (; i < buf.length;)
				{
					chk.updateByte(0xff & buf[i++]);
				}

				int hash32 = ___MurmurHash3.hash32(buf, 0, buf.length, 0);

				assertEquals(chk.getValue(), hash32);
			}
		}
	}


	@Test
	public void testUTF8_1()
	{
		MurmurHash3 chk = new MurmurHash3(0);
		chk.updateUTF8("testing лкмн");

		int hash32 = ___MurmurHash3.hash32("testing лкмн", 0);

		assertEquals(chk.getValue(), hash32);
	}


	@Test
	public void testUTF8_2()
	{
		MurmurHash3 chk1 = new MurmurHash3(0);
		MurmurHash3 chk2 = new MurmurHash3(0);
		MurmurHash3 chk3 = new MurmurHash3(0);

		chk1.updateUTF8("testing лкмн\udefa");
		chk2.updateUTF8("testing").updateUTF8(" лкмн\udefa");
		chk3.updateUTF8("testing").updateByte(' ').updateUTF8("лкмн\udefa");

		assertEquals(chk1.getValue(), chk2.getValue());
		assertEquals(chk1.getValue(), chk3.getValue());
	}
}
