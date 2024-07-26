package test_document;

import java.nio.charset.Charset;


public class _Log
{
	public static void hexDump(byte[] aBuffer)
	{
		int LW = 48;
		int MR = 1000;

		StringBuilder binText = new StringBuilder("");
		StringBuilder hexText = new StringBuilder("");

		for (int row = 0, offset = 0; offset < aBuffer.length && row < MR; row++)
		{
			hexText.append("\033[1;30m" + String.format("%04d: ", row * LW) + "\033[0m");

			int padding = 3 * LW + LW / 8;

			for (int i = 0; offset < aBuffer.length && i < LW; i++)
			{
				int c = 0xff & aBuffer[offset++];

				if (!(c < ' ' || c >= 128))
				{
					hexText.append("\033[0;36m");
					binText.append("\033[0;36m");
				}
				if (c >= '0' && c <= '9')
				{
					hexText.append("\033[0;35m");
					binText.append("\033[0;35m");
				}

				hexText.append(String.format("%02x ", c));
				binText.append(Character.isISOControl(c) || c > 127 ? '.' : (char)c);

				if (c < ' ' || c >= 128 || c >= '0' && c <= '9')
				{
					hexText.append("\033[0m");
					binText.append("\033[0m");
				}


				padding -= 3;

				if ((i & 7) == 7)
				{
					hexText.append(" ");
					padding--;
				}
			}

			for (int i = 0; i < padding; i++)
			{
				hexText.append(" ");
			}

			System.out.println(hexText.append(binText).toString());

			binText.setLength(0);
			hexText.setLength(0);
		}
	}


	public static String toHex(byte[] aValue)
	{
		StringBuilder sb = new StringBuilder();
		for (byte b : aValue)
		{
			sb.append(String.format("%02X", b));
		}
		return sb.toString();
	}


	public static String toString(byte[] aValue)
	{
		return aValue == null ? null : new String(aValue, Charset.defaultCharset());
	}
}
