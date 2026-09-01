package com.qa.api.contacts.tests;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.qa.api.base.BaseTest;
import com.qa.api.constants.AuthType;
import com.qa.api.pojo.ContactsCredentials;
import com.qa.api.utils.StringUtils;
import com.qa.gorest.manager.ConfigManager;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class GetContactsAPITests extends BaseTest

{
	
	private String tokenID;
	
	@BeforeMethod
	public void getToken()
	{
	   ContactsCredentials credentials = ContactsCredentials.builder()
			   .email("Test_user@email.com")
			   .password("Test123")
			   .build();
	   
	   
	  Response response = restClient.post(BASE_URL_CONTACTS, CONTACTS_LOGIN_ENDPOINT, credentials, null, null, AuthType.NO_AUTH, ContentType.JSON);
	  		tokenID = response.jsonPath().getString("token");
	  		System.out.println("contacts login token ------->"+ tokenID);
	  		ConfigManager.setProperty("Bearertoken", tokenID);
	}
	
	@Test
	public void getAllContactsTest()
	{
		restClient.get(BASE_URL_CONTACTS, CONTACTS_ENDPOINT, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		
		
	}
@Test
	public void getAllContactsWithValidTokenPositiveTest() {
		// positive: with a valid token the contacts list returns 200 and a JSON array
		Response response = restClient.get(BASE_URL_CONTACTS, CONTACTS_ENDPOINT, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(response.statusCode(), 200);
		List<Map<String, Object>> contacts = com.qa.api.utils.JsonPathValidatorUtils.readListOfMaps(response, "$.[*]");
		for (Map<String, Object> contact : contacts) {
			Assert.assertNotNull(contact.get("_id"), "Each contact must have an _id");
			Assert.assertNotNull(contact.get("firstName"), "Each contact must have a firstName");
		}
	}

	@Test
	public void getContactsWithoutToken_Expects401() {
		// negative: calling the contacts endpoint without a bearer token -> 401
		Response response = RestAssured.given()
				.baseUri(BASE_URL_CONTACTS)
				.accept(ContentType.JSON)
				.when()
				.get(CONTACTS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 401);
	}

	@Test
	public void getContactsWithInvalidToken_Expects401() {
		// negative: calling the contacts endpoint with a garbage token -> 401
		Response response = RestAssured.given()
				.baseUri(BASE_URL_CONTACTS)
				.header("Authorization", "Bearer this.is.not.a.valid.token")
				.accept(ContentType.JSON)
				.when()
				.get(CONTACTS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 401);
	}

	@Test
	public void createAndFetchContactsE2ETest() {
		// E2E: create a contact -> the list must contain it -> delete it -> list must not contain it
		String email = StringUtils.getRandomEmailId();

		// create the contact (POST returns 201)
		Response createResponse = RestAssured.given()
				.baseUri(BASE_URL_CONTACTS)
				.header("Authorization", "Bearer " + tokenID)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body("{\"firstName\":\"E2EList\",\"lastName\":\"Check\",\"email\":\"" + email + "\"}")
				.when()
				.post(CONTACTS_ENDPOINT);
		Assert.assertEquals(createResponse.statusCode(), 201);
		String contactId = createResponse.jsonPath().getString("_id");

		// the full list must contain the newly created contact id
		Response listResponse = restClient.get(BASE_URL_CONTACTS, CONTACTS_ENDPOINT, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(listResponse.statusCode(), 200);
		List<Map<String, Object>> contacts = com.qa.api.utils.JsonPathValidatorUtils.readListOfMaps(listResponse, "$.[*]");
		boolean found = false;
		for (Map<String, Object> contact : contacts) {
			if (contactId.equals(contact.get("_id"))) {
				found = true;
				break;
			}
		}
		Assert.assertTrue(found, "The created contact must be present in the contacts list");

		// cleanup - delete the contact (API returns 200 for delete)
		Response deleteResponse = RestAssured.given()
				.baseUri(BASE_URL_CONTACTS)
				.header("Authorization", "Bearer " + tokenID)
				.accept(ContentType.JSON)
				.when()
				.delete("/contacts/" + contactId);
		Assert.assertEquals(deleteResponse.statusCode(), 200);

		// verify it is gone from the list
		Response listAfterDelete = restClient.get(BASE_URL_CONTACTS, CONTACTS_ENDPOINT, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		List<Map<String, Object>> contactsAfter = com.qa.api.utils.JsonPathValidatorUtils.readListOfMaps(listAfterDelete, "$.[*]");
		for (Map<String, Object> contact : contactsAfter) {
			Assert.assertNotEquals(contactId, contact.get("_id"), "Deleted contact must not appear in the list");
		}
	}

	/**
	 * Boundary: Contact List API does not paginate — the response is a single
	 * JSON array. Verifying this guards against accidental server-side
	 * pagination changes.
	 */
	@Test
	public void getContactsListIsSingleArray() {
		Response response = restClient.get(BASE_URL_CONTACTS, CONTACTS_ENDPOINT, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(response.statusCode(), 200);
		List<Map<String, Object>> contacts = com.qa.api.utils.JsonPathValidatorUtils.readListOfMaps(response, "$[*]");
		Assert.assertNotNull(contacts, "Contacts list must be a JSON array");
	}

	/**
	 * Positive: GET an existing contact by its id must return 200 with all
	 * fields populated. Cleans up.
	 */
	@Test
	public void getSingleContactByIdPositiveTest() {
		String email = StringUtils.getRandomEmailId();
		Response createResponse = RestAssured.given().baseUri(BASE_URL_CONTACTS)
				.header("Authorization", "Bearer " + tokenID)
				.contentType(ContentType.JSON).accept(ContentType.JSON)
				.body("{\"firstName\":\"Single\",\"lastName\":\"Get\",\"email\":\"" + email + "\"}")
				.when().post(CONTACTS_ENDPOINT);
		String contactId = createResponse.jsonPath().getString("_id");
		try {
			Response response = restClient.get(BASE_URL_CONTACTS, "/contacts/" + contactId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
			Assert.assertEquals(response.statusCode(), 200);
			Assert.assertEquals(response.jsonPath().getString("_id"), contactId);
			Assert.assertEquals(response.jsonPath().getString("firstName"), "Single");
		} finally {
			RestAssured.given().baseUri(BASE_URL_CONTACTS).header("Authorization", "Bearer " + tokenID)
					.accept(ContentType.JSON).when().delete("/contacts/" + contactId);
		}
	}

	/**
	 * Negative: GET a non-existing contact id must return 404.
	 */
	@Test
	public void getSingleContactNonExistingId_Expects404() {
		Response response = RestAssured.given().baseUri(BASE_URL_CONTACTS)
				.header("Authorization", "Bearer " + tokenID).accept(ContentType.JSON)
				.when().get("/contacts/000000000000000000000000");
		Assert.assertEquals(response.statusCode(), 404);
	}

	/**
	 * Session: the same bearer token must work for multiple sequential GETs
	 * without re-login.
	 */
	@Test
	public void getContactsTokenPersistsAcrossCalls() {
		for (int i = 0; i < 3; i++) {
			Response response = restClient.get(BASE_URL_CONTACTS, CONTACTS_ENDPOINT, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
			Assert.assertEquals(response.statusCode(), 200,
					"Token must remain valid on call " + (i + 1));
		}
	}

	/**
	 * Security: lowercase {@code bearer} scheme header must be rejected with
	 * 401 (the API only accepts {@code Bearer}).
	 */
	@Test
	public void getContactsWithLowercaseBearer_Expects401() {
		Response response = RestAssured.given().baseUri(BASE_URL_CONTACTS)
				.header("Authorization", "bearer " + tokenID).accept(ContentType.JSON)
				.when().get(CONTACTS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 401);
	}

	/**
	 * Edge: an obviously-invalid JWT must be rejected with 401.
	 */
	@Test
	public void getContactsWithGarbageToken_Expects401() {
		Response response = RestAssured.given().baseUri(BASE_URL_CONTACTS)
				.header("Authorization", "Bearer aaa.bbb.ccc").accept(ContentType.JSON)
				.when().get(CONTACTS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 401);
	}
}
