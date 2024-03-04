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
}
