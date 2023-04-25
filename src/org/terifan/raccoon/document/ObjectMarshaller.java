package org.terifan.raccoon.document;

import java.lang.reflect.Field;


public class ObjectMarshaller
{
	public static Document toDocument(Object aInstance)
	{
		Document root = new Document();
		Document current = root;

		Class cls = aInstance.getClass();

		for (;;)
		{
			Document doc = new Document();
			current.put("class", cls.getCanonicalName());
			current.put("fields", doc);

			for (Field field : cls.getDeclaredFields())
			{
				try
				{
					field.setAccessible(true);
					Object value = field.get(aInstance);
					if (SupportedTypes.identify(value) != null)
					{
						doc.put(field.getName(), value);
					}
				}
				catch (IllegalAccessException e)
				{
					throw new IllegalArgumentException(e);
				}
			}

			cls = cls.getSuperclass();

			if (cls == Object.class)
			{
				break;
			}

			Document tmp = new Document();
			current.put("super", tmp);
			current = tmp;
		}

		return root;
	}
}
