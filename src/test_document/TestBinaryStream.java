package test_document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import org.terifan.raccoon.document.ObjectId;


public class TestBinaryStream
{
	public static void main(String... args)
	{
		try
		{
//			BinaryEncoder.DEBUG = true;

			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			try (BinaryEncoder out = new BinaryEncoder(baos))
			{
				out.writeObject(Document.of("id:[7,8,9],text:hello").put("x", new BigInteger("34169131061654613198432165462984651619")));
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
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
