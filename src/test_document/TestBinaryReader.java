package test_document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Random;
import org.terifan.raccoon.document.BinaryDecoder;
import org.terifan.raccoon.document.BinaryDecoder.Field;
import org.terifan.raccoon.document.BinaryEncoder;


public class TestBinaryReader
{
	public static void main(String... args)
	{
		try
		{
			byte[] image = new byte[4 * 1024 * 1024];
			new Random(1).nextBytes(image);
			byte[] thumb = new byte[12 * 1024];
			new Random(1).nextBytes(thumb);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			new BinaryEncoder(baos)
				.writeField("id", 346131916191L)
				.writeField("name", "hello")
				.writeField("created", LocalDateTime.now())
				.writeField("image", image)
				.writeField("thumb", thumb)
				.writeField("rating", 5);

//			System.out.println("length: " + baos.size());
//			_Log.hexDump(baos.toByteArray());
//			System.out.println();

			for (Field v : new BinaryDecoder(new ByteArrayInputStream(baos.toByteArray())).fields())
			{
				switch (v.getName())
				{
					case "id": System.out.println(v.getValue()); break;
					case "name": System.out.println(v.getValue()); break;
					case "created": System.out.println(v.getValue()); break;
					case "image": System.out.println(v.getValue()); break;
					case "thumb": System.out.println(v.getValue()); break;
					case "rating": System.out.println(v.getValue()); break;
				}
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
