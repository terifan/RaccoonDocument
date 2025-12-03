package test_document;


/*
 *    /...
 *    /personal/...
 *    /personal/healthInfo/...
 *    /personal/contacts/...
 *    /personal/home/...
 *    /personal/favorite/...
 *    /work/...
 *    /work/contacts/...
 *    /locationHistory/...
 */
public class Test4
{
	public static void main(String... args)
	{
		try
		{
//			Document doc = _Person.createPerson(new Random(1));
//
//			byte[] data = doc.toByteArray();
//
//			BinaryWalker walker = new BinaryWalker(new ByteArrayInputStream(data));
//
//			walker.visit(new Visitor()
//			{
//				@Override
//				public VisitorResult preVisit(Path aPath)
//				{
//					return aPath.startsWith("personal", "home") ? VisitorResult.CONTINUE : VisitorResult.SKIP;
////					return VisitorResult.CONTINUE;
//				}
//
//
//				@Override
//				public VisitorResult postVisit(Path aPath, Object aValue)
//				{
//					System.out.printf("%30s @ %s%n", (aValue.toString().length() > 30 ? aValue.toString().substring(0, 27) + "..." : aValue), aPath);
//					if (aPath.matches("personal", "home", "country"))
//					{
//						return VisitorResult.TERMINATE;
//					}
//					return VisitorResult.CONTINUE;
//				}
//			});
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}
	}
}
