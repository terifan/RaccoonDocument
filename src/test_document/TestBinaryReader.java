package test_document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import org.terifan.raccoon.document.BinaryDecoder;
import org.terifan.raccoon.document.BinaryEncoder;


public class TestBinaryReader
{
	public static void main(String... args)
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			new BinaryEncoder(baos)
				.writeObject("reference").writeObject(346131916191L)
				.writeObject("reftype").writeObject("waybill")
				.writeObject("doctype").writeObject("dwb")
				.writeObject("created").writeObject("2025-02-20")
				.writeObject("account").writeObject(164);

			System.out.println("length: " + baos.size());
			_Log.hexDump(baos.toByteArray(), 24);
			System.out.println();

			for (Iterator it = new BinaryDecoder(new ByteArrayInputStream(baos.toByteArray())); it.hasNext();)
			{
				switch (it.next().toString())
				{
					case "reference":
						System.out.println(it.next());
						break;
					case "reftype":
						System.out.println(it.next());
						break;
					case "doctype":
						System.out.println(it.next());
						break;
					case "created":
						System.out.println(it.next());
						break;
					case "account":
						System.out.println(it.next());
						break;
				}
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
