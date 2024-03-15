package org.terifan.raccoon.document;

import static org.testng.Assert.*;
import org.testng.annotations.Test;


public class ArrayNGTest
{
	@Test
	public void testToString()
	{
		Array arr = Array.of(1, false, "test");

		assertEquals(arr.toString(), "[1,false,\"test\"]");
	}


	@Test
	public void testConditionalAdd()
	{
		Array arr = new Array();

		arr.addWithCondition(1, x -> x == 1);
		arr.addWithCondition(2, x -> x == 1);

		assertEquals(arr.get(0), (Integer)1);
		assertEquals(arr.size(), 1);
	}


	@Test
	public void testConditionalPut()
	{
		Array arr = new Array();

		arr.putWithCondition(0, 1, x -> x == 1);
		arr.putWithCondition(1, 2, x -> x == 1);

		assertEquals(arr.get(0), (Integer)1);
		assertEquals(arr.size(), 1);
	}
}
