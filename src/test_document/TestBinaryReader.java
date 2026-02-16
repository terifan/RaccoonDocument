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
				.write(0).write(346131916191L)
				.write(1).write("waybill")
				.write(2).write("dwb")
				.write(3).write("2025-02-20")
				.write(4).write(164);

			System.out.println("length: " + baos.size());
			_Log.hexDump(baos.toByteArray(), 24);
			System.out.println();

			try (BinaryDecoder in = new BinaryDecoder(new ByteArrayInputStream(baos.toByteArray())))
			{
				for (Iterator it = in.iterator(); it.hasNext();)
				{
					switch ((Integer)it.next())
					{
						case 0:
							System.out.println("reference: " + it.next());
							break;
						case 1:
							System.out.println("reftype: " + it.next());
							break;
						case 2:
							System.out.println("doctype: " + it.next());
							break;
						case 3:
							System.out.println("created: " + it.next());
							break;
						case 4:
							System.out.println("account: " + it.next());
							break;
					}
				}
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
