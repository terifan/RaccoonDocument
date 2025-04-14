package org.terifan.raccoon.document;

import static org.testng.Assert.*;
import org.testng.annotations.Test;


public class PathExpressionNGTest
{
	@Test
	public void testSomeMethod()
	{
//		PathExpression e4 = new PathExpression("apple/[a=b && (9=9)=false]/x");

//		PathExpression e0 = new PathExpression("/apple");
//		PathExpression e1 = new PathExpression("0");
//		PathExpression e2 = new PathExpression("apple/b");
//		PathExpression e3 = new PathExpression("apple/0");
//		PathExpression e4 = new PathExpression("apple/b/[c=0]/d/[g=bob]/a");
//		PathExpression e5 = new PathExpression("apple/b/[c=false]/d/[g=bob]/a");
//		PathExpression e6 = new PathExpression("apple/b/[c='false']/d/[g=bob]/a");
//		PathExpression e7 = new PathExpression("apple/b/[c=test]/d/[g=bob]/a");
//		PathExpression e8 = new PathExpression("apple/b/[c=.5]/d/[g=bob]/a");
		PathExpression e9 = new PathExpression("apple/b/[c=/apple/e && c=../e && d=true || (d=false && (f<1 || f<=1 || f>1 || f>=1 || f!=1))]/d/[g=bob]/a");
	}
}
