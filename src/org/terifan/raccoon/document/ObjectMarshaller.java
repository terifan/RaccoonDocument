package org.terifan.raccoon.document;

import java.lang.reflect.Field;
import java.util.List;


public class ObjectMarshaller
{
	public static Document toDocument(Object aInstance)
	{
		if (aInstance instanceof List v)
		{
			Array array = new Array();
			v.forEach(o->array.add(impl(o)));

			Document root = new Document();
			root.put("class", aInstance.getClass().getCanonicalName());
			root.put("elements", array);

			return root;
		}
		else
		{
			return impl(aInstance);
		}
	}


	private static Document impl(Object aInstance)
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
