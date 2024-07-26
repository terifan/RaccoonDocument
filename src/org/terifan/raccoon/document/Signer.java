package org.terifan.raccoon.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.function.Function;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public class Signer
{
	private final Function<Document, byte[]> mSecret;
	private Algorithm mAlgorithm;


	public enum Algorithm
	{
		HS256, HS384, HS512
	}


	public Signer(String aSecret)
	{
		this(aSecret.getBytes(StandardCharsets.UTF_8));
	}


	public Signer(byte[] aSecret)
	{
		this(h -> aSecret);
	}


	public Signer(Function<Document, byte[]> aSecret)
	{
		if (aSecret == null)
		{
			throw new IllegalArgumentException();
		}

		mSecret = aSecret;
		mAlgorithm = Algorithm.HS256;
	}


	public Signer with(Algorithm aAlgorithm)
	{
		if (aAlgorithm == null)
		{
			throw new IllegalArgumentException();
		}

		mAlgorithm = aAlgorithm;
		return this;
	}


	/**
	 * Return an encoded signed string representation of this Document. The format is identical to a JWT token.
	 *
	 * @param aSecret secret passphrase used when signing the message
	 */
	public String toSignedString(Document aMessage)
	{
		return toSignedString(aMessage, null);
	}


	/**
	 * Return an encoded signed string representation of this Document. The format is identical to a JWT token.
	 *
	 * <code>
	 * String jwt = new Signer("1234").toSignedString(doc, Document.of("typ:JWT,alg:HS512"));
	 * </code>
	 *
	 * note: if the header contains an "alg" field that will algorithm is used when signing a message.
	 *
	 * @param aSecret secret passphrase used when signing the message
	 * @param aHeader a custom header document.
	 */
	public String toSignedString(Document aMessage, Document aHeader)
	{
		aHeader = new Document().putAll(aHeader).putIfAbsent("alg", k -> mAlgorithm.name());

		byte[] headerBytes = aHeader.toJson(true).getBytes(StandardCharsets.UTF_8);
		byte[] payloadBytes = aMessage.toJson(true).getBytes(StandardCharsets.UTF_8);

		Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
		byte[] header = encoder.encodeToString(headerBytes).getBytes(StandardCharsets.UTF_8);
		byte[] payload = encoder.encodeToString(payloadBytes).getBytes(StandardCharsets.UTF_8);

		byte[] sign = sign(aHeader, header, payload);

		return new String(header) + "." + new String(payload) + "." + encoder.encodeToString(sign);
	}


	/**
	 * Decode an encoded signed string representation of a Document.
	 *
	 * @param aSecret secret used when signing the message
	 * @param aMessage a three part base64 encoded and signed message
	 * @return this document with the content of the message decoded
	 */
	public Document fromSignedString(String aMessage)
	{
		return fromSignedString(aMessage, null);
	}


	/**
	 * Decode an encoded signed string representation of a Document.
	 *
	 * @param aSecretProvider Function returning the secret used when signing the message. The function receive the decoded header of the
	 * message.
	 * @param aMessage a three part base64 encoded and signed message
	 * @param oDecodedHeader if not null then the header of the signed message will be returned in this Document
	 * @return this document with the content of the message decoded
	 */
	public Document fromSignedString(String aMessage, Document oDecodedHeader)
	{
		if (!aMessage.matches("[0-9A-Za-z\\-\\_]{0,}\\.[0-9A-Za-z\\-\\_]{0,}\\.[0-9A-Za-z\\-\\_]{0,}"))
		{
			throw new IllegalArgumentException("Message not formatted correctly.");
		}

		Base64.Decoder decoder = Base64.getUrlDecoder();

		int i = aMessage.indexOf('.');
		int j = aMessage.lastIndexOf('.');
		String messageHeader = aMessage.substring(0, i);
		String messagePayload = aMessage.substring(i + 1, j);
		String messageSignature = aMessage.substring(j + 1);

		Document aHeader = new Document().fromJson(new String(decoder.decode(messageHeader), StandardCharsets.UTF_8));

		byte[] sign = sign(aHeader, messageHeader.getBytes(), messagePayload.getBytes());

		verify(decoder.decode(messageSignature), sign);

		if (oDecodedHeader != null)
		{
			oDecodedHeader.putAll(aHeader);
		}

		return new Document().fromJson(new String(decoder.decode(messagePayload), StandardCharsets.UTF_8));
	}


	/**
	 * Return a signed compressed binary representation of this Document. Signing algorithm is HS256.
	 *
	 * @param aSecret secret used when signing the message
	 */
	public byte[] toSignedByteArray(Document aMessage)
	{
		return toSignedByteArray(aMessage, null);
	}


	/**
	 * Return a signed compressed binary representation of this Document.
	 *
	 * note: if the header contains an "alg" field that will algorithm is used when signing a message.
	 *
	 * @param aSecret secret used when signing the message
	 * @param aHeader an optional custom header document.
	 */
	public byte[] toSignedByteArray(Document aMessage, Document aHeader)
	{
		aHeader = new Document().putAll(aHeader).putIfAbsent("alg", k -> mAlgorithm.name());

		byte[] header = aHeader.toByteArray();
		byte[] payload = aMessage.toByteArray();
		byte[] sign = sign(aHeader, header, payload);

		return Array.of(compress(header), compress(payload), sign).toByteArray();
	}


	/**
	 * Decode a signed binary representation of a Document without checking the signature.
	 *
	 * @param aMessage a three part base64 encoded and signed message
	 * @return this document with the content of the message decoded
	 */
	public Document fromSignedByteArray(byte[] aMessage)
	{
		return fromSignedByteArray(aMessage, null);
	}


	public Document fromSignedByteArray(byte[] aMessage, Document oDecodedHeader)
	{
		Array chunks = new Array().fromByteArray(aMessage);

		if (chunks.size() != 3)
		{
			throw new IllegalArgumentException("Expected exactly three entries in the message (array).");
		}

		byte[] headerBytes = decompress(chunks.getBinary(0));
		byte[] payloadBytes = decompress(chunks.getBinary(1));
		Document header = new Document().fromByteArray(headerBytes);

		byte[] sign = sign(header, headerBytes, payloadBytes);

		verify(chunks.getBinary(2), sign);

		if (oDecodedHeader != null)
		{
			oDecodedHeader.putAll(header);
		}

		return new Document().fromByteArray(payloadBytes);
	}


	protected void verify(byte[] aExpected, byte[] aFound) throws SignerException
	{
		if (!Arrays.equals(aExpected, aFound))
		{
			throw new SignerException("Message signature failed verification");
		}
	}


	private byte[] compress(byte[] aData)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (DeflaterOutputStream iis = new DeflaterOutputStream(baos))
		{
			iis.write(aData);
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Failed to compress message", e);
		}
		return baos.toByteArray();
	}


	private byte[] decompress(byte[] aData)
	{
		try (InflaterInputStream iis = new InflaterInputStream(new ByteArrayInputStream(aData)))
		{
			return iis.readAllBytes();
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Failed to decompress message", e);
		}
	}


	private byte[] sign(Document aHeader, byte[] aHeaderBytes, byte[] aPayloadBytes) throws IllegalStateException
	{
		Mac mac = createMac(aHeader);
		mac.update(aHeaderBytes);
		mac.update((byte)'.');
		mac.update(aPayloadBytes);
		return mac.doFinal();
	}


	private Mac createMac(Document aHeader)
	{
		String alg = aHeader.get("alg");

		if (alg == null)
		{
			throw new IllegalArgumentException("Unsupported algorithm: " + alg);
		}

		try
		{
			alg = switch (alg)
			{
				case "HS256" ->
					"HmacSHA256";
				case "HS384" ->
					"HmacSHA384";
				case "HS512" ->
					"HmacSHA512";
				default ->
					alg;
			};

			byte[] secret = mSecret.apply(aHeader);

			if (secret == null || secret.length == 0)
			{
				throw new IllegalArgumentException(secret == null ? "Signature is null" : "Signature is empty");
			}

			Mac mac = Mac.getInstance(alg);
			mac.init(new SecretKeySpec(secret, mac.getAlgorithm()));
			return mac;
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new SignerException("NoSuchAlgorithm " + alg);
		}
		catch (InvalidKeyException e)
		{
			throw new SignerException("Failed to create signature", e);
		}
	}
}
