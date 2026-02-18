package test_document;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Random;
import org.terifan.raccoon.document.Array;
import org.terifan.raccoon.document.Collection;
import org.terifan.raccoon.document.Document;


public class TestSerialization
{
	public static void main(String... args)
	{
		try
		{
//			Document a = Document.parseJson(new InputStreamReader(new FileInputStream("d:/data/json_testdata/manifest_3.json")));
			Document a = Document.parseJson(new InputStreamReader(new FileInputStream("d:/data/json_testdata/manifest_2.json")));
//			Document a = Document.parseJson(new InputStreamReader(new FileInputStream("d:/data/json_testdata/invoice.json")));
//			Document a = _Person.createPerson(new Random(1));
//			Document a = Document.of("id:641,name:something,properties:[{id:1,value:red},{id:7,value:long},{id:4,value:heavy,properties:[{id:8794,value:stone}]},{id:48,value:shreak}]");
//			Document a = Document.of("id:641,category:something,type:something,name:something");
//			Document a = Document.of("id:641,values:[1,2,3]");
//			Array a = Array.of(1,2,3);
//			Document a = Document.of("id:641,data:[[{ape:1,banana:{crow:1}},  {ape:2,banana:{crow:1}},  {ape:3,banana:{crow:1}},  {ape:4,banana:{crow:1}}], [{ape:1,banana:{crow:1}},  {ape:2,banana:{crow:1}},  {ape:3,banana:{crow:1}},  {ape:4,banana:{crow:1}}]]");
//			Document a = Document.of("id:641,data:[{ape:123}, {ape:123}]");
//			Document a = Document.of("id:641,data:{data2:[{a:{ape:123,banana:456}}]},data2:{ape:123,banana:456}");

//			System.out.println(a.toJson(false));
//			System.out.println(a);

//			a.put("min", Long.MIN_VALUE);
//			a.put("max", Long.MAX_VALUE);
//			a.put("mini", Integer.MIN_VALUE);
//			a.put("maxi", Integer.MAX_VALUE);
//			a.put("vals", Array.of(Long.MIN_VALUE,Long.MAX_VALUE,Integer.MIN_VALUE,Integer.MAX_VALUE));

			byte[] data = a.toByteArray();

			_Log.hexDump(data);

			System.out.println(a.toJson().length() + "\t" + _Support.zip(a.toJson().getBytes()).length);
			System.out.println(data.length + "\t" + _Support.zip(data).length);

			Collection doc = Document.parseByteArray(data);

//			System.out.println(doc.toJson(false));
//			System.out.println(doc);

			System.out.println(doc.equals(a));

//			BinaryInputStream in = new BinaryInputStream(new ByteArrayInputStream(data));
//			for (;;)
//			{
//				long params = in.readInterleaved();
//				int type = (int)(params >>> 32);
//				int len = (int)params;
//
//				System.out.println(type + "\t" + len);
//
//				if (type == 0)
//				{
//					break;
//				}
//
//				in.readBytes(new byte[len]);
//			}
//			Document b = Document.parseByteArray(data);
//
//			System.out.println(a.equals(b));
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
