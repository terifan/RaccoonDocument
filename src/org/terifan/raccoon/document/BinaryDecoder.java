package org.terifan.raccoon.document;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import static org.terifan.raccoon.document.BinaryCodec.ARRAY;
import static org.terifan.raccoon.document.BinaryCodec.DOCUMENT;
import static org.terifan.raccoon.document.BinaryCodec.REFERENCE;
import static org.terifan.raccoon.document.BinaryCodec.TERMINATOR;
import static org.terifan.raccoon.document.BinaryEncoder.VERSION;


public class BinaryDecoder
{
	private final byte[] mReadBuffer = new byte[8];
	private MurmurHash3 mChecksum;
	private InputStream mInputStream;
	private Visitor mVisitor;
	private final ReferenceMap mReferences;


	BinaryDecoder(InputStream aInputStream)
	{
		this(aInputStream, null);
	}


	BinaryDecoder(InputStream aInputStream, Visitor aVisitor)
	{
		mInputStream = aInputStream;
		mVisitor = aVisitor;
		mReferences = new ReferenceMap();
	}


	Object unmarshal() throws IOException
	{
		Token token = readToken();
		Path path = new Path();

		switch (token.type)
		{
			case DOCUMENT:
				Document d = new Document();
				mReferences.register(d, path.toString());
				readDocument(path, d, VisitorResult.CONTINUE);
				return d;
			case ARRAY:
				Array a = new Array();
				mReferences.register(a, path.toString());
				readArray(path, a, VisitorResult.CONTINUE);
				return a;
			case TERMINATOR:
				return token.type;
			default:
				return readValue(path, token.type, VisitorResult.CONTINUE);
		}
	}


//	@SuppressWarnings("unchecked")
//	Object unmarshal(Class aType) throws IOException
//	{
//		try
//		{
//			Token token = readToken();
//			Path path = new Path();
//
//			switch (token.type)
//			{
//				case DOCUMENT:
//					Document d = ((Constructor<Document>)aType.getConstructor()).newInstance();
//					mReferences.register(d);
//					return readDocument(path, d, VisitorResult.CONTINUE);
//				case ARRAY:
//					Array a = ((Constructor<Array>)aType.getConstructor()).newInstance();
//					mReferences.register(a);
//					return readArray(path, a, VisitorResult.CONTINUE);
//				case TERMINATOR:
//					return token.type;
//				default:
//					return readValue(path, token.type, VisitorResult.CONTINUE);
//			}
//		}
//		catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e)
//		{
//			throw new IOException(e);
//		}
//	}


	void unmarshal(Collection aContainer) throws IOException
	{
		Token token = readToken();
		Path path = new Path();

		if (aContainer instanceof Document v)
		{
			if (token.type == BinaryCodec.ARRAY)
			{
				throw new StreamException("Attempt to unmarshal a Document when binary stream contains an Array.");
			}
			if (token.type != BinaryCodec.DOCUMENT)
			{
				throw new StreamException("Stream corrupted.");
			}

			mReferences.register(v, path.toString());
			readDocument(path, v, VisitorResult.CONTINUE);
		}
		else if (aContainer instanceof Array v)
		{
			if (token.type == BinaryCodec.DOCUMENT)
			{
				throw new StreamException("Attempt to unmarshal an Array when binary stream contains a Document.");
			}
			if (token.type != BinaryCodec.ARRAY)
			{
				throw new StreamException("Stream corrupted.");
			}

			mReferences.register(v, path.toString());
			readArray(path, v, VisitorResult.CONTINUE);
		}
		else
		{
			throw new StreamException("Stream corrupted.");
		}
	}


	Token readToken() throws IOException
	{
		boolean first = mChecksum == null;
		if (first)
		{
			mChecksum = new MurmurHash3(VERSION);
		}

		int checksum = getChecksumValue();
		long params = readInterleaved();

		Token token = new Token();
		token.checksum = checksum;
		token.value = (int)(params >>> 32);
		token.type = BinaryCodec.values()[(int)params];

		if (first && token.value != VERSION)
		{
			throw new StreamException("Unsupported stream encoding version: " + token.value);
		}
		if (token.type == BinaryCodec.TERMINATOR && token.value != token.checksum)
		{
			throw new StreamException("Checksum error in data stream");
		}

		return token;
	}


