package org.terifan.raccoon.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.UUID;
import static org.testng.Assert.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import test_document._Log;


public class BinaryEncoderNGTest
{
	@Test
	public void testWritingOjectsToObjectOutputStream() throws IOException, ClassNotFoundException
	{
		Document doc = Document.of("id:123,text:'hello world'");
		Array arr = Array.of(1, 2, 3);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream dos = new ObjectOutputStream(baos))
		{
			dos.writeInt(0xcafebabe);
			dos.writeObject(doc);
			dos.writeInt(0xcafebabe);
			dos.writeObject(arr);
			dos.writeInt(0xcafebabe);
		}

		_Log.hexDump(baos.toByteArray());

		ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
		assertEquals(in.readInt(), 0xcafebabe);
		assertEquals(in.readObject(), doc);
		assertEquals(in.readInt(), 0xcafebabe);
		assertEquals(in.readObject(), arr);
		assertEquals(in.readInt(), 0xcafebabe);
	}


	@Test(dataProvider = "allTypes")
	public void testDocument(Array aAllTypes) throws IOException
	{
		Document out = new Document();

		int i = 0;
		for (Object v : aAllTypes)
		{
			out.put("f"+(i++), v);
		}

		Array arr = aAllTypes.clone();
		arr.add(out.clone());
		arr.add(out.clone());
		out.put("f" + i, arr);

		byte[] buffer = out.toByteArray();

//		System.out.println(out.toJson(false));
//		System.out.println("length: " + buffer.length);
//		_Log.hexDump(buffer);
//		System.out.println();

		Collection in = Collection.parseByteArray(buffer);

//		System.out.println(in.toJson(false));
//		System.out.println(in.equals(out));

		assertEquals(in, out);
	}


	@Test(dataProvider = "allTypes")
	public void testWriteObject(Array aAllTypes) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		BinaryEncoder encoder = new BinaryEncoder(baos);
		for (Object v : aAllTypes)
		{
			encoder.writeObject(v);
		}

//		System.out.println("length: " + baos.size());
//		_Log.hexDump(baos.toByteArray());
//		System.out.println();
		int i = 0;
		for (Object v : new BinaryDecoder(new ByteArrayInputStream(baos.toByteArray())))
		{
//			String type = v == null ? null : v.getClass().getSimpleName();
//			if (v instanceof Character)
//			{
//				v = v + " (" + (int)(char)v + ")";
//			}
//			if (v instanceof byte[])
//			{
//				v = Arrays.toString((byte[])v);
//			}
//			System.out.printf("%20s = %s%n", type, v);

			assertEquals(v, aAllTypes.get(i++));
		}
	}


	@DataProvider
	public Object[] allTypes()
	{
		return new Object[]
		{
			Array.of(
			LocalDate.now(),
			LocalDateTime.now(),
			LocalTime.now(),
			OffsetDateTime.now(),
			OffsetTime.now(),
			ZonedDateTime.now(),
			Duration.ofSeconds(4, 656431984),
			UUID.randomUUID(),
			new BigInteger("24169131061654613198432165462984651619"),
			new BigDecimal("2416913106165461319.432165462984651619"),
			null,
			"",
			"hello",
			false,
			true,
			(byte)0,
			(byte)17,
			Byte.MIN_VALUE,
			Byte.MAX_VALUE,
			(short)0,
			(short)1547,
			Short.MIN_VALUE,
			Short.MAX_VALUE,
			(int)0,
			(int)17016454,
			Integer.MIN_VALUE,
			Integer.MAX_VALUE,
			(long)0,
			(long)3651415726794265464L,
			Long.MIN_VALUE,
			Long.MAX_VALUE,
			(float)0,
			(float)Math.PI,
			(double)0,
			(double)Math.PI,
			'a',
			'\udddd',
			new byte[]
			{
				1, 2, 3
			},
			ObjectId.randomId(),
			Document.of("id:[7,8,9],text:hello"),
			Array.of(7, 8, 9)
			)
		};
	}
}
