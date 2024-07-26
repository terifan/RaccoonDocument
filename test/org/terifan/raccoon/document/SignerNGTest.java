package org.terifan.raccoon.document;

import java.io.IOException;
import java.util.Random;
import java.util.function.Function;
import static org.testng.Assert.*;
import org.testng.annotations.Test;


public class SignerNGTest
{
	@Test
	public void testSigedBinaryEncodeNoHeader() throws IOException
	{
		Random rnd = new Random(1);
		String secret = "1234";

		Document encodingMessage = _Person.createPerson(rnd);

		byte[] data = new Signer(secret).toSignedByteArray(encodingMessage);

		Document decodedHeader = new Document();
		Document decodedMessage = new Signer(secret).fromSignedByteArray(data, decodedHeader);

		assertEquals(decodedMessage, encodingMessage);
	}


	@Test
	public void testSigedBinaryEncode() throws IOException
	{
		Random rnd = new Random(1);
		String secret = "1234";

		Document encodingMessage = _Person.createPerson(rnd);
		Document encodingHeader = Document.of("alg:HS512,by:bobby");

		byte[] data = new Signer(secret).toSignedByteArray(encodingMessage, encodingHeader);

		Document decodedHeader = new Document();
		Document decodedMessage = new Signer(secret).fromSignedByteArray(data, decodedHeader);

		assertEquals(decodedMessage, encodingMessage);
		assertEquals(decodedHeader, encodingHeader);
	}


	@Test
	public void testSigedStringEncode() throws IOException
	{
		Random rnd = new Random(1);
		String secret = "1234";

		Document encodingMessage = _Person.createPerson(rnd);

		String data = new Signer(secret).toSignedString(encodingMessage);

		Document header = new Document();
		Document decodedMessage = new Signer(secret).fromSignedString(data, header);

		assertEquals(decodedMessage, encodingMessage);
	}


	@Test
	public void testSigedStringEncodeWithHeader() throws IOException
	{
		Random rnd = new Random(1);

		Function<Document, byte[]> secret = h ->
		{
			if ("bobby".equals(h.getString("by")))
			{
				return "1234".getBytes();
			}
			throw new IllegalStateException();
		};

		Document encodingMessage = _Person.createPerson(rnd);
		Document encodingHeader = Document.of("typ:JWT,alg:HS512,by:bobby");

		String data = new Signer(secret).toSignedString(encodingMessage, encodingHeader);

		System.out.println(data);

		Document decodedHeader = new Document();
		Document decodedMessage = new Signer(secret).fromSignedString(data, decodedHeader);

		System.out.println(decodedHeader);
		System.out.println(decodedMessage);

		assertEquals(decodedHeader.get("by"), "bobby");
		assertEquals(decodedMessage, encodingMessage);
	}


	@Test
	public void testAlgorithm() throws IOException
	{
		Random rnd = new Random(1);
		String secret = "1234";

		Document message = _Person.createPerson(rnd);

		byte[] data = new Signer(secret).with(Signer.Algorithm.HS384).toSignedByteArray(message);

		Document h = new Document();
		new Signer(secret).fromSignedByteArray(data, h);
		System.out.println(h);
	}


	@Test(expectedExceptions = SignerException.class)
	public void testBadSignature() throws IOException
	{
		Random rnd = new Random(1);
		String secret1 = "1234";
		String secret2 = "abcd";

		Document message = _Person.createPerson(rnd);
		byte[] data = new Signer(secret1).toSignedByteArray(message);
		new Signer(secret2).fromSignedByteArray(data);

		fail();
	}


	@Test(expectedExceptions = SignerException.class)
	public void testUnsupportedAlgorithm() throws IOException
	{
		String data = new Signer("abc").toSignedString(Document.of(""), Document.of("alg:xxx"));
	}
}