	Document readDocument(Path aPath, Document aDocument, VisitorResult aState) throws IOException
	{
		boolean skipSiblings = aState == VisitorResult.SKIP_SUBTREE;

		for (;;)
		{
			Token token = readToken();
			if (token.type == BinaryCodec.TERMINATOR)
			{
				if (token.value != token.checksum)
				{
					throw new StreamException("Checksum error in data stream");
				}
				break;
			}

			String key = readUTF(token.value);

			aPath.enter(key);

//			if (mVisitor != null)
//			{
//				if (skipSiblings)
//				{
//					Object value = readValue(aPath, token.type, aState);
//				}
//				else
//				{
//					VisitorResult result = mVisitor.preVisit(aPath);
//
//					if (result == VisitorResult.TERMINATE)
//					{
//						aPath.state = VisitorResult.TERMINATE;
//						return aDocument;
//					}
//					else if (result == VisitorResult.SKIP_SIBLINGS)
//					{
//						skipSiblings = true;
//						aPath.state = VisitorResult.SKIP_SUBTREE;
//					}
//
//					Object value = readValue(aPath, token.type, result == VisitorResult.SKIP ? VisitorResult.SKIP_SUBTREE : aState);
//
//					if (aPath.state == VisitorResult.TERMINATE)
//					{
//						return aDocument;
//					}
//
//					if (!skipSiblings && result != VisitorResult.SKIP)
//					{
//						aDocument.putImpl(key, value);
//						result = mVisitor.postVisit(aPath, value);
//
//						if (result == VisitorResult.TERMINATE)
//						{
//							aPath.state = VisitorResult.TERMINATE;
//							return aDocument;
//						}
//						else if (result == VisitorResult.SKIP_SIBLINGS)
//						{
//							skipSiblings = true;
//						}
//					}
//				}
//			}
//			else
			{
				Object value = readValue(aPath, token.type, aState);
				aDocument.putImpl(key, value);
			}

			aPath.exit();
		}

		return aDocument;
	}


	Array readArray(Path aPath, Array aArray, VisitorResult aState) throws IOException
	{
		for (;;)
		{
			Token token = readToken();

			if (token.type == BinaryCodec.TERMINATOR)
			{
				if (token.value != token.checksum)
				{
					throw new StreamException("Checksum error in data stream");
				}
				break;
			}

			for (int i = 0; i < token.value; i++)
			{
				aArray.add(readValue(aPath, token.type, aState));
			}
		}

		return aArray;
	}


	private Object readValue(Path aPath, BinaryCodec aType, VisitorResult aState) throws IOException
	{
		switch (aType)
		{
			case DOCUMENT:
				Document d = new Document();
				mReferences.register(d, aPath.toString());
				readDocument(aPath, d, aState);
				return d;
			case ARRAY:
				Array a = new Array();
				mReferences.register(a, aPath.toString());
				readArray(aPath, a, aState);
				return a;
			case REFERENCE:
				return mReferences.get((int)aType.decoder.decode(this, aPath, aState));
			default:
				return aType.decoder.decode(this, aPath, aState);
		}
	}


	void close() throws IOException
	{
		mInputStream = null;
	}


	int getChecksumValue()
	{
		return mChecksum.getValue4bits();
	}


	int readByte() throws IOException
	{
		int c = mInputStream.read();
		if (c == -1)
		{
			throw new StreamException("Premature end of stream");
		}
		mChecksum.updateByte(c);
		return c;
	}


	short readShort() throws IOException
	{
		return (short)((readByte() << 8) | readByte());
	}


	int readInt() throws IOException
	{
		readBytes(mReadBuffer, 0, 4);
		return ((mReadBuffer[0] & 0xff) << 24)
			+ ((mReadBuffer[1] & 0xff) << 16)
			+ ((mReadBuffer[2] & 0xff) << 8)
			+ ((mReadBuffer[3] & 0xff) << 0);
	}


	long readLong() throws IOException
	{
		readBytes(mReadBuffer, 0, 8);
		return (((long)(mReadBuffer[0] & 0xff) << 56)
			+ ((long)(mReadBuffer[1] & 0xff) << 48)
			+ ((long)(mReadBuffer[2] & 0xff) << 40)
			+ ((long)(mReadBuffer[3] & 0xff) << 32)
			+ ((long)(mReadBuffer[4] & 0xff) << 24)
			+ ((mReadBuffer[5] & 0xff) << 16)
			+ ((mReadBuffer[6] & 0xff) << 8)
			+ ((mReadBuffer[7] & 0xff) << 0));
	}


