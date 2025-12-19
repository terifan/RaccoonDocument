package test_document;

import java.io.FileInputStream;
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
			System.out.printf("%12s %12s %12s %12s %12s %19s %19s %19s%n", "", "json", "bin", "json+zip", "bin+zip", "bin enc/dec", "json enc/dec", "speed");

			Random rnd = new Random(0);

			Array people = new Array();
			for (int i = 0; i < 100; i++)
			{
				people.add(_Person.createPerson(rnd));
			}
			Array tinies = new Array();
			for (int i = 0; i < 100; i++)
			{
				Document p = _Person.createPerson(rnd);
				String name = p.findFirst("personal/givenName");
				tinies.add(Document.of("_id:" + rnd.nextInt(10000) + ",name:" + name + ",age:" + rnd.nextInt(100) + ",phone:'" + rnd.nextInt(100) + "-" + rnd.nextInt(100000000) + "',email:'" + name + "@mail.com'"));
			}

			run("person", _Person.createPerson(new Random(1)));
			run("100 people", people);
			run("tiny", Document.of("_id:3164,name:steve,age:45,phone:'34-35656464',email:'steve@mail.com'"));
			run("100 tiny", tinies);
			run("invoice", Document.parseJson(new InputStreamReader(new FileInputStream("d:/data/json_testdata/invoice.json"))));
			run("manifest-1", Document.parseJson(new InputStreamReader(new FileInputStream("d:/data/json_testdata/manifest_1.json"))));
			run("manifest-2", Document.parseJson(new InputStreamReader(new FileInputStream("d:/data/json_testdata/manifest_2.json"))));
			run("manifest-3", Document.parseJson(new InputStreamReader(new FileInputStream("d:/data/json_testdata/manifest_3.json"))));

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

		try
		{
			for (int i = 0; i < 10; i++)
			{
				Document.parseByteArray(bin);
				Document.parseJson(json);
			}
		}
		catch (Exception e)
		{
//			e.printStackTrace(System.out);
		}

		long et0 = System.currentTimeMillis();
		for (int i = 0; i < 100; i++)
		{
			aCollection.toByteArray();
		}
		long et1 = System.currentTimeMillis();
		for (int i = 0; i < 100; i++)
		{
			aCollection.toJson();
		}
		long et2 = System.currentTimeMillis();

		long dt0 = System.currentTimeMillis();
		for (int i = 0; i < 100; i++)
		{
			Document.parseByteArray(bin);
		}
		long dt1 = System.currentTimeMillis();
		for (int i = 0; i < 100; i++)
		{
			Document.parseJson(json);
		}
		long dt2 = System.currentTimeMillis();

		System.out.printf("%10s : ", aName);
		System.out.printf("%12d ", json.length());
		System.out.printf("%12d ", bin.length);
		System.out.printf("%12d ", zipjson.length);
		System.out.printf("%12d ", binzip.length);

		System.out.printf("%12.3f ", (et1 - et0) / 100.0);
		System.out.printf("%6.3f ", (dt1 - dt0) / 100.0);

		System.out.printf("%12.3f ", (et2 - et1) / 100.0);
		System.out.printf("%6.3f ", (dt2 - dt1) / 100.0);

		System.out.printf("%12.3f ", (et2 - et1) / (double)((et1 - et0)));
		System.out.printf("%6.3f ", (dt2 - dt1) / (double)((dt1 - dt0)));

		System.out.println();
	}
}
