package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Read or write values to the underlying stream. The SupportedTypes enum specifies the support types by the StreamMarshaller.
 * <p/>
 * This class maintains a running checksum of data written and each token contain a 4 bit checksum value.
 * <p/>
 * <pre>
 * 		ByteArrayOutputStream baos = new ByteArrayOutputStream();
 *		try (StreamMarshaller enc = new StreamMarshaller(baos))
 *		{
 *			enc.write(4);
 *			enc.write("test");
 *			enc.write(Array.of(1,2,3));
 *			enc.write(new Document().put("name", "John Doe"));
 *		}
 *
 * 		try (StreamMarshaller dec = new StreamMarshaller(new ByteArrayInputStream(baos.toByteArray())))
 *		{
 *			int i = dec.read();
 *			String s = dec.read();
 *			Array a = dec.read();
 *			Document d = dec.read();
 *		}
 * </pre>
 */
public class StreamMarshaller implements AutoCloseable
{
	private OutputStream mOutputStream;
	private InputStream mInputStream;
	private BinaryEncoder mEncoder;
	private BinaryDecoder mDecoder;


	public StreamMarshaller(OutputStream aOutputStream)
	{
		mOutputStream = aOutputStream;
		mEncoder = new BinaryEncoder(aOutputStream);
	}


	public StreamMarshaller(InputStream aInputStream)
	{
		mInputStream = aInputStream;
		mDecoder = new BinaryDecoder(aInputStream, false);
	}


	public <T extends Object> T read(Class aType) throws IOException
	{
		if (mDecoder == null)
		{
			throw new IllegalStateException("This Marshaller is either closed or not created for reading a stream.");
		}
		return (T)mDecoder.unmarshal(aType);
	}


	public <T extends Object> T read() throws IOException
	{
		if (mDecoder == null)
		{
			throw new IllegalStateException("This Marshaller is either closed or not created for reading a stream.");
		}
		return (T)mDecoder.unmarshal();
	}


	public StreamMarshaller write(Object aObject) throws IOException
	{
		if (mEncoder == null)
		{
			throw new IllegalStateException("This Marshaller is either closed or not created for writing a stream.");
		}
		mEncoder.marshal(aObject);
		return this;
	}


	/**
	 * The terminator token is optional and can be written to the stream to ensure a final checksum verification.
	 */
	public StreamMarshaller writeTerminator() throws IOException
	{
		if (mEncoder == null)
		{
			throw new IllegalStateException("This Marshaller is either closed or not created for writing a stream.");
		}
		mEncoder.terminate();
		return this;
	}


	/**
	 * Read the next token and throw an IOException if not a terminator.
	 */
	public StreamMarshaller expectTerminator() throws IOException
	{
		if (mDecoder == null)
		{
			throw new IllegalStateException("This Marshaller is either closed or not created for reading a stream.");
		}
		Object value = mDecoder.unmarshal();
		if (value != SupportedTypes.TERMINATOR)
		{
			throw new IOException("Expected a terminator but found: " + (value == null ? null : value.getClass()));
		}
		return this;
	}


	@Override
	public void close() throws IOException
	{
		if (mEncoder != null)
		{
			mEncoder.close();
			mEncoder = null;
			mOutputStream.close();
			mOutputStream = null;
		}
		if (mDecoder != null)
		{
			mDecoder.close();
			mDecoder = null;
			mInputStream.close();
			mInputStream = null;
		}
	}
}
