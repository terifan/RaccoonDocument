package org.terifan.raccoon.document;

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

		_List<_Color> colors = new _List<>();
		colors.add(new _Color(50, 100, 200));
		colors.add(new _Color(50, 200, 100));
		colors.add(new _Color(100, 200, 50));

		System.out.println(ObjectMarshaller.toDocument(colors));

		System.out.println(ObjectMarshaller.toDocument(new int[]{1,2,3}));

		System.out.println(ObjectMarshaller.toDocument(new _Text("hello world")));
	}
}
