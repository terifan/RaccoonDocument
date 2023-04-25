package org.terifan.raccoon.document;

import static org.testng.Assert.*;
import org.testng.annotations.Test;


public class ObjectMarshallerNGTest
{
	@Test
	public void testX()
	{
		_BaseColor basecolor = new _BaseColor(255,100,50);

		Document basedoc = ObjectMarshaller.toDocument(basecolor);

		System.out.println(basedoc);

		_Color color = new _Color(255,100,50);

		Document doc = ObjectMarshaller.toDocument(color);

		System.out.println(doc);

//		_Log.hexDump(data);
	}
}
