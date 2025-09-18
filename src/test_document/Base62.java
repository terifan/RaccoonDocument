package test_document;

import java.math.BigInteger;
import java.util.Arrays;


/**
 * https://base62.org/java_sample/
 */
public class Base62
{
	private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";


	public static String encode(byte[] aInput)
	{
		if (aInput.length == 0)
		{
			return "";
		}

		BigInteger value = new BigInteger(1, aInput);
		StringBuilder result = new StringBuilder();

		while (value.compareTo(BigInteger.ZERO) > 0)
		{
			BigInteger[] divmod = value.divideAndRemainder(BigInteger.valueOf(62));
			result.append(BASE62.charAt(divmod[1].intValue()));
			value = divmod[0];
		}

		for (byte b : aInput)
		{
			if (b == 0)
			{
				result.append(BASE62.charAt(0));
			}
			else
			{
				break;
			}
		}

		return result.reverse().toString();
	}


	public static byte[] decode(String aInput)
	{
		if (aInput.length() == 0)
		{
			return new byte[0];
		}

		BigInteger value = BigInteger.ZERO;
		for (char c : aInput.toCharArray())
		{
			value = value.multiply(BigInteger.valueOf(62)).add(BigInteger.valueOf(BASE62.indexOf(c)));
		}

		byte[] bytes = value.toByteArray();
		if (bytes[0] == 0)
		{
			bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
		}

		int leadingZeroes = 0;
		for (char c : aInput.toCharArray())
		{
			if (c != BASE62.charAt(0))
			{
				break;
			}
			leadingZeroes++;
		}

		byte[] result = new byte[leadingZeroes + bytes.length];
		System.arraycopy(bytes, 0, result, leadingZeroes, bytes.length);
		return result;
	}
}
