package org.terifan.raccoon.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;


public class Support
{
	public static byte[] uuidToBytes(UUID aUUID)
	{
		long mst = aUUID.getMostSignificantBits();
		long lst = aUUID.getLeastSignificantBits();
		ByteBuffer bb = ByteBuffer.allocate(16);
		bb.putLong(mst);
		bb.putLong(lst);
		return bb.array();
	}


	public static UUID bytesToUUID(byte[] aBytes)
	{
		ByteBuffer bb = ByteBuffer.wrap(aBytes);
		return new UUID(bb.getLong(), bb.getLong());
	}


	public static byte[] zip(byte[] aData)
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (DeflaterOutputStream dos = new DeflaterOutputStream(baos, new Deflater(Deflater.DEFAULT_COMPRESSION, true)))
			{
				dos.write(aData);
			}
			return baos.toByteArray();
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}


	public static byte[] unzip(byte[] aData)
	{
		try
		{
			try (InflaterInputStream in = new InflaterInputStream(new ByteArrayInputStream(aData)))
			{
				return in.readAllBytes();
			}
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}
}
