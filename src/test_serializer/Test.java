package test_serializer;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Random;
import org.terifan.raccoon.document.Array;
import org.terifan.raccoon.document.Collection;
import org.terifan.raccoon.document.Document;
import org.terifan.raccoon.document.Support;
import test_document._Log;


public class Test
{
	public static void main(String... args)
	{
		try
		{
//			Document doc = _Person.createPerson(new Random(1));

			Document doc = Document.parseJson(new InputStreamReader(new FileInputStream("c:/data/json_testdata/manifest_1.json")));
//			Document doc = Document.parseJson(new InputStreamReader(new FileInputStream("c:/data/json_testdata/manifest_2.json")));
//			Document doc = Document.parseJson(new InputStreamReader(new FileInputStream("c:/data/json_testdata/manifest_3.json")));

//			Document doc = new Document();
//			doc.put("arr1", Array.of("monkey", "dolphine", "dog"));
//			doc.put("arr2", Array.of("monkey", "dog", "dolphine"));
//			doc.put("arr3", Array.of(65464,65465498,651321,6549,3211654));
//			doc.put("arr4", Array.of(65464,65465498,6549,3211654,651321));
//			System.out.println(doc.toJson(false));

			byte[] data = doc.toByteArray();
			System.out.printf("%9s %s%n", "json", doc.toJson().length());
			System.out.printf("%9s %s%n", "json+zip", Support.zip(doc.toJson().getBytes()).length);
			System.out.printf("%9s %s%n", "bin", data.length);

//			_Log.hexDump(data, 24);

//			Collection in = Document.parseByteArray(data);
//			System.out.println(in);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
