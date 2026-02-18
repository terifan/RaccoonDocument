package test_document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;


public class _Support
{
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
