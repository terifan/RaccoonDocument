package org.terifan.raccoon.serializer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import org.terifan.raccoon.document.Array;
import org.terifan.raccoon.document.Document;
import org.terifan.raccoon.document.SupportedTypes;


public class Marshaller
{
	private Document mDocument;
	private Array mShared;
	private HashMap<Class, TypeEncoder> mTypeEncoders;
	private HashMap<Class, TypeDecoder> mTypeDecoders;
	private HashMap<Object, Document> mInstances;
	private int[] mExcludeFieldsWithModifiers;
	private boolean mEnableComplexMapKeySerialization;
	private boolean mExcludeFieldsWithNullValue;
	private boolean mIncludeOverriddenFields;
	private boolean mIncludeClassTypes;


	public Marshaller()
	{
		mTypeEncoders = new HashMap();
		mTypeDecoders = new HashMap();

		mShared = new Array();
		mInstances = new HashMap<>();

		mDocument = new Document();
		mDocument.put("version", 1);
		mDocument.put("shared", mShared);

		mExcludeFieldsWithModifiers = new int[]
		{
			Modifier.STATIC
		};
	}


	public boolean isIncludeClassTypes()
	{
		return mIncludeClassTypes;
	}


	public Marshaller setIncludeClassTypes(boolean aState)
	{
		mIncludeClassTypes = aState;
		return this;
	}


	public boolean isIncludeOverriddenFields()
	{
		return mIncludeOverriddenFields;
	}


	public Marshaller setIncludeOverriddenFields(boolean aState)
	{
		mIncludeOverriddenFields = aState;
		return this;
	}


	public boolean isEnableComplexMapKeySerialization()
	{
		return mEnableComplexMapKeySerialization;
	}


	public Marshaller setEnableComplexMapKeySerialization(boolean aState)
	{
		mEnableComplexMapKeySerialization = aState;
		return this;
	}


	public int[] getExcludeFieldsWithModifiers()
	{
		return mExcludeFieldsWithModifiers;
	}


	public Marshaller setExcludeFieldsWithModifiers(int... aModifiers)
	{
		mExcludeFieldsWithModifiers = aModifiers;
		return this;
	}


	public boolean isExcludeFieldsWithNullValue()
	{
		return mExcludeFieldsWithNullValue;
	}


	public Marshaller setExcludeFieldsWithNullValue(boolean aState)
	{
		mExcludeFieldsWithNullValue = aState;
		return this;
	}


	public Document marshall(Object aObject)
	{
		return marshallImpl(aObject);
	}


	public Document marshallImpl(Object aObject)
	{
		if (mInstances.containsKey(aObject))
		{
			return mInstances.get(aObject);
		}

		Document document = new Document();
		Document result = document;

		mInstances.put(aObject, result);

		try
		{
			Class<?> cls = aObject.getClass();

			if (mIncludeClassTypes)
			{
				document.put("$class-type", cls.getCanonicalName());

				for (Field field : cls.getDeclaredFields())
				{
					if (field.getName().equals("serialVersionUID"))
					{
						field.setAccessible(true);
						document.put("$class-version", field.get(aObject));
						break;
					}
				}
			}

			Array fields = null;
			if (mIncludeOverriddenFields)
			{
				fields = new Array();
				document.put("$class-fields", fields);
			}

			int level = 0;
			while (cls != null && cls != Object.class)
			{
				Document doc;
				if (mIncludeOverriddenFields)
				{
					doc = new Document();
					fields.add(doc);
				}
				else
				{
					doc = document;
				}
				for (Field field : cls.getDeclaredFields())
				{
					if (!Modifier.isTransient(field.getModifiers()) && !("serialVersionUID".equals(field.getName()) && field.getType() == Long.TYPE) && matchesModifiers(field))
					{
						field.setAccessible(true);

						Object value = serializeValue(field.get(aObject));

						if (value != null || !mExcludeFieldsWithNullValue)
						{
							doc.putIfAbsent(field.getName(), k -> value);
						}
					}
				}
				level++;
				cls = cls.getSuperclass();
				if (cls == Object.class)
				{
					break;
				}
			}
		}
		catch (RuntimeException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}

		return result;
	}


	private Object serializeValue(Object aObject)
	{
		try
		{
			if (aObject == null)
			{
				return null;
			}
			if (mTypeEncoders.containsKey(aObject.getClass()))
			{
				return mTypeEncoders.get(aObject.getClass()).write(aObject);
			}
			if (SupportedTypes.isSupported(aObject))
			{
				return aObject;
			}
			if (aObject.getClass().isEnum())
			{
				return aObject.getClass().getCanonicalName() + "." + ((Enum)aObject).name();
			}
			if (aObject instanceof List v)
			{
				Array array = new Array();
				for (Object item : v)
				{
					array.add(serializeValue(item));
				}
				return array;
			}
			if (aObject instanceof Map v)
			{
				if (mEnableComplexMapKeySerialization)
				{
					Document map = new Document();
					for (Entry item : (Set<Entry>)v.entrySet())
					{
						map.put(serializeValue(item.getKey()).toString(), serializeValue(item.getValue()));
					}
					return map;
				}

				Array array = new Array();
				for (Entry item : (Set<Entry>)v.entrySet())
				{
					array.add(new Document().put("key", serializeValue(item.getKey())).put("value", serializeValue(item.getValue())));
				}
				return array;
			}

			return marshallImpl(aObject);
		}
		catch (RuntimeException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
	}


	private boolean matchesModifiers(Field aField)
	{
		for (int p : mExcludeFieldsWithModifiers)
		{
			if ((aField.getModifiers() & p) == p)
			{
				return false;
			}
		}
		return true;
	}


	public Binder bind(String aClassName, int aVersion)
	{
		return new Binder(aClassName, aVersion);
	}


	public <T, U> Marshaller registerType(Class<T> aClass, TypeEncoder<T, U> aEncoder, TypeDecoder<T, U> aDecoder)
	{
		mTypeEncoders.put(aClass, aEncoder);
		mTypeDecoders.put(aClass, aDecoder);
		return this;
	}


	protected void warn(String aText)
	{
		System.err.println("ObjectMarshaller: " + aText);
	}


	public class Binder
	{
		private final String mClassName;
		private final int mVersion;
		private Function<Document, Object> mFunction;


		private Binder(String aClassName, int aVersion)
		{
			mClassName = aClassName;
			mVersion = aVersion;
		}


		public Marshaller to(Function<Document, Object> aFunction)
		{
			mFunction = aFunction;
			return Marshaller.this;
		}
	}
}
