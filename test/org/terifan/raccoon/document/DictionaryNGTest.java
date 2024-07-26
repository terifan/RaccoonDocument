package org.terifan.raccoon.document;

import java.time.LocalDate;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import test_document._Log;


public class DictionaryNGTest
{
	@Test
	public void testSomeMethod()
	{
		try
		{
			Document doc = Document.of("""
			{"firstName":"John", "familyName":"Doe", "birthDate": LocalDate(1982-08-15), "gender":"male", "4":true}
            """);

			Dictionary dic = Dictionary.of(doc);
			dic.add("male");
			dic.add("female");
			dic.add(LocalDate.of(1982, 8, 15));

			System.out.println(dic);

			byte[] data = dic.toByteArray(doc);
			_Log.hexDump(data);

			Document dec = dic.fromByteArray(data);

			assertEquals(doc, dec);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
