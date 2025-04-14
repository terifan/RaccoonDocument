package test_document;

import org.terifan.raccoon.document.Array;
import org.terifan.raccoon.document.Document;


public class Test5
{
	public static void main(String... args)
	{
		try
		{
			Array arr = Array.of(Document.of("a:1"), Document.of("a:2"), Document.of("a:3"));

			arr.stream(Document.class).forEach(doc -> System.out.println(doc.size()));

			for (Document doc : arr.asArrayOf(Document.class))
			{
				System.out.println(doc);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}
	}
}
