package org.terifan.raccoon.document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.testng.Assert.*;
import org.testng.annotations.DataProvider;
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


	@Test(dataProvider = "supportedArrayValues")
	public void testAddAndPutSupportedValues(Object aValue)
	{
		Array added = new Array();
		assertSame(added.add(aValue), added);
		assertEquals(added.size(), 1);
		assertArrayValue(added.get(0), aValue);

		Array put = new Array();
		assertSame(put.put(0, aValue), put);
		assertEquals(put.size(), 1);
		assertArrayValue(put.get(0), aValue);
	}


	@DataProvider
	private Object[][] supportedArrayValues()
	{
		return new Object[][]
		{
			{null},
			{"value"},
			{Document.of("nested:true")},
			{Array.of(1, "two")},
			{Integer.MIN_VALUE},
			{true},
			{Double.MAX_VALUE},
			{Long.MIN_VALUE},
			{Float.MAX_VALUE},
			{Byte.MIN_VALUE},
			{Short.MAX_VALUE},
			{'x'},
			{ObjectId.fromString("65dc9ad1b09c81b0e278e2c2")},
			{new byte[]{0, 1, -1}},
			{OffsetDateTime.parse("2024-02-03T04:05:06+02:00")},
			{LocalDateTime.parse("2024-02-03T04:05:06")},
			{LocalDate.parse("2024-02-03")},
			{LocalTime.parse("04:05:06")},
			{new BigDecimal("1234567890.0123456789")},
			{UUID.fromString("d9428888-122b-4c37-a4d8-7e6d1b6f3c21")}
		};
	}


	private void assertArrayValue(Object aActual, Object aExpected)
	{
		if (aExpected instanceof byte[] bytes)
		{
			assertEquals(aActual, bytes);
		}
		else
		{
			assertEquals(aActual, aExpected);
		}
	}


	@Test
	public void testTypedGettersForSupportedValues()
	{
		byte[] binary = {0, 1, -1};
		Array nestedArray = Array.of("nested");
		Document nestedDocument = Document.of("value:1");
		ObjectId objectId = ObjectId.fromString("65dc9ad1b09c81b0e278e2c2");
		UUID uuid = UUID.fromString("d9428888-122b-4c37-a4d8-7e6d1b6f3c21");
		LocalDate date = LocalDate.parse("2024-02-03");
		LocalTime time = LocalTime.parse("04:05:06");
		LocalDateTime dateTime = LocalDateTime.parse("2024-02-03T04:05:06");
		OffsetDateTime offsetDateTime = OffsetDateTime.parse("2024-02-03T04:05:06+02:00");
		BigDecimal decimal = new BigDecimal("123.45");
		Array values = Array.of(
			true,
			(byte)1,
			(short)2,
			3,
			4L,
			5.5f,
			6.5d,
			"text",
			date,
			time,
			dateTime,
			offsetDateTime,
			uuid,
			objectId,
			decimal,
			nestedArray,
			nestedDocument,
			binary,
			Array.of(nestedDocument),
			Array.of(nestedArray),
			(char)'x',
			(Object)null
		);

		assertEquals(values.getBoolean(0), Boolean.TRUE);
		assertEquals(values.getByte(1), Byte.valueOf((byte)1));
		assertEquals(values.getShort(2), Short.valueOf((short)2));
		assertEquals(values.getInt(3), Integer.valueOf(3));
		assertEquals(values.getLong(4), Long.valueOf(4));
		assertEquals(values.getFloat(5), Float.valueOf(5.5f));
		assertEquals(values.getDouble(6), Double.valueOf(6.5d));
		assertEquals(values.getString(7), "text");
		assertEquals(values.getDate(8), date);
		assertEquals(values.getTime(9), time);
		assertEquals(values.getDateTime(10), dateTime);
		assertEquals(values.getOffsetDateTime(11), offsetDateTime);
		assertEquals(values.getUUID(12), uuid);
		assertEquals(values.getObjectId(13), objectId);
		assertSame(values.getNumber(14), decimal);
		assertEquals(values.getDecimal(14), decimal);
		assertSame(values.getArray(15), nestedArray);
		assertSame(values.getDocument(16), nestedDocument);
		assertEquals(values.getBinary(17), binary);
		assertEquals(values.getDocuments(18), new Document[]{nestedDocument});
		assertEquals(values.getArrays(19), new Array[]{nestedArray});
		assertEquals(values.get(20), Character.valueOf('x'));
		assertNull(values.get(21));
		assertTrue(values.isNull(21));
	}


	@Test
	public void testPutStringIndexAndSparseValues()
	{
		Array values = new Array().put("2", "last");

		assertEquals(values.size(), 3);
		assertNull(values.get(0));
		assertNull(values.get(1));
		assertEquals(values.get(2), "last");
		assertTrue(values.isNull(0));
		assertSame(values.get(3, "default"), "default");
	}


	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testAddRejectsUnsupportedType()
	{
		new Array().add(new Object());
	}


	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testPutRejectsUnsupportedType()
	{
		new Array().put(0, new Object());
	}
}
