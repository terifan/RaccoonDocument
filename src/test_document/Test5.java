package test_document;

import java.util.Base64;
import java.util.UUID;
import org.terifan.raccoon.document.Array;
import org.terifan.raccoon.document.Document;


public class Test5
{
	public static void main(String... args)
	{
		try
		{
			// 15VPGdXe5YcINv3mqroCImlhR5phVWSyiQWwXG63UWLDjd9wGjzXIntnNKC7zy4hkgacIx7Hi1pVAjjgbJeX43u3OHTbcPWWemg177YrxuCiS
			// uL1HeNM3GCgkJx7YqkzQ8z6tPioMMILjl1RNqXb21LOTRJa0uc2c6PfzAixwcmdV3KBPbNzZS8tLpb1f7BKY4Ib8qxLXQ

			// 9Gvl2HSVGkTcHVtbZgmexkGFACd4mdg9FuRVKbLTSa3SBiN5er4IZ4SkBh98SIR7J30ldlhyzOR

			Array arr = Array.of(
				Array.of(0, UUID.randomUUID(), UUID.randomUUID()),
				Array.of(1, 315),
				Array.of(2, 1, 2, 3, 4),
				Array.of(3, (int)(System.currentTimeMillis() / 1000)),
				Array.of(4, (int)(System.currentTimeMillis() / 1000) + 500)
			);

			System.out.println(Base62.encode(arr.toByteArray()));

			Document doc = new Document()
				.put("0", Array.of(UUID.randomUUID(), UUID.randomUUID()))
				.put("1", 315)
				.put("2", Array.of(1, 2, 3, 4))
				.put("3", (int)(System.currentTimeMillis() / 1000))
				.put("4", (int)(System.currentTimeMillis() / 1000) + 500)
			;

			System.out.println(Base62.encode(doc.toByteArray()));
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}
	}
}
