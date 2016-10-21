package uk.gov.justice.payment.api.integration;

import static com.jayway.restassured.RestAssured.given;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jayway.restassured.response.Response;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PaymentsKeysMissingValidation extends TestBase {

	@Before
	public void setup() throws IOException {

		initialize();

	}

	@Test
	public void test1_POST_Empty_Json() throws IOException {

		String URL = CONFIG.getProperty("baseURL") + CONFIG.getProperty("payments_path");

		String formatted_json = formatedJson("", "", "", "", "", "", "", "", "", "", "", "", "", "");

		Response r = given().contentType("application/json").body(formatted_json).when().post(URL);
		Assert.assertEquals(400, r.statusCode());

	}

	@Test
	public void test2_POST_Without_Amount_Attribute() throws IOException {

		String URL = CONFIG.getProperty("baseURL") + CONFIG.getProperty("payments_path");

		String formatted_json = formatedJson("", "", CONFIG.getProperty("k_application_reference"),
				CONFIG.getProperty("application_ref"), CONFIG.getProperty("k_description"),
				CONFIG.getProperty("description"), CONFIG.getProperty("k_email"), CONFIG.getProperty("email"),
				CONFIG.getProperty("k_payment_reference"), CONFIG.getProperty("payment_ref"),
				CONFIG.getProperty("k_return_url"), CONFIG.getProperty("ret_url"), CONFIG.getProperty("k_service_id"),
				CONFIG.getProperty("service_id"));

		Response r = given().contentType("application/json").body(formatted_json).when().post(URL);
		Assert.assertEquals(400, r.statusCode());
		Assert.assertEquals(CONFIG.getProperty("amount_man_msg"), r.path("error"));

	}

	@Test
	public void test3_POST_Without_App_Ref_Attribute() throws IOException {

		String URL = CONFIG.getProperty("baseURL") + CONFIG.getProperty("payments_path");

		String formatted_json = formatedJson(CONFIG.getProperty("k_amount"), CONFIG.getProperty("amount"), "", "",
				CONFIG.getProperty("k_description"), CONFIG.getProperty("description"), CONFIG.getProperty("k_email"),
				CONFIG.getProperty("email"), CONFIG.getProperty("k_payment_reference"),
				CONFIG.getProperty("payment_ref"), CONFIG.getProperty("k_return_url"), CONFIG.getProperty("ret_url"),
				CONFIG.getProperty("k_service_id"), CONFIG.getProperty("service_id"));

		Response r = given().contentType("application/json").body(formatted_json).when().post(URL);
		Assert.assertEquals(422, r.statusCode());
		Assert.assertEquals(CONFIG.getProperty("application_man_msg"), r.body().asString());

	}

	@Test
	public void test4_POST_Without_Desc_Attribute() throws IOException {

		String URL = CONFIG.getProperty("baseURL") + CONFIG.getProperty("payments_path");

		String formatted_json = formatedJson(CONFIG.getProperty("k_amount"), CONFIG.getProperty("amount"),
				CONFIG.getProperty("k_application_reference"), CONFIG.getProperty("application_ref"), "", "",
				CONFIG.getProperty("k_email"), CONFIG.getProperty("email"), CONFIG.getProperty("k_payment_reference"),
				CONFIG.getProperty("payment_ref"), CONFIG.getProperty("k_return_url"), CONFIG.getProperty("ret_url"),
				CONFIG.getProperty("k_service_id"), CONFIG.getProperty("service_id"));

		Response r = given().contentType("application/json").body(formatted_json).when().post(URL);
		Assert.assertEquals(422, r.statusCode());
		Assert.assertEquals(CONFIG.getProperty("description_man_msg"), r.getBody().asString());

	}

	@Test
	public void test5_POST_Without_Pay_Ref_Attribute() throws IOException {

		String URL = CONFIG.getProperty("baseURL") + CONFIG.getProperty("payments_path");

		String formatted_json = formatedJson(CONFIG.getProperty("k_amount"), CONFIG.getProperty("amount"),
				CONFIG.getProperty("k_application_reference"), CONFIG.getProperty("application_ref"),
				CONFIG.getProperty("k_description"), CONFIG.getProperty("description"), CONFIG.getProperty("k_email"),
				CONFIG.getProperty("email"), "", "", CONFIG.getProperty("k_return_url"), CONFIG.getProperty("ret_url"),
				CONFIG.getProperty("k_service_id"), CONFIG.getProperty("service_id"));

		Response r = given().contentType("application/json").body(formatted_json).when().post(URL);
		Assert.assertEquals(422, r.statusCode());
		Assert.assertEquals(CONFIG.getProperty("payment_ref_man_msg"), r.getBody().asString());

	}

	@Test
	public void test6_POST_Without_Ret_Url_Attribute() throws IOException {

		String URL = CONFIG.getProperty("baseURL") + CONFIG.getProperty("payments_path");

		String formatted_json = formatedJson(CONFIG.getProperty("k_amount"), CONFIG.getProperty("amount"),
				CONFIG.getProperty("k_application_reference"), CONFIG.getProperty("application_ref"),
				CONFIG.getProperty("k_description"), CONFIG.getProperty("description"), CONFIG.getProperty("k_email"),
				CONFIG.getProperty("email"), CONFIG.getProperty("k_payment_reference"),
				CONFIG.getProperty("payment_ref"), "", "", CONFIG.getProperty("k_service_id"),
				CONFIG.getProperty("service_id"));

		Response r = given().contentType("application/json").body(formatted_json).when().post(URL);
		Assert.assertEquals(422, r.statusCode());
		Assert.assertEquals(CONFIG.getProperty("ret_url_man_msg"), r.getBody().asString());

	}

	@Test
	public void test7_POST_Without_Ser_Id_Attribute() throws IOException {

		String URL = CONFIG.getProperty("baseURL") + CONFIG.getProperty("payments_path");

		String formatted_json = formatedJson(CONFIG.getProperty("k_amount"), CONFIG.getProperty("amount"),
				CONFIG.getProperty("k_application_reference"), CONFIG.getProperty("application_ref"),
				CONFIG.getProperty("k_description"), CONFIG.getProperty("description"), CONFIG.getProperty("k_email"),
				CONFIG.getProperty("email"), CONFIG.getProperty("k_payment_reference"),
				CONFIG.getProperty("payment_ref"), CONFIG.getProperty("k_return_url"), CONFIG.getProperty("ret_url"),
				"", "");

		Response r = given().contentType("application/json").body(formatted_json).when().post(URL);
		Assert.assertEquals(422, r.statusCode());
		Assert.assertEquals(CONFIG.getProperty("service_id_man_msg"), r.getBody().asString());

	}

	private String formatedJson(String k_amount, String amount, String k_application_ref, String application_ref,
			String k_description, String description, String k_email, String email, String k_payment_ref,
			String payment_ref, String k_ret_url, String ret_url, String k_service_id, String service_id)
			throws IOException {

		Map<String, String> m = new HashMap<String, String>();
		m.put("k_amount", k_amount);
		m.put("amount", amount);
		m.put("k_application_ref", k_application_ref);
		m.put("application_ref", application_ref);
		m.put("k_description", k_description);
		m.put("description", description);
		m.put("k_email", k_email);
		m.put("email", email);
		m.put("k_payment_ref", k_payment_ref);
		m.put("payment_ref", payment_ref);
		m.put("k_ret_url", k_ret_url);
		m.put("ret_url", ret_url);
		m.put("k_service_id", k_service_id);
		m.put("service_id", service_id);

		return getProcessedTemplateValue(loadFile("Message_Appeal_Post_Key_Validation.json"), m);
	}

}
