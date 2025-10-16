package org.terifan.raccoon.document;

import java.util.ArrayList;
import test_document.Console;
import test_document.Console.Color;


public class PathExpression
{
	private String[] OPS =
	{
		"!==", // not any
		"!=", // not
		"==", // all
		"=", // any
		"<=",
		">=",
		"<",
		"<"
	};

	private boolean mEOF;


	public String parse(String aConsumedPath, String aPath, Expression aParent)
	{
		mEOF = false;
		Expression node = new ExpressionAnd();

		while (!mEOF)
		{
			if (aPath.isEmpty())
			{
				throw new IllegalStateException("Parser error");
			}

			aPath = aPath.trim();
			if (aPath.startsWith("&&"))
			{
				aPath = aPath.substring(2).trim();
			}
			else if (aPath.startsWith("||"))
			{
				ExpressionAnd right = new ExpressionAnd();
				ExpressionOr parent = new ExpressionOr();
				parent.nodes.add(node);
				parent.nodes.add(right);
				node = parent;
				aPath = parse(aConsumedPath, aPath.substring(2), right);
			}
			else if (aPath.startsWith("("))
			{
				ExpressionAnd right = new ExpressionAnd();
				ExpressionOr parent = new ExpressionOr();
				parent.nodes.add(node);
				parent.nodes.add(right);
				node = parent;
				aPath = parse(aConsumedPath, aPath.substring(1), right);
			}
			else if (aPath.startsWith("]"))
			{
				aPath = aPath.substring(1);
				mEOF = true;
				break;
			}
			else if (aPath.startsWith(")"))
			{
				aPath = aPath.substring(1);
				break;
			}
			else
			{
				Statement statement = new Statement();
				aPath = findOp(aPath, statement);

				if (aPath.startsWith("'"))
				{
					int i = aPath.indexOf("'", 1);
					statement.value = aPath.substring(1, i);
					aPath = aPath.substring(i + 1).trim();
				}
				else
				{
					int i = find(aPath, "]", ")", " ");
					String tmp = aPath.substring(0, i);
					aPath = aPath.substring(i).trim();
					statement.value = convertValue(tmp);
				}

				node.nodes.add(statement);
			}
		}

		aParent.nodes.add(node);

		return aPath;
	}


	private Object convertValue(String aToken) throws NumberFormatException
	{
		Object value;
		if (aToken.equals("null"))
		{
			value = null;
		}
		else if (aToken.equals("true"))
		{
			value = true;
		}
		else if (aToken.equals("false"))
		{
			value = true;
		}
		else if (aToken.matches("[0-9]+"))
		{
			value = Long.valueOf(aToken);
		}
		else
		{
			value = aToken;
		}
		return value;
	}


	private int find(String aString, String... aToken)
	{
		int offset = aString.length();
		for (String t : aToken)
		{
			int i = aString.indexOf(t);
			if (i != -1 && i < offset)
			{
				offset = i;
			}
		}
		return offset;
	}


	private String startsWith(String aString, String... aToken)
	{
		for (String t : aToken)
		{
			if (aString.startsWith(t))
			{
				return t;
			}
		}
		return "";
	}


	public static interface Node
	{
		void print(int aLevel);


		boolean eval(Collection aCollection);
	}


	public static abstract class Expression implements Node
	{
		ArrayList<Node> nodes = new ArrayList<>();


		private Expression()
		{
		}
	}


	private String findOp(String aPath, Statement aStatement)
	{
		int i = find(aPath, OPS);
		if (i > 0)
		{
			aStatement.key = aPath.substring(0, i);

//			if (aStatement.key.startsWith("'"))
//			{
//				aStatement.key = "constant(" + aStatement.key + ")";
//			}
//			else if (!aStatement.key.contains("("))
//			{
//				aStatement.key = "lookup(" + aStatement.key + ")";
//			}

			aPath = aPath.substring(i);
			aStatement.op = startsWith(aPath, OPS);
			aPath = aPath.substring(aStatement.op.length());
		}
		return aPath;
	}


	public static class ExpressionAnd extends Expression
	{
		@Override
		public boolean eval(Collection aCollection)
		{
			for (Node node : nodes)
			{
				if (!node.eval(aCollection))
				{
					return false;
				}
			}
			return true;
		}


		@Override
		public void print(int aLevel)
		{
			Console.println(Color.BLACK, aLevel, "(");
			boolean f = true;
			for (Node n : nodes)
			{
				if (!f)
				{
					Console.println(Color.BLACK, aLevel, "&&");
				}
				f = false;
				n.print(aLevel + 1);
			}
			Console.println(Color.BLACK, aLevel, ")");
		}


		@Override
		public String toString()
		{
			boolean first = true;
			StringBuilder sb = new StringBuilder();
			for (Node n : nodes)
			{
				if (!first)
				{
					sb.append(" && ");
				}
				first = false;
				sb.append(n);
			}
			return sb.toString();
		}
	}


	public static class ExpressionOr extends Expression
	{
		@Override
		public boolean eval(Collection aCollection)
		{
			boolean a = nodes.get(0).eval(aCollection);
			boolean b = nodes.get(1).eval(aCollection);
			return a | b;
		}


