package org.terifan.raccoon.document;

import java.util.ArrayList;
import org.testng.annotations.Test;


public class ObjectMarshallerNGTest
{
	@Test
	public void testX()
	{
		_BaseColor basecolor = new _BaseColor(255,100,50);

		System.out.println(ObjectMarshaller.toDocument(basecolor));

		_Color color = new _Color(255,100,50);

		System.out.println(ObjectMarshaller.toDocument(color));

		ArrayList<_Color> colors = new ArrayList<>();
		colors.add(new _Color(50, 100, 200));
		colors.add(new _Color(50, 200, 100));
		colors.add(new _Color(100, 200, 50));

		System.out.println(ObjectMarshaller.toDocument(colors));

//		_Log.hexDump(data);
	}
}
