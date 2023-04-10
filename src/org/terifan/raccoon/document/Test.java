package org.terifan.raccoon.document;


public class Test
{
	public static void main(String ... args)
	{
		try
		{
			Document doc = new Document();
			doc.put("@a", "A");
			doc.put("@b/c", "B-C");
			doc.put("@b/i", "B-I");
			doc.put("@b/j/k", "B-J-K");
			doc.put("@d/e/0", "D-E-0");
			doc.put("@f/g/0/h", "F-G-0-H");
			doc.put("@m/n/0/0/p", "M-N-0-0-P");

			System.out.println(doc);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