		@Override
		public void print(int aLevel)
		{
			Console.println(Color.BLACK, aLevel, "(");
			nodes.get(0).print(aLevel + 1);
			Console.println(Color.BLACK, aLevel, "||");
			nodes.get(1).print(aLevel + 1);
			Console.println(Color.BLACK, aLevel, ")");
		}


		@Override
		public String toString()
		{
			return "(" + nodes.get(0) + " || " + nodes.get(1) + ")";
		}
	}


	public static class Statement implements Node
	{
		String key;
		String op;
		Object value;


		public Statement()
		{
		}


		@Override
		public void print(int aLevel)
		{
			Console.println(Color.BLACK, aLevel, "%s", this);
		}


		@Override
		public boolean eval(Collection aCollection)
		{
			Console.println(Color.YELLOW, "eval " + aCollection.getClass().getSimpleName() + " " + key + " " + op + " " + value);

			if (aCollection instanceof Array arr)
			{
				Console.println(Color.YELLOW, "find in " + arr);

				boolean all = true;
				boolean any = false;
				for (Object o : arr)
				{
					if (o instanceof Collection doc) // ????????????????????
					{
						o = doc.findFirst(key);
						throw new IllegalStateException();
					}

					Console.println(Color.YELLOW, "-- " + key + " " + op + " " + value + " // " + o);

//					if (key.startsWith("lookup("))
//					{
//						o = lookup(aCollection, key.substring(key.indexOf('(') + 1, key.length() - 1));
//					}
//					else
//					{
//						throw new IllegalStateException();
//					}

					if (op.equals("=") || op.equals("==") || op.equals("!=") || op.equals("!=="))
					{
						boolean b = equalValues(o, value);
						all &= b;
						any |= b;
						if (b & op.equals("=") || !b && op.equals("!="))
						{
							return true;
						}
					}
					else if ((op.equals("<") || op.equals("<=")) && lessValue(o, value) || op.equals("<=") && equalValues(o, value))
					{
						return true;
					}
					else if ((op.equals(">") || op.equals(">=")) && greaterValue(o, value) || op.equals(">=") && equalValues(o, value))
					{
						return true;
					}
					else
					{
						throw new IllegalStateException();
					}
				}
				if (all && op.equals("==") || !any & op.equals("!=="))
				{
					return true;
				}
			}
			else if (aCollection instanceof Document doc)
			{
				Array findMany = doc.findMany(key);

				Console.println(Color.YELLOW, "findMany " + key + " " + findMany);

				if ((op.equals("=") || op.equals("==")) && (value == null && findMany.isEmpty()))
				{
					return true;
				}
				if ((op.equals("!=") || op.equals("!==")) && (value != null && findMany.isEmpty()))
				{
					return true;
				}

				boolean all = true;
				boolean any = false;
				for (Object o : findMany)
				{
					boolean b;
					if (op.equals("=") || op.equals("==") || op.equals("!=") || op.equals("!=="))
					{
						b = equalValues(o, value);
						Console.println(Color.YELLOW, o + "=" + b);
						all &= b;
						any |= b;
						if (b & op.equals("=") || !b && op.equals("!="))
						{
							return true;
						}
					}
					else if ((op.equals("<") || op.equals("<=")) && lessValue(o, value) || op.equals("<=") && equalValues(o, value))
					{
						return true;
					}
					else if ((op.equals(">") || op.equals(">=")) && greaterValue(o, value) || op.equals(">=") && equalValues(o, value))
					{
						return true;
					}
					else
					{
						throw new IllegalStateException();
					}
				}
				if (all && op.equals("==") || !any & op.equals("!=="))
				{
					return true;
				}
			}
			return false;
		}


		@Override
		public String toString()
		{
			if (value instanceof String)
			{
				return key + op + "'" + value + "'";
			}
			return key + op + value;
		}
	}


	private static Object lookup(Collection aCollection, String aPath)
	{
		return aCollection.findFirst(aPath);
	}


	private static Number toNumber(Object aValue)
	{
		if (aValue instanceof Number n)
		{
			return n;
		}
		return Double.valueOf(aValue.toString());
	}


	private static boolean lessValue(Object aValue, Object aComparedTo)
	{
		return toNumber(aValue).doubleValue() < toNumber(aComparedTo).doubleValue();
	}


	private static boolean greaterValue(Object aValue, Object aComparedTo)
	{
		return toNumber(aValue).doubleValue() > toNumber(aComparedTo).doubleValue();
	}


	private static boolean equalValues(Object aValue, Object aExpression)
	{
		if (aValue == null)
		{
			return aExpression == null;
		}
		if (aExpression == null)
		{
			return false;
		}
		if (aValue instanceof Boolean v)
		{
			return aExpression == v;
		}
		if (aValue instanceof Integer v && aExpression instanceof Integer w)
		{
			return v == (int)w;
		}
		return aValue.toString().equalsIgnoreCase(aExpression.toString());
	}
}
