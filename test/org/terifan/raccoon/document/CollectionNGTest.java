package org.terifan.raccoon.document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import static org.testng.Assert.*;
import org.testng.annotations.Test;


public class CollectionNGTest
{
	@Test
	public void testJsonAndBinaryRoundTrip()
	{
		Collection col1 = Collection.parseJson("[1,2,3]");
		Collection col2 = Collection.parseJson("{a:1,b:true,c:3.14,d:hello}");

		Array col3 = Collection.parseByteArray(col1.toByteArray());
		Document col4 = Collection.parseByteArray(col2.toByteArray());

		assertEquals(col1.toJson(), "[1,2,3]");
		assertEquals(col2.toJson(), "{\"a\":1,\"b\":true,\"c\":3.14,\"d\":\"hello\"}");
		assertEquals(col3.toJson(), "[1,2,3]");
		assertEquals(col4.toJson(), "{\"a\":1,\"b\":true,\"c\":3.14,\"d\":\"hello\"}");
	}


	@Test
	public void testDefaultValuesAndComputeIfAbsent()
	{
		Document doc = new Document().put("value", 7).put("nullable", null);
		AtomicInteger calls = new AtomicInteger();

		assertEquals(doc.get("missing", "fallback"), "fallback");
		assertEquals(doc.get("nullable", "fallback"), "fallback");
		assertEquals(doc.get("missing", key -> "default:" + key), "default:missing");
		assertEquals(doc.get("missing", () -> "supplier"), "supplier");
		assertEquals(doc.computeIfAbsent("value", key ->
		{
			calls.incrementAndGet();
			return 8;
		}), (Integer)7);
		assertEquals(doc.computeIfAbsent("created", key ->
		{
			calls.incrementAndGet();
			return "new value";
		}), "new value");

		assertEquals(calls.get(), 1);
		assertEquals(doc.get("created"), "new value");
	}


	@Test
	public void testTypedConversionsAndNullValues()
	{
		UUID uuid = UUID.fromString("d9428888-122b-4c37-a4d8-7e6d1b6f3c21");
		ObjectId objectId = ObjectId.fromString("65dc9ad1b09c81b0e278e2c2");
		Document values = new Document()
			.put("boolean", "true")
			.put("number", 42L)
			.put("text", 123)
			.put("date", "2024-02-03")
			.put("time", "04:05:06")
			.put("dateTime", "2024-02-03T04:05:06")
			.put("offsetDateTime", "2024-02-03T04:05:06+02:00")
			.put("uuid", uuid.toString())
			.put("objectId", objectId.toString())
			.put("decimal", "123.45")
			.put("binary", "AQID")
			.put("nullable", null);

		assertEquals(values.getBoolean("boolean"), Boolean.TRUE);
		assertEquals(values.getByte("number"), Byte.valueOf((byte)42));
		assertEquals(values.getShort("number"), Short.valueOf((short)42));
		assertEquals(values.getInt("number"), Integer.valueOf(42));
		assertEquals(values.getLong("number"), Long.valueOf(42));
		assertEquals(values.getFloat("number"), Float.valueOf(42));
		assertEquals(values.getDouble("number"), Double.valueOf(42));
		assertEquals(values.getString("text"), "123");
		assertEquals(values.getDate("date"), LocalDate.of(2024, 2, 3));
		assertEquals(values.getTime("time"), LocalTime.of(4, 5, 6));
		assertEquals(values.getDateTime("dateTime"), LocalDateTime.of(2024, 2, 3, 4, 5, 6));
		assertEquals(values.getOffsetDateTime("offsetDateTime"), OffsetDateTime.parse("2024-02-03T04:05:06+02:00"));
		assertEquals(values.getUUID("uuid"), uuid);
		assertEquals(values.getObjectId("objectId"), objectId);
		assertEquals(values.getDecimal("decimal"), new BigDecimal("123.45"));
		assertEquals(values.getBinary("binary"), new byte[]{1, 2, 3});
		assertNull(values.getString("nullable"));
		assertTrue(values.isNull("nullable"));
		assertTrue(values.isNull("missing"));
	}
}
