package org.terifan.raccoon.document;

import java.util.ArrayList;


public class Path
{
	ArrayList<Object> mNodes = new ArrayList<>();


	Path(Object... aPath)
	{
		for (Object o : aPath)
		{
			if (o instanceof String s)
			{
				enter(s);
			}
			else if (o instanceof Integer i)
			{
				enter(i);
			}
			else
			{
				throw new IllegalArgumentException();
			}
		}
	}


	void enter(String aKey)
	{
		mNodes.addLast(aKey);
	}


	void enter(Integer aKey)
	{
		mNodes.addLast(aKey);
	}


	void exit()
	{
		mNodes.removeLast();
	}


	public int size()
	{
		return mNodes.size();
	}


	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		if (mNodes.isEmpty())sb.append("/");
		for (Object o : mNodes)
		{
			sb.append("/" + o);
		}
		return sb.toString();
	}


	@Override
	public boolean equals(Object aExpected)
	{
		if (aExpected != null)
		{
			if (aExpected instanceof Path expected)
			{
				return matches(expected);
			}
			if (aExpected.getClass().isArray())
			{
				return matches((Object[])aExpected);
			}
			if (aExpected instanceof String expected)
			{
				return matches(expected);
			}
			if (aExpected instanceof Integer expected)
			{
				return matches(expected);
			}
		}

		return false;
	}


	public boolean matches(Object... aExpected)
	{
		if (aExpected.length > mNodes.size())
		{
			return false;
		}

		for (int i = 0; i < aExpected.length; i++)
		{
			Object current = mNodes.get(i);
			Object expected = aExpected[i];
			if (current instanceof String s && (!(expected instanceof String t) || !s.matches(t)) || current instanceof Integer j && (!(expected instanceof Integer k) || !j.equals(k)))
			{
				return false;
			}
		}
		return true;
	}


	public boolean matches(Path aExpected)
	{
		if (aExpected.mNodes.size() > mNodes.size())
		{
			return false;
		}

		for (int i = 0; i < aExpected.mNodes.size(); i++)
		{
			Object current = mNodes.get(i);
			Object expected = aExpected.mNodes.get(i);
			if (current instanceof String s && (!(expected instanceof String t) || !s.matches(t)) || current instanceof Integer j && (!(expected instanceof Integer k) || !j.equals(k)))
			{
				return false;
			}
		}

		return true;
	}


	public boolean startsWith(Object... aExpected)
	{
		for (int i = 0; i < aExpected.length && i < mNodes.size(); i++)
		{
			Object current = mNodes.get(i);
			Object expected = aExpected[i];
			if (current instanceof String s && (!(expected instanceof String t) || !s.matches(t)) || current instanceof Integer j && (!(expected instanceof Integer k) || !j.equals(k)))
			{
				return false;
			}
		}
		return true;
	}


	public boolean startsWith(Path aExpected)
	{
		for (int i = 0; i < aExpected.size() && i < mNodes.size(); i++)
		{
			Object current = mNodes.get(i);
			Object expected = aExpected.mNodes.get(i);
			if (current instanceof String s && (!(expected instanceof String t) || !s.matches(t)) || current instanceof Integer j && (!(expected instanceof Integer k) || !j.equals(k)))
			{
				return false;
			}
		}
		return true;
	}
}
