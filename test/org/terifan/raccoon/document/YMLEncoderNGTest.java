package org.terifan.raccoon.document;

import static org.testng.Assert.*;
import org.testng.annotations.Test;


public class YMLEncoderNGTest
{
	@Test
	public void testEncodingYml()
	{
		String expected =
			"_id: 0x65ce2f9bcfa1e6cc9cd9baa2 #ObjectId\n" +
			"changeDateTime: '2009-02-06T15:42:34' #LocalDateTime\n" +
			"createDateTime: '2014-05-15T22:58:28' #LocalDateTime\n" +
			"locationHistory:\n" +
			"- lat: 41.701477\n" +
			"  lng: 79.05859\n" +
			"  time: '2014-01-25T15:03:07-03:00' #OffsetDateTime\n" +
			"- lat: 57.318306\n" +
			"  lng: 56.736435\n" +
			"  time: '2013-10-03T17:05:10-01:00' #OffsetDateTime\n" +
			"- lat: 38.590572\n" +
			"  lng: 12.31474\n" +
			"  time: '2010-05-01T16:21:19+04:00' #OffsetDateTime\n" +
			"personal:\n" +
			"  account_balance: 5433.112382065 #BigDecimal\n" +
			"  birthday: '1976-06-17' #LocalDate\n" +
			"  contacts:\n" +
			"  - text: lanwatan_bregol2@yahoo.com\n" +
			"    type: email\n" +
			"  - text: 050-6359054507\n" +
			"    type: phone\n" +
			"  - text: 077-2080723656\n" +
			"    type: mobilePhone\n" +
			"  displayName: laurealasso_amdirthorn99\n" +
			"  favorite:\n" +
			"    color: DarkGoldenRod\n" +
			"    food: Wonton soup\n" +
			"    fruit: Apricot\n" +
			"    number: 28\n" +
			"  gender: Female\n" +
			"  givenName: Danielle\n" +
			"  healthInfo:\n" +
			"    bloodType: AB+\n" +
			"    height: 149\n" +
			"    weight: 54\n" +
			"  home:\n" +
			"    address: Parker Fork\n" +
			"    city: Cork\n" +
			"    country: Russia\n" +
			"    postalCode: 401 63\n" +
			"    state: Washington\n" +
			"    street: Saffron Route\n" +
			"  language: Awngi\n" +
			"  surname: Mitchell\n" +
			"version: 592\n" +
			"work:\n" +
			"  company: Maturation Place\n" +
			"  contacts:\n" +
			"  - text: danielle.mitchell@maturation_place.com\n" +
			"    type: email\n" +
			"  - text: 051-7821859959\n" +
			"    type: phone\n" +
			"  - text: 017-6403511604\n" +
			"    type: mobilePhone\n" +
			"  jobTitle: Dancer\n" +
			"  role: Employee\n" +
			"  team: Frontend\n" +
			"  token: 'ceaf5858-1be0-4d03-8b27-809e9398b1e0' #UUID\n" +
			"  usageLocation: Toliara";

		Document doc = new Document().fromJson("{\"_id\":ObjectId(65ce2f9bcfa1e6cc9cd9baa2),\"changeDateTime\":LocalDateTime(2009-02-06T15:42:34),\"createDateTime\":LocalDateTime(2014-05-15T22:58:28),\"locationHistory\":[{\"lat\":41.701477,\"lng\":79.05859,\"time\":OffsetDateTime(2014-01-25T15:03:07-03:00)},{\"lat\":57.318306,\"lng\":56.736435,\"time\":OffsetDateTime(2013-10-03T17:05:10-01:00)},{\"lat\":38.590572,\"lng\":12.31474,\"time\":OffsetDateTime(2010-05-01T16:21:19+04:00)}],\"personal\":{\"account_balance\":BigDecimal(5433.112382065),\"birthday\":LocalDate(1976-06-17),\"contacts\":[{\"text\":\"lanwatan_bregol2@yahoo.com\",\"type\":\"email\"},{\"text\":\"050-6359054507\",\"type\":\"phone\"},{\"text\":\"077-2080723656\",\"type\":\"mobilePhone\"}],\"displayName\":\"laurealasso_amdirthorn99\",\"favorite\":{\"color\":\"DarkGoldenRod\",\"food\":\"Wonton soup\",\"fruit\":\"Apricot\",\"number\":28},\"gender\":\"Female\",\"givenName\":\"Danielle\",\"healthInfo\":{\"bloodType\":\"AB+\",\"height\":149,\"weight\":54},\"home\":{\"address\":\"Parker Fork\",\"city\":\"Cork\",\"country\":\"Russia\",\"postalCode\":\"401 63\",\"state\":\"Washington\",\"street\":\"Saffron Route\"},\"language\":\"Awngi\",\"surname\":\"Mitchell\"},\"version\":592,\"work\":{\"company\":\"Maturation Place\",\"contacts\":[{\"text\":\"danielle.mitchell@maturation_place.com\",\"type\":\"email\"},{\"text\":\"051-7821859959\",\"type\":\"phone\"},{\"text\":\"017-6403511604\",\"type\":\"mobilePhone\"}],\"jobTitle\":\"Dancer\",\"role\":\"Employee\",\"team\":\"Frontend\",\"token\":UUID(ceaf5858-1be0-4d03-8b27-809e9398b1e0),\"usageLocation\":\"Toliara\"}}");
		String actual = doc.toYml();

		assertEquals(actual, expected);
	}
}
