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

/*
    iss (issuer): Issuer of the JWT
    sub (subject): Subject of the JWT (the user)
    aud (audience): Recipient for which the JWT is intended
    exp (expiration time): Time after which the JWT expires
    nbf (not before time): Time before which the JWT must not be accepted for processing
    iat (issued at time): Time at which the JWT was issued; can be used to determine age of the JWT
    jti (JWT ID): Unique identifier; can be used to prevent the JWT from being replayed (allows a token to be used only once)
*/

/**
 * The Signer class performs JWT (JSON Web Token) compatible encoding, decoding and verification of a document.
 *
 * String message = new Signer("secret").toSignedString(Document.of("id:123"));
 */
public class Signer
{
	private final Function<Document, byte[]> mSecretProvider;
	private Algorithm mAlgorithm;


	public enum Algorithm
	{
		HS256, HS384, HS512,
		/** unsupported by JWT */
		MD5_HMAC
	}


	public Signer(String aSecret)
	{
		this(aSecret.getBytes(StandardCharsets.UTF_8));
	}


	public Signer(byte[] aSecret)
	{
		this(h -> aSecret);
	}


	public Signer(Function<Document, byte[]> aSecretProvider)
	{
		if (aSecretProvider == null)
		{
			throw new IllegalArgumentException();
		}

		mSecretProvider = aSecretProvider;
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
	 * @param aHeader an optional custom header document
	 */
	public String toSignedString(Document aMessage, Document aHeader)
	{
		aHeader = new Document().putAll(aHeader).putIfAbsent("alg", k -> mAlgorithm.name());

		byte[] headerBytes = aHeader.toJson().getBytes(StandardCharsets.UTF_8);
		byte[] payloadBytes = aMessage.toJson().getBytes(StandardCharsets.UTF_8);

		Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
		String headerBase64 = encoder.encodeToString(headerBytes);
		String payloadBase64 = encoder.encodeToString(payloadBytes);

		headerBytes = headerBase64.getBytes(StandardCharsets.UTF_8);
		payloadBytes = payloadBase64.getBytes(StandardCharsets.UTF_8);

		byte[] sign = sign(aHeader, headerBytes, payloadBytes);

		return headerBase64 + "." + payloadBase64 + "." + encoder.encodeToString(sign);
	}


	/**
	 * Decode an encoded signed string representation of a Document.
	 *
	 * @param aMessage a three part base64 encoded and signed message
	 * @return this document with the content of the message decoded
	 */
	public Document fromSignedString(String aMessage)
	{
		return fromSignedStringImpl(aMessage, null);
	}


	/**
	 * Decode an encoded signed string representation of a Document.
	 *
	 * @param aMessage a three part base64 encoded and signed message
	 * @param oDecodedHeader if not null then the header of the signed message will be returned in this Document
	 * @return this document with the content of the message decoded
	 */
	public Document fromSignedString(String aMessage, Document oDecodedHeader)
	{
		if (oDecodedHeader == null)
		{
			throw new IllegalArgumentException("oDecodedHeader is null");
		}

		return fromSignedStringImpl(aMessage, oDecodedHeader);
	}


	private Document fromSignedStringImpl(String aMessage, Document oDecodedHeader)
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
	 * Decode a signed binary representation of a Document.
	 *
	 * @param aMessage a singed binary message
	 * @return this document with the content of the message decoded
	 */
	public Document fromSignedByteArray(byte[] aMessage)
	{
		return fromSignedByteArrayImpl(aMessage, null);
	}


	/**
	 * Decode a signed binary representation of a Document.
	 *
	 * @param aMessage a singed binary message
	 * @param oDecodedHeader a Document that with receive all header elements, must not be null
	 * @return this document with the content of the message decoded
	 */
	public Document fromSignedByteArray(byte[] aMessage, Document oDecodedHeader)
	{
		if (oDecodedHeader == null)
		{
			throw new IllegalArgumentException("oDecodedHeader is null");
		}

		return fromSignedByteArrayImpl(aMessage, oDecodedHeader);
	}


	private Document fromSignedByteArrayImpl(byte[] aMessage, Document oDecodedHeader)
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
			byte[] secret = mSecretProvider.apply(aHeader);

			if (secret == null || secret.length == 0)
			{
				throw new IllegalArgumentException(secret == null ? "Signature is null" : "Signature is empty");
			}

			Mac mac = createMac(alg);
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


	protected Mac createMac(String aAlgorithm) throws NoSuchAlgorithmException
	{
		String impl = switch (aAlgorithm)
		{
			case "HS256" ->
				"HmacSHA256";
			case "HS384" ->
				"HmacSHA384";
			case "HS512" ->
				"HmacSHA512";
			case "MD5_HMAC" ->
				"HmacMD5";
			default ->
				aAlgorithm;
		};
		return Mac.getInstance(impl);
	}
}
