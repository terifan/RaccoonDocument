package test_document;

import java.util.Random;
import org.terifan.raccoon.document.Array;


public class TestBinaryPerson
{
	public static void main(String... args)
	{
		try
		{
			Array out = Array.of(
				_Person.createPerson(new Random(1)),
				_Person.createPerson(new Random(2)),
				_Person.createPerson(new Random(3)),
				_Person.createPerson(new Random(4)),
				_Person.createPerson(new Random(5))
			);

			byte[] data = out.toByteArray();

			_Log.hexDump(data, 32);
			System.out.println();

			Array in = Array.parseByteArray(data);

			System.out.println("length: " + data.length);
			System.out.println("equals: " + in.equals(out));
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
