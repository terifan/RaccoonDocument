package test_document;

import java.util.Base64;
import org.terifan.raccoon.document.Document;
import org.terifan.raccoon.document.Signer;


public class Test2
{
	public static void main(String ... args)
	{
		try
		{
			Document doc = Document.of("path:/,filename:'image.jpg',host:'ftp.patrikolsson.se',port:21,userName:guest,password:password1,keyFormat:,keyData:,content:Base64(a6f615Ad)");

			System.out.println(doc.toTypedJson(false));

			System.out.println(new String(doc.toByteArray()));
			System.out.println(new Signer("secret").toSignedString(doc));
			System.out.println(Base64.getEncoder().withoutPadding().encodeToString(doc.toByteArray()));
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
