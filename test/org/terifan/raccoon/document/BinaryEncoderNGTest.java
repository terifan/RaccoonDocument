package org.terifan.raccoon.document;

import java.io.IOException;
import static org.testng.Assert.*;
import org.testng.annotations.Test;


public class BinaryEncoderNGTest
{
	@Test
	public void testReference() throws IOException
	{
		Document a1 = Document.of("number:'47314631'");
		Document a2 = Document.of("number:'47314631'");
		Document a3 = Document.of("number:'47314631'");
		Document a4 = Document.of("number:'47314631'");
		Document a5 = Document.of("number:'47314631'");

		Document out = Document.of("text:'hello world'");
		out.put("alpha", a1);
		out.put("beta", a2);
		out.put("gamma", a3);
		out.put("omega", a4);
		out.put("zeta", a5);

		out.reduce();

//		System.out.println(out);

		Document in = new Document().fromByteArray(out.toByteArray());

		assertEquals(in.hashCode(), out.hashCode());
		assertEquals(in, out);
	}
}
