package com.qa.api.contacts.tests;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.qa.api.base.BaseTest;
import com.qa.api.constants.AuthType;
import com.qa.api.pojo.ContactsCredentials;
import com.qa.api.utils.StringUtils;
import com.qa.gorest.manager.ConfigManager;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class CreateAContactTest extends BaseTest {

	private String tokenID;

	@BeforeMethod
	public void getToken() {
		ContactsCredentials credentials = ContactsCredentials.builder()
				.email("Test_user@email.com")
				.password("Test123")
				.build();

		Response response = restClient.post(BASE_URL_CONTACTS, CONTACTS_LOGIN_ENDPOINT, credentials, null, null, AuthType.NO_AUTH, ContentType.JSON);
		tokenID = response.jsonPath().getString("token");
		Assert.assertNotNull(tokenID, "Login token must not be null");
		System.out.println("contacts login token ------->" + tokenID);
		ConfigManager.setProperty("Bearertoken", tokenID);
	}

	@Test
	public void createAContactPositiveTest() {
		// positive: create a contact with valid payload -> 201, returns an _id
		// NOTE: the Contact List delete returns 200 (framework delete spec is 204), so the
		// cleanup delete is issued directly to keep the framework untouched.
		String email = StringUtils.getRandomEmailId();
		Response response = RestAssured.given()
				.baseUri(BASE_URL_CONTACTS)
				.header("Authorization", "Bearer " + tokenID)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body("{\"firstName\":\"Yasir\",\"lastName\":\"Lava\",\"email\":\"" + email + "\"}")
				.when()
				.post(CONTACTS_ENDPOINT);

		Assert.assertEquals(response.statusCode(), 201);
		Assert.assertEquals(response.jsonPath().getString("firstName"), "Yasir");
		Assert.assertEquals(response.jsonPath().getString("lastName"), "Lava");
		Assert.assertEquals(response.jsonPath().getString("email"), email);
		Assert.assertNotNull(response.jsonPath().getString("_id"));

		// cleanup - delete the created contact (API returns 200 on delete)
		String contactId = response.jsonPath().getString("_id");
		Response deleteResponse = RestAssured.given()
				.baseUri(BASE_URL_CONTACTS)
				.header("Authorization", "Bearer " + tokenID)
				.accept(ContentType.JSON)
				.when()
				.delete("/contacts/" + contactId);
		Assert.assertEquals(deleteResponse.statusCode(), 200);
	}

	@Test
	public void createContactWithMissingLastName_Expects400() {
		// negative: missing required lastName -> 400 validation error
		Response response = RestAssured.given()
				.baseUri(BASE_URL_CONTACTS)
				.header("Authorization", "Bearer " + tokenID)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body("{\"firstName\":\"Yasir\",\"email\":\"" + StringUtils.getRandomEmailId() + "\"}")
				.when()
				.post(CONTACTS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 400);
	}

	@Test
	public void createAContactWithInvalidEmail_Expects400() {
		// negative: invalid email format -> 400 validation error
		Response response = RestAssured.given()
				.baseUri(BASE_URL_CONTACTS)
				.header("Authorization", "Bearer " + tokenID)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body("{\"firstName\":\"Yasir\",\"lastName\":\"Lava\",\"email\":\"not-an-email\"}")
				.when()
				.post(CONTACTS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 400);
	}

	@Test
	public void createAContactWithoutToken_Expects401() {
		// negative: no bearer token -> 401
		Response response = RestAssured.given()
				.baseUri(BASE_URL_CONTACTS)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body("{\"firstName\":\"Yasir\",\"lastName\":\"Lava\",\"email\":\"" + StringUtils.getRandomEmailId() + "\"}")
				.when()
				.post(CONTACTS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 401);
	}

	@Test
	public void createGetDeleteContactE2ETest() {
		// E2E: login -> create contact -> GET the single contact -> DELETE it -> verify gone (404)
		String email = StringUtils.getRandomEmailId();
		Response createResponse = RestAssured.given()
				.baseUri(BASE_URL_CONTACTS)
				.header("Authorization", "Bearer " + tokenID)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body("{\"firstName\":\"E2E\",\"lastName\":\"Contact\",\"email\":\"" + email + "\"}")
				.when()
				.post(CONTACTS_ENDPOINT);
		Assert.assertEquals(createResponse.statusCode(), 201);
		String contactId = createResponse.jsonPath().getString("_id");
		Assert.assertNotNull(contactId);

		// GET the single contact - framework get spec 200/401 covers the 200
		Response getResponse = restClient.get(BASE_URL_CONTACTS, "/contacts/" + contactId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(getResponse.statusCode(), 200);
		Assert.assertEquals(getResponse.jsonPath().getString("_id"), contactId);

		// DELETE - contact-list API returns 200 for delete (framework spec is 204) -> direct call
		Response deleteResponse = RestAssured.given()
				.baseUri(BASE_URL_CONTACTS)
				.header("Authorization", "Bearer " + tokenID)
				.accept(ContentType.JSON)
				.when()
				.delete("/contacts/" + contactId);
		Assert.assertEquals(deleteResponse.statusCode(), 200);

		// verify it is gone -> GET returns 404
		Response verifyGone = RestAssured.given()
				.baseUri(BASE_URL_CONTACTS)
				.header("Authorization", "Bearer " + tokenID)
				.accept(ContentType.JSON)
				.when()
				.get("/contacts/" + contactId);
		Assert.assertEquals(verifyGone.statusCode(), 404);
	}

	/**
	 * Negative: empty {@code firstName} is treated as missing by the validator
	 * and must return 400.
	 */
	@Test
	public void createContactWithEmptyFirstName_Expects400() {
		Response response = RestAssured.given()
				.baseUri(BASE_URL_CONTACTS)
				.header("Authorization", "Bearer " + tokenID)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body("{\"firstName\":\"\",\"lastName\":\"Lava\",\"email\":\"" + StringUtils.getRandomEmailId() + "\"}")
				.when()
				.post(CONTACTS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 400);
	}

	/**
	 * Negative: empty {@code lastName} must return 400.
	 */
	@Test
	public void createContactWithEmptyLastName_Expects400() {
		Response response = RestAssured.given()
				.baseUri(BASE_URL_CONTACTS)
				.header("Authorization", "Bearer " + tokenID)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body("{\"firstName\":\"Yasir\",\"lastName\":\"\",\"email\":\"" + StringUtils.getRandomEmailId() + "\"}")
				.when()
				.post(CONTACTS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 400);
	}

	/**
	 * Negative: empty {@code email} must return 400 (validator: "Email is invalid").
	 */
	@Test
	public void createContactWithEmptyEmail_Expects400() {
		Response response = RestAssured.given()
				.baseUri(BASE_URL_CONTACTS)
				.header("Authorization", "Bearer " + tokenID)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body("{\"firstName\":\"Yasir\",\"lastName\":\"Lava\",\"email\":\"\"}")
				.when()
				.post(CONTACTS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 400);
	}

	/**
	 * Negative: malformed/short JSON must yield 400 (the server cannot parse it).
	 */
	@Test
	public void createContactWithMalformedJson_Expects400() {
		Response response = RestAssured.given()
				.baseUri(BASE_URL_CONTACTS)
				.header("Authorization", "Bearer " + tokenID)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body("{\"firstName\":\"X\"")
				.when()
				.post(CONTACTS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 400);
	}

	/**
	 * Edge: extra unknown fields are ignored by the API and the contact is
	 * still created (201). Cleans up.
	 */
	@Test
	public void createContactWithExtraUnknownField_Expects201() {
		String email = StringUtils.getRandomEmailId();
		Response response = RestAssured.given()
				.baseUri(BASE_URL_CONTACTS)
				.header("Authorization", "Bearer " + tokenID)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body("{\"firstName\":\"Yasir\",\"lastName\":\"Lava\",\"email\":\"" + email + "\",\"junkField\":\"ignored\"}")
				.when()
				.post(CONTACTS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 201);
		String contactId = response.jsonPath().getString("_id");
		RestAssured.given().baseUri(BASE_URL_CONTACTS).header("Authorization", "Bearer " + tokenID)
				.accept(ContentType.JSON).when().delete("/contacts/" + contactId);
	}

	/**
	 * Edge: optional fields ({@code birthdate}, {@code phone}, {@code city},
	 * {@code state}, {@code country}) must round-trip through the response.
	 * Cleans up.
	 */
	@Test
	public void createContactWithOptionalFields_Expects201() {
		String email = StringUtils.getRandomEmailId();
		Response response = RestAssured.given()
				.baseUri(BASE_URL_CONTACTS)
				.header("Authorization", "Bearer " + tokenID)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body("{\"firstName\":\"Opt\",\"lastName\":\"Fields\",\"email\":\"" + email
						+ "\",\"birthdate\":\"1990-01-01\",\"phone\":\"8005551234\",\"city\":\"London\",\"state\":\"LDN\",\"country\":\"UK\"}")
				.when()
				.post(CONTACTS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 201);
		Assert.assertEquals(response.jsonPath().getString("city"), "London");
		Assert.assertEquals(response.jsonPath().getString("country"), "UK");
		String contactId = response.jsonPath().getString("_id");
		RestAssured.given().baseUri(BASE_URL_CONTACTS).header("Authorization", "Bearer " + tokenID)
				.accept(ContentType.JSON).when().delete("/contacts/" + contactId);
	}

	/**
	 * Edge / i18n: unicode first/last name must be accepted. Cleans up.
	 */
	@Test
	public void createContactWithUnicodeName_Expects201() {
		String email = StringUtils.getRandomEmailId();
		Response response = RestAssured.given()
				.baseUri(BASE_URL_CONTACTS)
				.header("Authorization", "Bearer " + tokenID)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body("{\"firstName\":\"测试\",\"lastName\":\"用户\",\"email\":\"" + email + "\"}")
				.when()
				.post(CONTACTS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 201);
		Assert.assertEquals(response.jsonPath().getString("firstName"), "测试");
		String contactId = response.jsonPath().getString("_id");
		RestAssured.given().baseUri(BASE_URL_CONTACTS).header("Authorization", "Bearer " + tokenID)
				.accept(ContentType.JSON).when().delete("/contacts/" + contactId);
	}

	/**
	 * Boundary: very long email local-part (300 chars) must be rejected with
	 * 400 by the email validator.
	 */
	@Test
	public void createContactWithVeryLongEmail_Expects400() {
		String longLocal = new String(new char[300]).replace('\0', 'a');
		Response response = RestAssured.given()
				.baseUri(BASE_URL_CONTACTS)
				.header("Authorization", "Bearer " + tokenID)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body("{\"firstName\":\"Long\",\"lastName\":\"Email\",\"email\":\"" + longLocal + "@dummy.com\"}")
				.when()
				.post(CONTACTS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 400);
	}

	/**
	 * Positive: the {@code owner} field on a newly-created contact must equal
	 * the id of the authenticated user. Cleans up.
	 */
	@Test
	public void createContactOwnerFieldIsLoggedInUser() {
		// get the current user id by logging in once more and reading the body
		io.restassured.response.Response loginResp = restClient.post(BASE_URL_CONTACTS, CONTACTS_LOGIN_ENDPOINT,
				com.qa.api.pojo.ContactsCredentials.builder().email("Test_user@email.com").password("Test123").build(),
				null, null, AuthType.NO_AUTH, ContentType.JSON);
		String loggedInUserId = loginResp.jsonPath().getString("user._id");

		String email = StringUtils.getRandomEmailId();
		Response response = RestAssured.given()
				.baseUri(BASE_URL_CONTACTS)
				.header("Authorization", "Bearer " + tokenID)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body("{\"firstName\":\"Owner\",\"lastName\":\"Check\",\"email\":\"" + email + "\"}")
				.when()
				.post(CONTACTS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 201);
		Assert.assertEquals(response.jsonPath().getString("owner"), loggedInUserId,
				"owner must equal the logged-in user id");
		String contactId = response.jsonPath().getString("_id");
		RestAssured.given().baseUri(BASE_URL_CONTACTS).header("Authorization", "Bearer " + tokenID)
				.accept(ContentType.JSON).when().delete("/contacts/" + contactId);
	}
}