	byte[] readBytes(byte[] aBuffer) throws IOException
	{
		int len = mInputStream.read(aBuffer);
		mChecksum.updateBytes(aBuffer, 0, len);
		return aBuffer;
	}


	<T> T skipBytes(int aLength) throws IOException
	{
		byte[] t = new byte[aLength];
		int len = mInputStream.read(t);
		mChecksum.updateBytes(t, 0, len);
		return null;
	}


	byte[] readBytes(byte[] aBuffer, int aOffset, int aLength) throws IOException
	{
		int len = mInputStream.read(aBuffer, aOffset, aLength);
		mChecksum.updateBytes(aBuffer, aOffset, aLength);
		if (len != aLength)
		{
			throw new IOException("Error reading from underlying stream. Only " + len + " bytes available, expected " + aLength);
		}
		return aBuffer;
	}


	long readVarint() throws IOException
	{
		for (long n = 0, result = 0; n < 64; n += 7)
		{
			int b = readByte();
			result += (long)(b & 127) << n;
			if (b < 128)
			{
				return (result >>> 1) ^ -(result & 1);
			}
		}

		throw new StreamException("Variable int64 exceeds maximum length");
	}


	long readUnsignedVarint() throws IOException
	{
		for (long n = 0, result = 0; n < 64; n += 7)
		{
			int b = readByte();
			result += (long)(b & 127) << n;
			if (b < 128)
			{
				return result;
			}
		}

		throw new StreamException("Variable int64 exceeds maximum length");
	}


	String readUTF(int aLength) throws IOException
	{
		if (aLength < 0)
		{
			throw new StreamException("Negative string length");
		}

		char[] output = new char[aLength];

		for (int i = 0; i < output.length; i++)
		{
			int c = readByte();

			if (c < 128) // 0xxxxxxx
			{
				output[i] = (char)c;
			}
			else if ((c & 0xE0) == 0xC0) // 110xxxxx
			{
				output[i] = (char)(((c & 0x1F) << 6) | (readByte() & 0x3F));
			}
			else if ((c & 0xF0) == 0xE0) // 1110xxxx
			{
				output[i] = (char)(((c & 0x0F) << 12) | ((readByte() & 0x3F) << 6) | (readByte() & 0x3F));
			}
			else
			{
				throw new StreamException("This decoder only handles 16-bit characters: c = " + c);
			}
		}

		return new String(output);
	}


	long readInterleaved() throws IOException
	{
		long p = readUnsignedVarint();
		return (reverseShift(p) << 32) | reverseShift(p >>> 1);
	}


	private static long reverseShift(long aWord)
	{
		aWord &= 0x5555555555555555L;

		aWord = (aWord | (aWord >> 1)) & 0x3333333333333333L;
		aWord = (aWord | (aWord >> 2)) & 0x0f0f0f0f0f0f0f0fL;
		aWord = (aWord | (aWord >> 4)) & 0x00ff00ff00ff00ffL;
		aWord = (aWord | (aWord >> 8)) & 0x0000ffff0000ffffL;
		aWord = (aWord | (aWord >> 16)) & 0x00000000ffffffffL;

		return aWord;
	}


	static class Token
	{
		int value;
		int checksum;
		BinaryCodec type;


		@Override
		public String toString()
		{
			return type.name();
		}
	}


	public static interface Visitor
	{
		default VisitorResult preVisit(Path aPath)
		{
			return VisitorResult.CONTINUE;
		}


		default Object valueProxy(Path aPath, Object aValue)
		{
			return aValue;
		}


		VisitorResult postVisit(Path aPath, Object aValue);
	}


	public static enum VisitorResult
	{
		TERMINATE,
		SKIP,
		SKIP_SIBLINGS,
		SKIP_SUBTREE,
		CONTINUE;


		boolean isSkip()
		{
			switch (this)
			{
				case VisitorResult.SKIP_SUBTREE:
				case VisitorResult.SKIP_SIBLINGS:
				case VisitorResult.SKIP:
					return true;
				default:
					return false;
			}
		}
	}
}
