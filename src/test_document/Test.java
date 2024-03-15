package test_document;

import java.io.ByteArrayInputStream;
import org.terifan.raccoon.document.Array;
import org.terifan.raccoon.document.Document;
import org.terifan.raccoon.document.StreamMarshaller;
import static test_document._Log.hexDump;


public class Test
{
	public static void main(String ... args)
	{
		try
		{
//			Document doc = Document.of("""
//            {"_id":"65ce1edbfd3d2ddc4f3784b1","changeDateTime":"2009-02-06T15:42:34","createDateTime":"2014-05-15T22:58:28","locationHistory":[{"lat":41.701477,"lng":79.05859,"time":"2014-01-25T15:03:07-03:00"},{"lat":57.318306,"lng":56.736435,"time":"2013-10-03T17:05:10-01:00"},{"lat":38.590572,"lng":12.31474,"time":"2010-05-01T16:21:19+04:00"}],"personal":{"account_balance":5433.112382065,"birthday":"1976-06-17","contacts":[{"text":"lanwatan_bregol2@yahoo.com","type":"email"},{"text":"050-6359054507","type":"phone"},{"text":"077-2080723656","type":"mobilePhone"}],"displayName":"laurealasso_amdirthorn99","favorite":{"color":"DarkGoldenRod","food":"Wonton soup","fruit":"Apricot","number":28},"gender":"Female","givenName":"Danielle","healthInfo":{"bloodType":"AB+","height":149,"weight":54},"home":{"address":"Parker Fork","city":"Cork","country":"Russia","postalCode":"401 63","state":"Washington","street":"Saffron Route"},"language":"Awngi","surname":"Mitchell"},"version":592,"work":{"company":"Maturation Place","contacts":[{"text":"danielle.mitchell@maturation_place.com","type":"email"},{"text":"051-7821859959","type":"phone"},{"text":"017-6403511604","type":"mobilePhone"}],"jobTitle":"Dancer","role":"Employee","team":"Frontend","token":"d229037e-1583-4fda-9001-8c6617057b88","usageLocation":"Toli'ara","z1":"a\\"b","z2":"a'b","z2":"'a","z3":"\\"a"}}
//            """);
//			Document doc = Document.of("""
//              {"policy_number":5143215528,"policy_holder_first_name":"Kiah","policy_holder_last_name":"Chaldecott","policy_holder_email":"vaskew0@topsy.com","policy_holder_phone":"128-457-2067","policy_holder_address":"8482 Vahlen Lane","policy_holder_city":"Marks","policy_holder_state":null,"policy_holder_postal_code":"413093","policy_holder_country":"Russia","insured_first_name":"Van","insured_last_name":"Askew","insured_email":"vaskew0@jugem.jp","insured_phone":"834-989-8609","insured_address":"762 Gateway Terrace","insured_city":"El Retorno","insured_state":null,"insured_postal_code":"951017","insured_country":"Colombia","policy_start_date":"11/21/2022","policy_end_date":"1/3/2023","premium_amount":8760.6,"deductible_amount":2966.72,"coverage_limit":573501.55,"policy_type":"Auto","insurance_company":"XYZ Insurance","agent_name":"Van Askew","agent_email":"vaskew0@baidu.com","agent_phone":"554-915-9262","claim_status":"Approved","claim_amount":3528.34,"claim_date":"7/24/2022"}
//              """);
//			Document doc = Document.of("""
//              {"id":"0001","type":"donut","name":"Cake","ppu":0.55,"batters":{"batter":[{"id":"1001","type":"Regular"},{"id":"1002","type":"Chocolate"},{"id":"1003","type":"Blueberry"},{"id":"1004","type":"Devil's Food"}]},"topping":[{"id":"5001","type":"None"},{"id":"5002","type":"Glazed"},{"id":"5005","type":"Sugar"},{"id":"5007","type":"Powdered Sugar"},{"id":"5006","type":"Chocolate with Sprinkles"},{"id":"5003","type":"Chocolate"},{"id":"5004","type":"Maple"}]}
//            """);

//			Document doc = new Document().fromJson("{\"_id\":ObjectId(65ce2f9bcfa1e6cc9cd9baa2),\"changeDateTime\":LocalDateTime(2009-02-06T15:42:34),\"createDateTime\":LocalDateTime(2014-05-15T22:58:28),\"locationHistory\":[{\"lat\":41.701477,\"lng\":79.05859,\"time\":OffsetDateTime(2014-01-25T15:03:07-03:00)},{\"lat\":57.318306,\"lng\":56.736435,\"time\":OffsetDateTime(2013-10-03T17:05:10-01:00)},{\"lat\":38.590572,\"lng\":12.31474,\"time\":OffsetDateTime(2010-05-01T16:21:19+04:00)}],\"personal\":{\"account_balance\":BigDecimal(5433.112382065),\"birthday\":LocalDate(1976-06-17),\"contacts\":[{\"text\":\"lanwatan_bregol2@yahoo.com\",\"type\":\"email\"},{\"text\":\"050-6359054507\",\"type\":\"phone\"},{\"text\":\"077-2080723656\",\"type\":\"mobilePhone\"}],\"displayName\":\"laurealasso_amdirthorn99\",\"favorite\":{\"color\":\"DarkGoldenRod\",\"food\":\"Wonton soup\",\"fruit\":\"Apricot\",\"number\":28},\"gender\":\"Female\",\"givenName\":\"Danielle\",\"healthInfo\":{\"bloodType\":\"AB+\",\"height\":149,\"weight\":54},\"home\":{\"address\":\"Parker Fork\",\"city\":\"Cork\",\"country\":\"Russia\",\"postalCode\":\"401 63\",\"state\":\"Washington\",\"street\":\"Saffron Route\"},\"language\":\"Awngi\",\"surname\":\"Mitchell\"},\"version\":592,\"work\":{\"company\":\"Maturation Place\",\"contacts\":[{\"text\":\"danielle.mitchell@maturation_place.com\",\"type\":\"email\"},{\"text\":\"051-7821859959\",\"type\":\"phone\"},{\"text\":\"017-6403511604\",\"type\":\"mobilePhone\"}],\"jobTitle\":\"Dancer\",\"role\":\"Employee\",\"team\":\"Frontend\",\"token\":UUID(ceaf5858-1be0-4d03-8b27-809e9398b1e0),\"usageLocation\":\"Toliara\"}}");

//			System.out.println(doc.toJson(false));
//
//			System.out.println("-".repeat(100));
//			System.out.println(doc.toYml());
//			System.out.println(doc.toJson(false));

			Document doc = new Document().fromJson("a:1,b:test,c:[{d:2,e:[{f:3,g:4},{h:5,i:6}]},{d:7,e:[{f:8,g:9},{h:10,i:11}]},{d:12,e:[{f:13,g:14},{h:15,i:[16,17]}]}]");
//			System.out.println(doc.toJson(false));

			Array arr = doc.findMany("c/e/i");
			System.out.println(arr);

			StreamMarshaller stream = new StreamMarshaller(new ByteArrayInputStream(doc.toByteArray()));
			System.out.println("" + stream.read());

			hexDump(doc.toByteArray());
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
