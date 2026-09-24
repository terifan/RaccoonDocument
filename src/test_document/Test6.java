package test_document;

import java.nio.file.Files;
import java.nio.file.Paths;
import org.terifan.raccoon.document.Array;
import org.terifan.raccoon.document.Collection;
import org.terifan.raccoon.document.Document;


public class Test6
{
	public static void main(String... args)
	{
		try
		{
//			Console.enabled = true;

			Document doc = Document.of(new String(Files.readAllBytes(Paths.get("d:\\data\\json_testdata\\manifest_5.json"))));

			System.out.println(doc.distinct("manifest/manifestItems[type=shipsEquipment]/equipmentType/identifier"));

			for (String name : doc.distinct("manifest/manifestItems[type=shipsEquipment]/equipmentType/identifier").map(String::toLowerCase).keys().sort().iterable(String.class))
			{
				System.out.println(name);

				Array docs = doc.findMany("manifest/manifestItems[type=shipsEquipment && equipmentType/identifier='" + name + "']");

				System.out.println(docs.sum("dimensions/outer/length/amount"));
			}

		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	public static void xmain(String... args)
	{
		try
		{
//			Console.enabled = true;

			Document col = Document.of("name:test,order:[7,name:apa,{orderLine:[{name:A},{name:B,hazard:true}]},{orderLine:[{name:C,hazard:true},{name:D,orderLine:[{name:E,hazard:true}]}]}],data:[{orderLine:[{name:F},{name:G,hazard:true}]}]");

			System.out.println(col.findMany("//name"));
			System.out.println(col.findMany("order/*"));
			System.out.println(col.findMany("//orderLine[hazard=true]/name"));
			System.out.println(col.findMany("order//orderLine[hazard=true]/name"));
			System.out.println(col.findMany("data//orderLine[hazard=true]/name"));
			System.out.println(col.getArray("order").findMany("//orderLine[hazard=true]/name"));
			System.out.println(col.getArray("data").findMany("//orderLine[hazard=true]/name"));
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}
	}
}
