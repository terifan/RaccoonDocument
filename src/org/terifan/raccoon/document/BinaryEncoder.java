package org.terifan.raccoon.document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import test_document._Log;


// <obj1><obj2><obj3><strings><structs><directory>
class BinaryEncoder
{
	private LinkedHashMap<String, Integer> strs = new LinkedHashMap();
	private LinkedHashMap<Object, Integer> mObjectLookup = new LinkedHashMap();
	private LinkedHashMap<String, Integer> structs = new LinkedHashMap();
	private ArrayList<Long> mObjectPosition = new ArrayList();

	private final BinaryOutputStream mOutputStream;


	public BinaryEncoder(OutputStream aOutputStream)
	{
		mOutputStream = new BinaryOutputStream(aOutputStream);
	}


	void marshal(Object aObject) throws IOException
	{
		BinaryCodec type = BinaryCodec.identify(aObject);

		if (type == null)
		{
			throw new IllegalArgumentException("Unsupported type: " + aObject.getClass().getCanonicalName());
		}

		if (aObject instanceof Document v)
		{
			writeDocument(v);
		}
		else if (aObject instanceof Array v)
		{
			writeArray(v);
		}
		else
		{
			throw new IllegalStateException();
		}

//		System.out.println(strs);
//		System.out.println(refs);
//		System.out.println(structs);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BinaryOutputStream buffer = new BinaryOutputStream(baos);
		buffer.writeUnsignedVarint(strs.size());
		for (String s : strs.keySet())
		{
			buffer.writeUnsignedVarint(s.length());
			buffer.writeUTF(s);
		}
		byte[] buf = zip(baos.toByteArray());
		writeInterleaved(2, buf.length);
		mOutputStream.writeBytes(buf);

		baos = new ByteArrayOutputStream();
		buffer = new BinaryOutputStream(baos);
		buffer.writeUnsignedVarint(structs.size());
		for (String s : structs.keySet())
		{
			buffer.writeUnsignedVarint(s.length());
			buffer.writeUTF(s);
		}
		buf = zip(baos.toByteArray());
		writeInterleaved(1, buf.length);
		mOutputStream.writeBytes(buf);

		baos = new ByteArrayOutputStream();
		buffer = new BinaryOutputStream(baos);
		buffer.writeUnsignedVarint(mObjectPosition.size());
		for (long pos : mObjectPosition)
		{
			buffer.writeUnsignedVarint(pos);
		}
		buf = zip(baos.toByteArray());
		writeInterleaved(0, buf.length);
		mOutputStream.writeBytes(buf);

		mOutputStream.close();
	}


	void writeDocument(Document aDocument) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BinaryOutputStream buffer = new BinaryOutputStream(baos);

		ArrayList<Object> pendingValues = new ArrayList<>();
		ArrayList<BinaryCodec> pendingTypes = new ArrayList<>();

		StringBuilder format = new StringBuilder();
		for (Entry<String, Object> entry : aDocument.entrySet())
		{
			String key = entry.getKey();
			Object value = entry.getValue();
			BinaryCodec type = BinaryCodec.identify(value);

			pendingTypes.add(type);
			pendingValues.add(value);

			format.append("[" + type + "," + key + "]");
		}

		String formatString = format.toString();
		if (structs.containsKey(formatString))
		{
			buffer.writeUnsignedVarint(structs.get(formatString));
		}
		else
		{
			buffer.writeUnsignedVarint(structs.size());
			structs.put(formatString, structs.size());
		}

		for (int i = 0; i < pendingValues.size(); i++)
		{
			BinaryCodec type = pendingTypes.get(i);
			Object value = pendingValues.get(i);

			writeValue(buffer, type, value);
		}

		byte[] buf = zip(baos.toByteArray());
		if (baos.size()<= buf.length)buf=baos.toByteArray();

		writeInterleaved(3 + mObjectPosition.size(), buf.length);
		mOutputStream.writeBytes(buf);

		mObjectPosition.add(mOutputStream.position());
		mObjectLookup.put(aDocument, mObjectLookup.size());
	}


	void writeArray(Array aArray) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BinaryOutputStream buffer = new BinaryOutputStream(baos);

		for (int offset = 0, elementCount = aArray.size(); offset < elementCount;)
		{
			ArrayList<Object> pending = new ArrayList<>();
			BinaryCodec type = null;
			int runLen = 0;

			for (int i = offset; i < elementCount; i++, runLen++)
			{
				Object value = aArray.get(i);
				BinaryCodec nextType = BinaryCodec.identify(value);

				if (type != nextType && type != null)
				{
					break;
				}

				pending.add(value);
				type = nextType;
			}

			writeToken(buffer, type, runLen);

			for (int i = 0; --runLen >= 0; i++, offset++)
			{
				Object value = pending.get(i);

				writeValue(buffer, type, value);
			}
		}

		byte[] buf = zip(baos.toByteArray());
		if (baos.size()<= buf.length)buf=baos.toByteArray();

		writeInterleaved(3 + mObjectPosition.size(), buf.length);
		mOutputStream.writeBytes(buf);

		mObjectPosition.add(mOutputStream.position());
		mObjectLookup.put(aArray, mObjectLookup.size());
	}


	private void writeValue(BinaryOutputStream aOutputStream, BinaryCodec aType, Object aValue) throws IOException
	{
		if (aType == BinaryCodec.STRING)
		{
			Integer ref = strs.get(aValue.toString());
			if (ref != null)
			{
				BinaryCodec.STRING.encoder.encode(aOutputStream, ref);
			}
			else
			{
				BinaryCodec.STRING.encoder.encode(aOutputStream, strs.size());

				strs.put(aValue.toString(), strs.size());
			}
		}
		else if (aType == BinaryCodec.DOCUMENT || aType == BinaryCodec.ARRAY)
		{
			Integer ref = mObjectLookup.get(aValue);
			if (ref != null)
			{
				BinaryCodec.REFERENCE.encoder.encode(aOutputStream, ref);
			}
			else
			{
				if (aType == BinaryCodec.DOCUMENT)
				{
					writeDocument((Document)aValue);
				}
				else
				{
					writeArray((Array)aValue);
				}

				BinaryCodec.REFERENCE.encoder.encode(aOutputStream, mObjectLookup.size());
			}
		}
		else
		{
			aType.encoder.encode(aOutputStream, aValue);
		}
	}


	void writeToken(BinaryOutputStream aOutputStream, BinaryCodec aType, int aValue) throws IOException
	{
		aOutputStream.writeInterleaved(aType.ordinal(), aValue);
	}


	private byte[] zip(byte[] aData) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
//		try (DeflaterOutputStream dos = new DeflaterOutputStream(baos, deflater))
		try (DeflaterOutputStream dos = new DeflaterOutputStream(baos))
		{
			dos.write(aData);
		}
		return baos.toByteArray();
	}


	private void writeInterleaved(int aOffset, int aIndex) throws IOException
	{
		mOutputStream.writeInterleaved(aIndex, aOffset);
	}
}
