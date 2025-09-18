package test_document;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.terifan.raccoon.document.Array;
import org.terifan.raccoon.document.Collection;
import org.terifan.raccoon.document.Document;
import org.terifan.raccoon.document.Support;


public class CompareSerialization
{
	public static void main(String... args)
	{
		try
		{
			System.out.printf("%12s %12s %12s %12s %12s %12s %12s %12s %12s%n", "", "json", "bin", "json+zip", "bin+zip", "jsonEnc", "binEnc", "jsonDec", "binDec");

			Random rnd = new Random(0);

			Array persons = new Array();
			for (int i = 0; i < 100; i++)
			{
				persons.add(_Person.createPerson(rnd));
			}
			Array tinies = new Array();
			for (int i = 0; i < 100; i++)
			{
				String name = persons.getDocument(i).getDocument("personal").getArray("contacts").getDocument(0).getString("text");
				tinies.add(Document.of("_id:" + rnd.nextInt(10000) + ",name:'" + name + "',age:" + rnd.nextInt(100) + ",phone:'" + rnd.nextInt(100) + "-" + rnd.nextInt(100000000) + "',email:'" + name + "@mail.com'"));
			}

			run("invoice", Document.parseJson(new InputStreamReader(CompareSerialization.class.getResourceAsStream("invoice.json"))));
			run("manifest", Document.parseJson(new InputStreamReader(CompareSerialization.class.getResourceAsStream("manifest.json"))));
			run("person", _Person.createPerson(rnd));
			run("tiny", Document.of("_id:3164,name:'steve bobs',age:45,phone:'34-35656464',email:'steve.bobs@mail.com'"));
			run("100 tiny", tinies);
			run("trip", Document.parseJson(new InputStreamReader(CompareSerialization.class.getResourceAsStream("trip.json"))));
			run("manifest", Document.parseJson(new InputStreamReader(CompareSerialization.class.getResourceAsStream("test19.json"))));

//			for (int i = 0; i <= 18; i++)
//			{
//				run("test-%02d".formatted(i), Collection.parseJson(new InputStreamReader(CompareSerialization.class.getResourceAsStream("test%02d.json".formatted(i)))));
//			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	protected static void run(String aName, Collection aCollection) throws IOException
	{
		String json = aCollection.toJson();
		byte[] bin = aCollection.toByteArray();

		byte[] binzip = Support.zip(bin);
		byte[] zipjson = Support.zip(json.getBytes(StandardCharsets.UTF_8));

//		for (int i = 0; i < 100; i++)
//		{
//			Document.parseByteArray(bin).toByteArray();
//			Document.parseJson(json).toJson();
//		}
//
//		long t0 = System.currentTimeMillis();
//		for (int i = 0; i < 100; i++)
//		{
//			aCollection.toJson();
//		}
//		long t1 = System.currentTimeMillis();
//		for (int i = 0; i < 100; i++)
//		{
//			aCollection.toByteArray();
//		}
//		long t2 = System.currentTimeMillis();
//		for (int i = 0; i < 100; i++)
//		{
//			Document.parseJson(json);
//		}
//		long t3 = System.currentTimeMillis();
//		for (int i = 0; i < 100; i++)
//		{
//			Document.parseByteArray(bin);
//		}
//		long t4 = System.currentTimeMillis();

		System.out.printf("%10s : ", aName);
		System.out.printf("%12d ", json.length());
		System.out.printf("%12d ", bin.length);
		System.out.printf("%12d ", zipjson.length);
		System.out.printf("%12d ", binzip.length);

//		System.out.printf("%12.3f ", (t1 - t0) / 100.0);
//		System.out.printf("%12.3f ", (t2 - t1) / 100.0);
//		System.out.printf("%12.3f ", (t3 - t2) / 100.0);
//		System.out.printf("%12.3f ", (t4 - t3) / 100.0);

		System.out.println();
	}
}
