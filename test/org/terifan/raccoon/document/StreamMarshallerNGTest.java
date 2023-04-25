package org.terifan.raccoon.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
