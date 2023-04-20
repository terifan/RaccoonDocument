package org.terifan.raccoon.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import static org.testng.Assert.*;
import org.testng.annotations.Test;


public class MarshallerNGTest
{
	@Test
	public void testSomeMethod() throws IOException
	{
		Array arr = Array.of(1,2,3);
		Document doc = new Document()
			.put("test", "hello world")
			.put("arr", arr);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (Marshaller enc = new Marshaller(baos))
		{
			enc.write(4);
			enc.write("test");
			enc.write(arr);
			enc.write(doc);
			enc.writeTerminator();
		}
		baos.write("END".getBytes());

		ByteArrayInputStream in = new ByteArrayInputStream(baos.toByteArray());

		Marshaller dec = new Marshaller(new ByteArrayInputStream(baos.toByteArray()));
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
}
