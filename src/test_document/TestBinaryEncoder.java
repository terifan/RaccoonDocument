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
import java.util.Date;
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
				.write(null)
				.write(true)
				.write((byte)17)
				.write((short)1547)
				.write((int)17016454)
				.write((long)2651415726794265464L)
				.write((float)Math.PI)
				.write((double)Math.PI)
				.write("hello")
				.write('a')
				.write('\udddd')
				.write(new Date())
				.write(LocalDate.now())
				.write(LocalDateTime.now())
				.write(LocalTime.now())
				.write(OffsetDateTime.now())
				.write(OffsetTime.now())
				.write(ZonedDateTime.now())
				.write(Duration.ofSeconds(4, 656431984))
				.write(UUID.randomUUID())
				.write(new BigInteger("34169131061654613198432165462984651619"))
				.write(new BigDecimal("3416913106165461319.432165462984651619"))
				.write(new byte[]
				{
					1, 2, 3
			})
				.write(ObjectId.randomId())
				.write(Document.of("id:[7,8,9],text:hello"))
				.write(Array.of(7, 8, 9));

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
