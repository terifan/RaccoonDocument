package test_document;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.terifan.raccoon.document.Array;
import org.terifan.raccoon.document.BinaryCodec;
import org.terifan.raccoon.document.BinaryInputStream;
import org.terifan.raccoon.document.Collection;
import org.terifan.raccoon.document.Document;
import org.terifan.raccoon.document.Support;


public class TestBinarySerialization
{
	public static void main(String... args)
	{
		try
		{
			Document a = Document.parseJson(new InputStreamReader(new FileInputStream("c:/data/json_testdata/manifest_2.json")));
//			Document a = _Person.createPerson(new Random(1));
//			Document a = Document.of("id:641,name:something,properties:[{id:1,value:red},{id:7,value:long},{id:4,value:heavy,properties:[{id:8794,value:stone}]},{id:48,value:shreak}]");
//			Document a = Document.of("id:641,name:something");
//			Document a = Document.of("id:641,values:[1,2,3]");
//			Document a = Document.of("id:641,data:[[{ape:1,banana:{crow:1}},  {ape:2,banana:{crow:1}},  {ape:3,banana:{crow:1}},  {ape:4,banana:{crow:1}}], [{ape:1,banana:{crow:1}},  {ape:2,banana:{crow:1}},  {ape:3,banana:{crow:1}},  {ape:4,banana:{crow:1}}]]");
//			Document a = Document.of("id:641,data:[[{ape:1}], [{ape:1}]]");
//			System.out.println(a);

//for (int i = 0; i <100;i++)
//{
//	System.out.println(i);
//			byte[] data = a.toByteArray();
//}
			byte[] data = a.toByteArray();

//			_Log.hexDump(data);

//			System.out.println(a.toJson().length()+"\t"+Support.zip(a.toJson().getBytes()).length);
//			System.out.println(data.length+"\t"+Support.zip(data).length);

			Collection doc = Document.parseByteArray(data);

			System.out.println(a);
			System.out.println(doc);

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
