package test_document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Base64;
import org.terifan.raccoon.document.Document;
import org.terifan.raccoon.document.Signer;


public class Test3
{
	public static void main(String ... args)
	{
		try
		{
			Document doc = Document.of("path:/,filename:'image.jpg',host:'ftp.patrikolsson.se',port:21,userName:guest,password:password1,keyFormat:,keyData:,content:Base64(a6f615Ad)");

//			doc.fromByteArray(byteBuffer);
//			doc.fromByteArray(byteArray);
//			doc.fromByteArray(byteArray, offset, length);
//			doc.fromJson(reader);
//			doc.fromJson(string);
//			doc.fromJson(string, shortValues);
//
//			doc.toByteArray();
//			doc.toByteArray(filter);
//			doc.toJson();
//			doc.toJson(appendable);
//			doc.toJson(compact);
//			doc.toJson(appendable, compact);
//			doc.toTypedJson();
//			doc.toTypedJson(appendable);
//			doc.toTypedJson(compact);
//			doc.toTypedJson(appendable, compact);

			StringWriter sw = new StringWriter();
			doc.toJson(sw);
			doc.serialize().asJson().withIndents(true).withTypes(true).to(sw);
			System.out.println(sw.toString());
//			doc.serialize().asJson().toString();
//			doc.serialize().asJson().toByteArray();
//			doc.serialize().asJson().to(new ByteArrayOutputStream());

			doc.deserialize().asBinary().from(new ByteArrayInputStream(new byte[100]));
			doc.deserialize().asJson().from(new StringReader(""));
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
