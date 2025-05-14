package org.terifan.raccoon.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDate;
import java.util.Random;
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

//			System.out.println(dic);

			byte[] data = dic.toByteArray(doc);
//			_Log.hexDump(data);

			Document dec = dic.fromByteArray(data);

			assertEquals(doc, dec);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	@Test
	public void testMarshal() throws IOException, ClassNotFoundException
	{
		Document doc = _Person.createPerson(new Random(1));
		Dictionary dic = Dictionary.of(doc);

		byte[] dicData = marshal(dic);

		Dictionary dicRestored = unmarshal(dicData);

//		System.out.println(dic);
//		System.out.println(dicRestored);
//		_Log.hexDump(dicData);

		assertEquals(dicRestored, dic);
	}


	static byte[] marshal(Dictionary dic) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(baos))
		{
			dic.writeExternal(oos);
		}
		return baos.toByteArray();
	}


	static Dictionary unmarshal(byte[] aBuffer) throws IOException, ClassNotFoundException
	{
		Dictionary dic = new Dictionary();
		try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(aBuffer)))
		{
			dic.readExternal(ois);
		}
		return dic;
	}
}
