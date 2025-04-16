package test_document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;
import org.terifan.raccoon.document.Array;
import org.terifan.raccoon.document.BinaryDecoder.Visitor;
import org.terifan.raccoon.document.BinaryDecoder.VisitorResult;
import org.terifan.raccoon.document.BinaryWalker;
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
			c();
//			d();
//			e();
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
					System.out.println(aPath+" "+aValue);
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
}
