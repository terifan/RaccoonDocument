package test_document;

import java.util.UUID;
import org.terifan.raccoon.document.Document;
import org.terifan.raccoon.document.Signer;
import org.terifan.raccoon.document.Support;


public class Auth
{
	public static void xmain(String ... args)
	{
		try
		{
			Document doc1 = new Document().put("a", 1);
//			Document doc1 = new Document()
//				.put("iss", Support.uuidToBytes(UUID.fromString("602a25ad-008f-4e4f-bbac-6230d2e191cf")))
//				.put("iat", System.currentTimeMillis() / 1000)
//				.put("id", Support.uuidToBytes(UUID.fromString("6abe6312-3046-41e3-9847-47311916d48b")))
//				;

			String data = new Signer("secret").toSignedString(doc1);

			System.out.println(data);

			System.out.println("https://mydocumentscan.eu/load/" + data + "/HazardousManifest.pdf");

			Document head = new Document();
			Document dec = new Signer("secret").fromSignedString(data, head);

			System.out.println(head);
			System.out.println(dec);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
