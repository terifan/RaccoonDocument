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
import java.util.UUID;
import org.terifan.raccoon.document.Array;
import org.terifan.raccoon.document.BinaryEncoder;
import org.terifan.raccoon.document.BinaryDecoder;
import org.terifan.raccoon.document.Document;
import org.terifan.raccoon.document.DocumentWriter;
import org.terifan.raccoon.document.ObjectId;


public class TestBinary
{
	public static void main(String... args)
	{
		try
		{
			testDocument();
//			testWriteToStream();
//			testDocumentWriter();
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	public static void testDocument() throws IOException
	{
//		BinaryEncoder.DEBUG = true;

		Document doc = new Document()
			.put("name", "bob")
			.put("values", Array.of(
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
				new BigInteger("34169131061654613198432165462984651619"),
				new BigDecimal("3416913106165461319.432165462984651619"),
				null,
				"",
				true,
				(byte)17,
				(short)1547,
				(int)17016454,
				(long)2651415726794265464L,
				(float)Math.PI,
				(double)Math.PI,
				(byte)0,
				(short)0,
				(int)0,
				(long)0,
				(float)0,
				(double)0,
				'a',
				new byte[]{1,2,3},
				ObjectId.randomId(),
				Document.of("id:[7,8,9],text:hello"),
				Array.of(7, 8, 9)
			));

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(doc.toByteArray());

		System.out.println("length: " + baos.size());
		_Log.hexDump(baos.toByteArray());
		System.out.println();

		BinaryDecoder in = new BinaryDecoder(new ByteArrayInputStream(baos.toByteArray()));
		while (in.next())
		{
			Object v = in.readObject();
			System.out.printf("%20s = %s%n", v == null ? null : v.getClass().getSimpleName(), v);
		}
	}


	public static void testDocumentWriter() throws IOException
	{
//		BinaryEncoder.DEBUG = true;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (BinaryEncoder out = new BinaryEncoder(baos))
		{
			new DocumentWriter(out)
				.beginDocument()
				.name("name")
				.value("bob")
				.name("values")
				.beginArray()
				.value(346131916191L)
				.value(346131916191L)
				.value("hello")
				.value("hello")
				.value("hello")
				.value(LocalDate.now())
				.value(LocalDateTime.now())
				.value(LocalTime.now())
				.value(OffsetDateTime.now())
				.value(OffsetTime.now())
				.value(ZonedDateTime.now())
				.value(Duration.ofSeconds(4, 656431984))
				.value(UUID.randomUUID())
				.value(new BigInteger("34169131061654613198432165462984651619"))
				.value(new BigDecimal("3416913106165461319.432165462984651619"))
				.nullValue()
				.value("")
				.value(true)
				.value((byte)17)
				.value((short)1547)
				.value((int)17016454)
				.value((long)2651415726794265464L)
				.value((float)Math.PI)
				.value((double)Math.PI)
				.value((byte)0)
				.value((short)0)
				.value((int)0)
				.value((long)0)
				.value((float)0)
				.value((double)0)
				.value('a')
				.value(new byte[]{1,2,3})
				.value(ObjectId.randomId())
				.value(Document.of("id:[7,8,9],text:hello"))
				.value(Array.of(7, 8, 9))
				.endArray()
				.endDocument();
		}

		System.out.println("length: " + baos.size());
		_Log.hexDump(baos.toByteArray());
		System.out.println();

		BinaryDecoder in = new BinaryDecoder(new ByteArrayInputStream(baos.toByteArray()));
		while (in.next())
		{
			Object v = in.readObject();
			System.out.printf("%20s = %s%n", v == null ? null : v.getClass().getSimpleName(), v);
		}
	}


	public static void testWriteToStream() throws IOException
	{
//		BinaryEncoder.DEBUG = true;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (BinaryEncoder out = new BinaryEncoder(baos))
		{
			out.writeObject(Document.of("id:[7,8,9],text:hello"));
			out.writeObject(Array.of(7, 8, 9));
			out.writeObject(346131916191L);
			out.writeObject(346131916191L);
			out.writeObject("hello");
			out.writeObject("hello");
			out.writeObject("hello");
			out.writeObject(LocalDate.now());
			out.writeObject(LocalDateTime.now());
			out.writeObject(LocalTime.now());
			out.writeObject(OffsetDateTime.now());
			out.writeObject(OffsetTime.now());
			out.writeObject(ZonedDateTime.now());
			out.writeObject(Duration.ofSeconds(4, 656431984));
			out.writeObject(UUID.randomUUID());
			out.writeObject(new BigInteger("34169131061654613198432165462984651619"));
			out.writeObject(new BigDecimal("3416913106165461319.432165462984651619"));
			out.writeObject(null);
			out.writeObject("");
			out.writeObject(true);
			out.writeObject((byte)17);
			out.writeObject((short)1547);
			out.writeObject((int)17016454);
			out.writeObject((long)2651415726794265464L);
			out.writeObject((float)Math.PI);
			out.writeObject((double)Math.PI);
			out.writeObject((byte)0);
			out.writeObject((short)0);
			out.writeObject((int)0);
			out.writeObject((long)0);
			out.writeObject((float)0);
			out.writeObject((double)0);
			out.writeObject('a');
			out.writeObject(new byte[]{1,2,3});
			out.writeObject(ObjectId.randomId());
		}

		System.out.println("length: " + baos.size());
		_Log.hexDump(baos.toByteArray());
		System.out.println();

		BinaryDecoder in = new BinaryDecoder(new ByteArrayInputStream(baos.toByteArray()));
		while (in.next())
		{
			Object v = in.readObject();
			System.out.printf("%20s = %s%n", v == null ? null : v.getClass().getSimpleName(), v);
		}
	}
}
