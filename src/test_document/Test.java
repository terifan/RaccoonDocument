package test_document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;
import org.terifan.raccoon.document.Array;
import org.terifan.raccoon.document.BinaryDecoder.Visitor;
import org.terifan.raccoon.document.BinaryDecoder.VisitorResult;
import org.terifan.raccoon.document.BinaryWalker;
import org.terifan.raccoon.document.Collection;
import org.terifan.raccoon.document.Document;
import org.terifan.raccoon.document.Path;


public class Test
{
	public static void main(String... args)
	{
		try
		{
//			Document doc = Document.parseJson(new InputStreamReader(Test.class.getResourceAsStream("manifest.json")));
//
//			System.out.printf("%10s %10s %10s %10s %10s %10s %10s%n", "js", "bi", "bizip", "bidi", "bidizip", "jszip", "di");
//
//			System.out.printf("%10d ", doc.toJson().length());
//			System.out.printf("%10d ", doc.toByteArray().length);
//
//			byte[] data = Support.zip(doc.toByteArray());
//			System.out.printf("%10d ", data.length);
//
//			Dictionary dic = Dictionary.of(doc);
//			System.out.printf("%10d ", dic.toByteArray(doc).length);
//			System.out.printf("%10d ", Support.zip(dic.toByteArray(doc)).length);
//
//			byte[] data2 = Support.zip(doc.toJson().getBytes(StandardCharsets.UTF_8));
//			System.out.printf("%10d ", data2.length);
//			System.out.printf("%10d ", dic.writeExternal().length);
//			System.out.println();

//			a();
//			b();
//			c();
//			d();
//			e();
			f();
//			ObjectId id = ObjectId.fromParts(1, 2, 3);
//
//			ObjectId.Key key = new ObjectId.Key(123);
//			String bs = id.toBase62String();
//			String as = id.toArmouredString(key);
//
//			System.out.println(id);
//			System.out.println(bs);
//			System.out.println(as);
//
//			System.out.println(ObjectId.fromBase62String(bs));
//			System.out.println(ObjectId.fromArmouredString(key, as));
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	public static void c(String... args)
	{
		try
		{
			Document doc = _Person.createPerson(new Random(1));

			byte[] data = doc.toByteArray();

			BinaryWalker walker = new BinaryWalker(new ByteArrayInputStream(data));

			walker.visit(new Visitor()
			{
				@Override
				public VisitorResult preVisit(Path aPath)
				{
//					System.out.println(aPath);
//					if (aPath.matches("work", "contacts"))
//					{
//						return VisitorResult.SKIP;
//					}
//					if (aPath.matches("locationHistory"))
//					{
//						return VisitorResult.SKIP;
//					}
//					if (aPath.matches("personal"))
//					{
//						return VisitorResult.SKIP;
//					}
					return VisitorResult.CONTINUE;
				}


				@Override
				public VisitorResult postVisit(Path aPath, Object aValue)
				{
					System.out.println(aPath + " " + aValue);
					if (aPath.matches("version"))
					{
						return VisitorResult.TERMINATE;
					}
					return VisitorResult.CONTINUE;
				}
			});
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static void d() throws IOException
	{
		Document doc = Document.of(new String(Test.class.getResourceAsStream("invoice.json").readAllBytes()));

		System.out.println("" + doc.findFirst("taxTotal/taxSubtotals/taxAmount/amount"));
		System.out.println("" + doc.findMany("taxTotal/taxSubtotals/taxAmount/amount"));
		System.out.println("" + doc.sum("taxTotal/taxSubtotals/taxAmount/amount"));
		System.out.println("" + doc.count("taxTotal/taxSubtotals/taxAmount/amount"));
	}


	private static void e() throws IOException
	{
		Document doc = Document.of(new String(Test.class.getResourceAsStream("invoice.json").readAllBytes()));

		for (Document person : doc.getDocuments("distributionList"))
		{
			System.out.println(person);
		}

		for (Document person : doc.getArray("distributionList").asDocumentArray())
		{
			System.out.println(person);
		}

		for (Array array : doc.getArrays("arrayOfArrays"))
		{
			System.out.println(array);
			for (int a : array.asIntArray())
			{
				System.out.println(a);
			}
		}

//		for (int a : doc.getInts("array"))
//		{
//			System.out.println(a);
//		}
		System.out.println("" + doc.getArray("distributionList").toJson());
	}


	private static void f() throws IOException
	{
		Document doc = Document.of(new String(Test.class.getResourceAsStream("manifest_1.json").readAllBytes()));

//		System.out.println(doc.findMany("manifest/manifestItems[subType/identifier='travel']/guests[classification='adult']/weight/amount").size());
//		System.out.println(doc.findMany("manifest/manifestItems[subType/identifier='travel']/driver[classification='adult']/weight/amount").size());
//
//		System.out.println(doc.findMany("manifest/manifestItems[subType/identifier='cargo']/guests[classification='adult']/weight/amount").size());
//		System.out.println(doc.findMany("manifest/manifestItems[subType/identifier='cargo']/driver[classification='adult']/weight/amount").size());
//		System.out.println(doc.findMany("manifest/manifestItems/subType/identifier").stream().distinct().toList());
//		System.out.println(doc.distinct("manifest/manifestItems/subType/identifier"));
//		System.out.println(doc.distinct("manifest/manifestItems/type"));

//		Stream s = doc.findMany("manifest/manifestItems/guests").reduce().stream();
//		System.out.println("-".repeat(100));
//		s.forEach(System.out::println);

//System.out.println(doc.findMany("manifest/manifestItems/guests/*").reduce().size());

		Document guestClasses1 = Document.of("a:1,b:1");
		Document guestClasses2 = Document.of("a:1,c:1");

//		guestClasses.merge(guestClasses2, Integer::sum);
		Document guestClasses = new Document();
		guestClasses.merge(guestClasses1, (a, b) -> (Integer)a + (int)(Integer)b);
		guestClasses.merge(guestClasses2, (a, b) -> (Integer)a + (int)(Integer)b);

		System.out.println(guestClasses);
		System.out.println(guestClasses1);
		System.out.println(guestClasses2);

//		HashMap<String,Integer> m = new HashMap<>();
//		m.merge("a", 1, Integer::sum);


//		System.out.println(doc.count("manifest/manifestItems[subType/identifier=travel]/guests/*"));
//		System.out.println(doc.count("manifest/manifestItems[subType/identifier!=travel]/guests/*"));
//		System.out.println(doc.count("manifest/manifestItems/driver"));
//		System.out.println(doc.count("manifest/manifestItems[driver!=null]/guests/*"));
//		System.out.println(doc.count("manifest/manifestItems[driver=null]/guests/*"));

//		System.out.println(doc.count("manifest/manifestItems[subType!=null]"));
//		System.out.println(doc.count("manifest/manifestItems[subType=null]"));
////		System.out.println(doc.findMany("manifest/manifestItems[subType!=null]/guests"));
////		System.out.println(doc.findMany("manifest/manifestItems[subType=null]/guests"));
//		System.out.println(doc.findMany("manifest/manifestItems[subType!=null]/guests/*").size());
//		System.out.println(doc.findMany("manifest/manifestItems[subType=null]/guests/*").size());
//		System.out.println(doc.findMany("manifest/manifestItems[type/identifier=travel][subType=travel]/guests/*").size());
//		System.out.println(doc.findMany("manifest/manifestItems[type/identifier=travel][subType=null]/guests/*").size());
//		System.out.println(doc.distinct("manifest/manifestItems/subType/identifier"));
//		System.out.println(doc.distinct("manifest/manifestItems/guests/classification"));
//		System.out.println(doc.distinct("manifest/manifestItems/driver/classification"));

//		System.out.println(doc.findMany("manifest/manifestItems/guests[classification='adult']/weight/amount").size());
//		System.out.println(doc.findMany("manifest/manifestItems/guests[classification='child']/weight/amount").size());
//		System.out.println(doc.findMany("manifest/manifestItems/guests[classification='baby']/weight/amount").size());
//		System.out.println(doc.findMany("manifest/manifestItems/guests[classification='toddler']/weight/amount").size());
//		System.out.println(doc.findMany("manifest/manifestItems/guests[classification!='child']/weight/amount").size());
//		System.out.println(doc.findMany("manifest/manifestItems/guests/weight/amount").size());

//		System.out.println("" + doc.findMany("manifest/manifestItems/guests[classification='adult']/weight/amount"));
//		System.out.println("" + doc.findMany("manifest/manifestItems/guests[classification='child']/weight/amount"));
//		System.out.println("" + doc.findMany("manifest/manifestItems/guests[classification='infant']/weight/amount"));
//
//		System.out.println("" + doc.sum("manifest/manifestItems[terminalPosition/level=1]/dimensions/outer/length/amount"));
//		System.out.println("" + doc.count("manifest/manifestItems/terminalPosition/[level=1]"));
//		System.out.println("" + doc.count("manifest/manifestItems/terminalPosition/[level=2]"));
//
//		Array d0 = doc.findMany("manifest/manifestItems/loadingEquipments[equipmentType/identifier='AR']");
//		double d1 = d0.sum("dimensions/outer/length/amount");
//		double d2 = d0.sum("measurements/gross_weight/value/amount");
//
//		System.out.println(d0);
//		System.out.println(d1);
//		System.out.println(d2);
//
//		System.out.println(doc.count("manifest/manifestItems/orderLineServices/[code='generic_add_on']/properties/[value='keep_cabin']"));
//		System.out.println(doc.count("manifest/manifestItems/orderLineServices/[code='generic_add_on']/properties/[value='firearms']"));
//		System.out.println(doc.count("manifest/manifestItems/orderLineServices/[code='meal_service']/properties/[value='Dinner'][value='Adult']"));
	}
}
