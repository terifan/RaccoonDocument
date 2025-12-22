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
import java.util.Arrays;
import java.util.UUID;
import org.terifan.raccoon.document.Array;
import org.terifan.raccoon.document.BinaryDecoder;
import org.terifan.raccoon.document.BinaryEncoder;
import org.terifan.raccoon.document.Document;
import org.terifan.raccoon.document.ObjectId;


public class TestBinaryEncoder
{
	public static void main(String... args)
	{
		try
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
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
