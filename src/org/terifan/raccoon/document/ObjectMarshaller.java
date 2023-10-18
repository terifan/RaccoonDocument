package org.terifan.raccoon.document;

import java.lang.reflect.Field;
import static java.lang.reflect.Modifier.TRANSIENT;
import java.util.List;


public class ObjectMarshaller
{
	public static Document toDocument(Object aInstance)
	{
		if (aInstance == null)
		{
			throw new IllegalArgumentException();
		}

		if (aInstance instanceof List || aInstance.getClass().isArray())
		{
			Array array = new Array();
			if (aInstance.getClass().isArray())
			{
				array.addAll(aInstance);
			}
			else
			{
				((List)aInstance).forEach(o -> array.add(impl(o)));
			}

			Document root = new Document();
			root.put("class", aInstance.getClass().getCanonicalName());
			root.put("elements", array);

			try
			{
				Field field = aInstance.getClass().getDeclaredField("serialVersionUID");
				field.setAccessible(true);
				if (field.getType() == Long.TYPE)
				{
					root.put("version", field.getLong(aInstance));
				}
			}
			catch (NoSuchFieldException e)
			{
			}
			catch (IllegalAccessException | SecurityException e)
			{
				throw new IllegalArgumentException(e);
			}

			return root;
		}

		return impl(aInstance);
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
					if (field.getName().equals("serialVersionUID") && field.getType() == Long.TYPE)
					{
						current.put("version", field.getLong(aInstance));
					}
					else if ((field.getModifiers() & TRANSIENT) == 0)
					{
						Object value = field.get(aInstance);
						if (SupportedTypes.identify(value) != null)
						{
							doc.put(field.getName(), value);
						}
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
