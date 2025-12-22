package test_document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;
import org.terifan.raccoon.document.Array;
import org.terifan.raccoon.document.BinaryDecoder;
import org.terifan.raccoon.document.BinaryEncoder;
import org.terifan.raccoon.document.Collection;
import org.terifan.raccoon.document.Document;
import org.terifan.raccoon.document.ObjectId;


public class TestBinary
{
	public static void main(String... args)
	{
		try
		{
			testBinaryInAndOut();

			testDocument();
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	public static void testDocument() throws IOException
	{
		Document out = new Document()
			.put("f0", 346131916191L)
			.put("f1", 346131916191L)
			.put("f2", "hello")
			.put("f3", "hello")
			.put("f4", "hello")
			.put("f5", LocalDate.now())
			.put("f6", LocalDateTime.now())
			.put("f7", LocalTime.now())
			.put("f8", OffsetDateTime.now())
			.put("f9", OffsetTime.now())
			.put("f10", ZonedDateTime.now())
			.put("f11", Duration.ofSeconds(4, 656431984))
			.put("f12", UUID.randomUUID())
			.put("f13", new BigInteger("34169131061654613198432165462984651619"))
			.put("f14", new BigDecimal("3416913106165461319.432165462984651619"))
			.put("f15", null)
			.put("f16", "")
			.put("f17", true)
			.put("f18", (byte)0)
			.put("f19", (byte)17)
			.put("f20", (short)0)
			.put("f21", (short)1547)
			.put("f22", (int)0)
			.put("f23", (int)17016454)
			.put("f24", (long)0)
			.put("f25", (long)2651415726794265464L)
			.put("f26", (float)0)
			.put("f27", (float)Math.PI)
			.put("f28", (double)0)
			.put("f29", (double)Math.PI)
			.put("f30", 'a')
			.put("f31", '\udddd')
			.put("f32", new byte[]
			{
				1, 2, 3
		})
			.put("f33", ObjectId.randomId())
			.put("f34", Document.of("id:[7,8,9],text:hello"))
			.put("f35", Array.of(7, 8, 9))
			.put("f36", Array.of(
				346131916191L,
				346131916191L,
				"hello",
				"hello",
				"hello",
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
				true,
				(byte)0,
				(byte)17,
				(short)0,
				(short)1547,
				(int)0,
				(int)17016454,
				(long)0,
				(long)3651415726794265464L,
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
			)
		;

		byte[] buffer = out.toByteArray();
		System.out.println(out.toJson(false));

		System.out.println("length: " + buffer.length);
		_Log.hexDump(buffer);
		System.out.println();

		Collection in = Collection.parseByteArray(buffer);
		System.out.println(in.toJson(false));

		System.out.println(in.equals(out));
	}


	public static void testBinaryInAndOut() throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		new BinaryEncoder(baos)
			.writeObject(346131916191L)
			.writeObject(346131916191L)
			.writeObject("hello")
			.writeObject("hello")
			.writeObject("hello")
			.writeObject(LocalDate.now())
			.writeObject(LocalDateTime.now())
			.writeObject(LocalTime.now())
			.writeObject(OffsetDateTime.now())
			.writeObject(OffsetTime.now())
			.writeObject(ZonedDateTime.now())
			.writeObject(Duration.ofSeconds(4, 656431984))
			.writeObject(UUID.randomUUID())
			.writeObject(new BigInteger("34169131061654613198432165462984651619"))
			.writeObject(new BigDecimal("3416913106165461319.432165462984651619"))
			.writeObject(null)
			.writeObject("")
			.writeObject(false)
			.writeObject(true)
			.writeObject((byte)0)
			.writeObject((byte)17)
			.writeObject((short)0)
			.writeObject((short)1547)
			.writeObject((int)0)
			.writeObject((int)17016454)
			.writeObject((long)0)
			.writeObject((long)2651415726794265464L)
			.writeObject((float)0)
			.writeObject((float)Math.PI)
			.writeObject((double)0)
			.writeObject((double)Math.PI)
			.writeObject('a')
			.writeObject('\udddd')
			.writeObject(new byte[]
			{
				1, 2, 3
		})
			.writeObject(ObjectId.randomId())
			.writeObject(Document.of("id:[7,8,9],text:hello"))
			.writeObject(Array.of(7, 8, 9));

		System.out.println("length: " + baos.size());
		_Log.hexDump(baos.toByteArray());
		System.out.println();

		for (Object v : new BinaryDecoder(new ByteArrayInputStream(baos.toByteArray())))
		{
			String type = v == null ? null : v.getClass().getSimpleName();
			if (v instanceof Character)
			{
				v = v + " (" + (int)(char)v + ")";
			}
			if (v instanceof byte[])
			{
				v = Arrays.toString((byte[])v);
			}
			System.out.printf("%20s = %s%n", type, v);
		}
	}
}
