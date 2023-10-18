package org.terifan.raccoon.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import static org.testng.Assert.*;
import org.testng.annotations.Test;


public class StreamMarshallerNGTest
{
	@Test
	public void testMarshallerToByteArrayCompatible() throws IOException
	{
		Document doc = Document.of("message:'hello world',child:{name:bob},numbers:[1,2,3]");

		byte[] buf = doc.toByteArray();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (StreamMarshaller m = new StreamMarshaller(baos))
		{
			m.write(doc);
		}

		assertEquals(buf, baos.toByteArray());

		try (StreamMarshaller m = new StreamMarshaller(new ByteArrayInputStream(buf)))
		{
			Document d = m.read();
			assertEquals(d, doc);
		}
	}


	@Test
	public void testMarshalling() throws IOException
	{
		Document doc = Document.of("message:'hello world',child:{name:bob},numbers:[1,2,3]");
		Array arr = doc.getArray("numbers");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (StreamMarshaller enc = new StreamMarshaller(baos))
		{
			enc.write(4);
			enc.write("test");
			enc.write(arr);
			enc.write(doc);
			enc.writeTerminator();
		}
		baos.write("END".getBytes());

//		_Log.hexDump(baos.toByteArray());

		ByteArrayInputStream in = new ByteArrayInputStream(baos.toByteArray());

		StreamMarshaller dec = new StreamMarshaller(in);
		int i = dec.read();
		String s = dec.read();
		Array a = dec.read();
		Document d = dec.read();
		dec.expectTerminator();

		assertEquals(i, 4);
		assertEquals(s, "test");
		assertEquals(a, arr);
		assertEquals(d, doc);

		byte[] end = new byte[3];
		in.read(end);
		assertEquals(end, "END".getBytes());
	}


	@Test
	public void testMarshalling2() throws IOException
	{
		{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (StreamMarshaller enc = new StreamMarshaller(baos))
		{
			enc.write(3);
			enc.write(2);
			enc.write(1);
			enc.write(0);
			enc.write(32768);
			enc.write(31264);
			enc.write(21679);
			enc.write(Array.of(26161,0,0));
			enc.write(71);
			enc.write(Array.of(0x88888888,0x99999999,0xaaaaaaaa,0xbbbbbbbb));
			enc.write(Array.of(0xcccccccc,0xdddddddd,0xeeeeeeee,0xeeeeeeee));
		}

		System.out.println(baos.size());

		_Log.hexDump(baos.toByteArray());
		}

		{
		Document doc = Document.of("0:3,1:2,2:1,3:0,4:32768,5:31264,6:21679,7:[26161,0,0],8:71,9:[0x88888888,0x99999999,0xaaaaaaaa,0xbbbbbbbb],10:[0xcccccccc,0xdddddddd,0xeeeeeeee,0xeeeeeeee]");

		System.out.println(doc.toByteArray().length);

		_Log.hexDump(doc.toByteArray());
		}

		{
		Document doc = Document.of("a:3,b:2,c:1,d:0,e:32768,f:31264,g:21679,h:[26161,0,0],i:71,j:[0x88888888,0x99999999,0xaaaaaaaa,0xbbbbbbbb],k:[0xcccccccc,0xdddddddd,0xeeeeeeee,0xeeeeeeee]");

		System.out.println(doc.toByteArray().length);

		_Log.hexDump(doc.toByteArray());
		}

		{
		Array doc = Array.of(Array.of(3,2,1,0),32768,31264,21679,Array.of(26161),71,Array.of(0x88888888,0x99999999,0xaaaaaaaa,0xbbbbbbbb),Array.of(0xcccccccc,0xdddddddd,0xeeeeeeee,0xeeeeeeee));

		System.out.println(doc.toByteArray().length);

		_Log.hexDump(doc.toByteArray());

		System.out.println(doc.toTypedJson());
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (DataOutputStream enc = new DataOutputStream(baos))
		{
			enc.write(3);
			enc.write(2);
			enc.write(1);
			enc.write(0);
			enc.writeInt(32768);
			enc.writeInt(31264);
			enc.writeInt(21679);
			enc.writeLong(26161);
			enc.writeLong(0);
			enc.writeLong(0);
			enc.writeLong(71);
			enc.writeInt(0x88888888);
			enc.writeInt(0x99999999);
			enc.writeInt(0xaaaaaaaa);
			enc.writeInt(0xbbbbbbbb);
			enc.writeInt(0xcccccccc);
			enc.writeInt(0xdddddddd);
			enc.writeInt(0xeeeeeeee);
			enc.writeInt(0xeeeeeeee);
		}

		System.out.println(baos.size());

		_Log.hexDump(baos.toByteArray());
	}


	@Test(expectedExceptions = StreamException.class)
	public void testChecksumError() throws IOException
	{
		Document doc = Document.of("message:'hello world',child:{name:bob},numbers:[1,2,3]");
		Array arr = doc.getArray("numbers");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (StreamMarshaller enc = new StreamMarshaller(baos))
		{
			enc.write(4);
			enc.write("test");
			enc.write(arr);
			enc.write(doc);
			enc.writeTerminator();
		}

		byte[] data = baos.toByteArray();
		data[7] ^= 1;

		try (StreamMarshaller dec = new StreamMarshaller(new ByteArrayInputStream(data)))
		{
			int i = dec.read();
			String s = dec.read();
			Array a = dec.read();
			Document d = dec.read();
			dec.expectTerminator();
		}
	}
}
