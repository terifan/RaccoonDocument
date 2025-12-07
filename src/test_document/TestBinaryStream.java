package test_document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.ZoneOffset;
import java.util.Date;
import org.terifan.raccoon.document.BinaryEncoder;
import org.terifan.raccoon.document.BinaryDecoder;
import org.terifan.raccoon.document.Document;


public class TestBinaryStream
{
	public static void main(String ... args)
	{
		try
		{
//			BinaryEncoder.DEBUG = true;

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			BinaryEncoder out = new BinaryEncoder(baos);

			out.writeObject(Document.of("id:7,name:bob"));
			out.writeObject(13);
			out.writeObject(346131916191L);
			out.writeObject(346131916191L);
			out.writeObject((short)3);
			out.writeObject("test");
			out.writeObject("test");
			out.writeObject("test");
			out.writeObject(new Date().toInstant().atOffset(ZoneOffset.UTC));
			out.writeObject(Math.PI);

			System.out.println("length: " + baos.size());
			_Log.hexDump(baos.toByteArray());
			System.out.println();

			BinaryDecoder in = new BinaryDecoder(new ByteArrayInputStream(baos.toByteArray()));
			while (in.next())
			{
				Object v = in.readObject();
				System.out.printf("%20s = %s%n", v==null?null:v.getClass().getSimpleName(), v);
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
