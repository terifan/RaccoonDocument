package org.terifan.raccoon.document;

import java.io.StringReader;
import java.util.ArrayList;


public class PathExpression_Deprecated
{
	public PathExpression_Deprecated(String aPath)
	{
		try
		{
			Token root = new Token(Type.Expression, "");
			Token current = root;
			ArrayList<Token> tokens = current.tokens;

			StringReader in = new StringReader(aPath);
			StringBuilder sb = new StringBuilder();
			for (Type type = null;;)
			{
				int c = in.read();

				if (c == -1)
				{
					if (type != null)
					{
						Token t = new Token(type, sb.toString());
						tokens.add(t);
					}
					break;
				}

				if (c == ' ')
				{
					if (sb.length() > 0)
					{
						Token t = new Token(type, sb.toString());
						tokens.add(t);
						sb.setLength(0);
					}
					type = null;
				}
				else if (c == '/')
				{
					if (sb.length() > 0)
					{
						Token t = new Token(type, sb.toString());
						tokens.add(t);
						sb.setLength(0);
					}
					Token t = new Token(Type.Path, "/");
					tokens.add(t);
					type = null;
				}
				else if (c == '\'' || c == '\"')
				{
					if (sb.length() > 0)
					{
						Token t = new Token(type, sb.toString());
						tokens.add(t);
						sb.setLength(0);
					}
					for (;;)
					{
						int d = in.read();
						if (d == -1)
						{
							throw new IllegalStateException("Unclosed string");
						}
						if (d == c)
						{
							break;
						}
						sb.append((char)d);
					}
					Token t = new Token(Type.Literal, sb.toString());
					tokens.add(t);
					sb.setLength(0);
					type = null;
				}
				else if (c >= '0' && c <= '9' || c == '-' || c == '+' || c == '.')
				{
					if (c == '.' && sb.toString().equals("."))
					{
						Token t = new Token(Type.Path, "..");
						tokens.add(t);
						sb.setLength(0);
						type = null;
					}
					else if (c == '.' && sb.toString().contains("."))
					{
						throw new IllegalStateException();
					}
					else
					{
						if (type != null && type != Type.Number)
						{
							tokens.add(new Token(type, sb.toString()));
							sb.setLength(0);
						}
						sb.append((char)c);
						type = Type.Number;
					}
				}
				else if (c == '<' || c == '>' || c == '=' || c == '!' || c == '&' || c == '|')
				{
					if (type != null && type != Type.Op)
					{
						tokens.add(new Token(type, sb.toString()));
						sb.setLength(0);
					}
					sb.append((char)c);
					type = Type.Op;
				}
				else if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_')
				{
					if (type == Type.Number)
					{
						throw new IllegalStateException();
					}
					if (type != null && type != Type.Path)
					{
						tokens.add(new Token(type, sb.toString()));
						sb.setLength(0);
					}
					sb.append((char)c);
					type = Type.Path;
				}
				else if (c == '[' || c == '(')
				{
					if (type != null)
					{
						tokens.add(new Token(type, sb.toString()));
						sb.setLength(0);
						type = null;
					}
					Token tmp = new Token(Type.Expression, "[");
					tmp.parent = current;
					tokens.add(tmp);
					current = tmp;
					tokens = current.tokens;
				}
				else if (c == ']' || c == ')')
				{
					if (type != null)
					{
						tokens.add(new Token(type, sb.toString()));
						sb.setLength(0);
						type = null;
					}
					current = current.parent;
					tokens = current.tokens;
				}
				else
				{
					sb.append((char)'#');
					sb.append((char)c);
//					throw new IllegalStateException();
				}
			}

			System.out.println(tokens);
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}
	}


	enum Type
	{
		Literal,
		Number,
		Op,
		Path,
		Expression;
	}


	public static class Token
	{
		ArrayList<Token> tokens = new ArrayList<>();
		Token parent;
		String token;
		Type type;


		public Token(Type aType, String aToken)
		{
			type = aType;
			this.token = aToken;
		}


		@Override
		public String toString()
		{
			if (type == Type.Expression)
			{
				return type + "{" + tokens.toString() + "}";
			}
			return type + "{" + token + '}';
		}
	}
}
