package org.terifan.raccoon.document;

import java.io.FileNotFoundException;
import java.io.FileReader;
import static org.testng.Assert.*;
import org.testng.annotations.Test;


public class PathExpressionNGTest
{
	@Test
	public void testSomeMethod1() throws Exception
	{
		String name = "TRL";
		Collection doc = Document.parseJson(new FileReader("C:\\Develop\\surikat\\mds\\MDSServer\\src\\com\\surikat\\dips\\message_queue_consumer\\_sample_pof_manifest_sm_shipeqp.json"));
		Array docs = doc.findMany("manifest/manifestItems[type!=shipsEquipment && loadingEquipments[0]/equipmentType/identifier='" + name + "']");
		double v1 = docs.sum("loadingEquipments[0]/measurements/gross_weight/value/amount");
		double v2 = docs.sum("loadingEquipments/measurements/gross_weight/value/amount");
		assertEquals(v1, 72100.0);
		assertEquals(v2, 72100.0);
	}


	@Test
	public void testSomeMethod2() throws Exception
	{
		Document doc = Document.of(new String(test_document.Test.class.getResourceAsStream("test19.json").readAllBytes()));
		double v = doc.sum("aaa[bbb<=5]/bbb");
		assertEquals(v, 6.0);
	}


	@Test
	public void testSomeMethod3() throws Exception
	{
		Array doc = Array.of(1, 2, 4, 8);
		Array v = doc.findMany("[1]");
		assertEquals(v.size(), 1);
		assertEquals((int)v.getInt(0), 2);
	}


	@Test
	public void testSomeMethod4() throws Exception
	{
		Collection doc = Document.parseJson(new FileReader("C:\\Develop\\surikat\\mds\\MDSServer\\src\\com\\surikat\\dips\\message_queue_consumer\\_sample_pof_manifest_sm_shipeqp.json"));
		Array docs = doc.findMany("manifest/manifestItems[subType/identifier!=travel]/driver");
		assertEquals(docs.size(), 8);
	}


	@Test
	public void testSomeMethod5() throws Exception
	{
		String name = "CNT";
		Collection doc = Document.parseJson(new FileReader("C:\\Develop\\surikat\\mds\\MDSServer\\src\\com\\surikat\\dips\\message_queue_consumer\\_sample_pof_manifest_sm_shipeqp.json"));
		Array docs = doc.findMany("manifest/manifestItems[type!=shipsEquipment && loadingEquipments[0]/equipmentType/identifier='" + name + "']/*");
		double d = docs.sum("loadingEquipments[0]/dimensions/outer/length/amount");
		assertEquals(d, 36.0);
	}


	@Test
	public void testSomeMethod6() throws Exception
	{
		String name = "towed";
		Collection doc = Document.parseJson(new FileReader("C:\\Develop\\surikat\\mds\\MDSServer\\src\\com\\surikat\\dips\\message_queue_consumer\\_sample_pof_manifest_sm.json"));
		Array loadingEquipment = doc.findMany("manifest/manifestItems[loadingEquipments/equipmentType/identifier='" + name + "']");
		assertEquals(loadingEquipment.size(), 14);
	}


	@Test
	public void testSomeMethod7() throws Exception
	{
		Collection doc = Document.parseJson(new FileReader("C:\\Develop\\surikat\\mds\\MDSServer\\src\\com\\surikat\\dips\\message_queue_consumer\\_sample_pof_manifest_sm.json"));
		assertEquals(doc.count("manifest/manifestItems[subType/identifier=travel]/guests/*"), 853);
		assertEquals(doc.count("manifest/manifestItems[subType/identifier=travel]/driver"), 0);
		assertEquals(doc.count("manifest/manifestItems[subType/identifier!=travel]/guests/*"), 12);
		assertEquals(doc.count("manifest/manifestItems[subType/identifier!=travel]/driver"), 71);
		assertEquals((int)doc.distinct("manifest/manifestItems[guests!=null]/subType/identifier").get("travel", 0), 334);
		assertEquals(doc.count("manifest/manifestItems/driver"), 71);
		assertEquals(doc.count("manifest/manifestItems/guests"), 405);
		assertEquals(doc.count("manifest/manifestItems/guests/*"), 853+12);
	}


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
		PathExpression_Deprecated e9 = new PathExpression_Deprecated("apple/b/[c=/apple/e && c=../e && d=true || (d=false && (f<1 || f<=1 || f>1 || f>=1 || f!=1))]/d/[g=bob]/a");
		System.out.println(e9);
	}
}
