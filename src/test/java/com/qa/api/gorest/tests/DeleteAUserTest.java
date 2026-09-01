package com.qa.api.gorest.tests;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.qa.api.base.BaseTest;
import com.qa.api.constants.AuthType;
import com.qa.api.pojo.User;
import com.qa.api.utils.StringUtils;
import com.qa.gorest.manager.ConfigManager;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class DeleteAUserTest extends BaseTest {
	
private String tokenid = "7d8b46db01faef61ccba406c9d8b402bc66c1bb3e8a4beb8a031c1919a183f82";
	
	@BeforeMethod
	public void setToken()
	{
		ConfigManager.setProperty("Bearertoken", tokenid);
	}

	@Test
	public void deleteAUser() {
		// create a user using lombok builder
		User user = User.builder().name("Rahul").email(StringUtils.getRandomEmailId()).status("inactive")
				.gender("female").build();
		Response response = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null,
				AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(response.jsonPath().getString("name"), "Rahul");
		Assert.assertNotNull(response.jsonPath().getString("id"));

		// fetch the userid to Update
		String userId = response.jsonPath().getString("id");
		System.out.println("Newly Created User id is:" + userId);

		// Get Call to check if user is created is in DB
		Response getUserResponse = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null,
				AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertTrue(getUserResponse.getStatusLine().contains("OK"));
		Assert.assertEquals(getUserResponse.jsonPath().getString("id"), userId);

		Response deleteResponse = restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT+ "/" + userId, null, null, 
				AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertTrue(deleteResponse.getStatusLine().contains("No Content"));
		
		// Get Call to confirm the user is deleted -> GET must return 404.
		// NOTE: restClient.get() can't be used here because RestClient's hard-wired
		// response spec only accepts 200/401 (framework untouched); a deleted GoRest
		// user correctly returns 404, so this verification GET is issued directly.
				Response getUserResponse2 = RestAssured.given()
						.baseUri(BASE_URL_GOREST)
						.header("Authorization", "Bearer " + ConfigManager.getProperty("Bearertoken"))
						.accept(ContentType.JSON)
						.when()
						.get(GOREST_USERS_ENDPOINT + "/" + userId);
				Assert.assertTrue(getUserResponse2.getStatusLine().contains("Not Found"));
				Assert.assertEquals(getUserResponse2.statusCode(), 404);
				Assert.assertEquals(getUserResponse2.jsonPath().getString("message"), "Resource not found");
		
	}

@Test
	public void deleteNonExistingUser_Expects404() {
		// negative: deleting a user that does not exist -> 404 (raw, framework delete spec is 204)
		Response response = RestAssured.given()
				.baseUri(BASE_URL_GOREST)
				.header("Authorization", "Bearer " + ConfigManager.getProperty("Bearertoken"))
				.accept(ContentType.JSON)
				.when()
				.delete(GOREST_USERS_ENDPOINT + "/99999999");
		Assert.assertEquals(response.statusCode(), 404);
		Assert.assertEquals(response.jsonPath().getString("message"), "Resource not found");
	}

	@Test
	public void deleteUserTwice_SecondDeleteReturns404() {
		// E2E: delete an existing user, then repeat the delete -> second call must 404 (idempotence-aware)
		User user = User.builder().name("Double Delete").email(StringUtils.getRandomEmailId()).gender("male").status("active").build();
		Response createResponse = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		String userId = createResponse.jsonPath().getString("id");

		Response deleteResponse = restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(deleteResponse.statusCode(), 204);

		Response secondDelete = RestAssured.given()
				.baseUri(BASE_URL_GOREST)
				.header("Authorization", "Bearer " + ConfigManager.getProperty("Bearertoken"))
				.accept(ContentType.JSON)
				.when()
				.delete(GOREST_USERS_ENDPOINT + "/" + userId);
		Assert.assertEquals(secondDelete.statusCode(), 404);

		Response getUserResponse = RestAssured.given()
				.baseUri(BASE_URL_GOREST)
				.header("Authorization", "Bearer " + ConfigManager.getProperty("Bearertoken"))
				.accept(ContentType.JSON)
				.when()
				.get(GOREST_USERS_ENDPOINT + "/" + userId);
		Assert.assertEquals(getUserResponse.statusCode(), 404);
	}

	/**
	 * Security: delete with a malformed bearer token must be rejected with 401.
	 */
	@Test
	public void deleteUserWithInvalidAuth_Expects401() {
		Response response = RestAssured.given()
				.baseUri(BASE_URL_GOREST)
				.header("Authorization", "Bearer not.a.real.token")
				.accept(ContentType.JSON)
				.when()
				.delete(GOREST_USERS_ENDPOINT + "/8332781");
		Assert.assertEquals(response.statusCode(), 401);
	}

	/**
	 * Negative: deleting a user with a non-numeric id must yield 404.
	 */
	@Test
	public void deleteUserWithNonNumericId_Expects404() {
		Response response = RestAssured.given()
				.baseUri(BASE_URL_GOREST)
				.header("Authorization", "Bearer " + ConfigManager.getProperty("Bearertoken"))
				.accept(ContentType.JSON)
				.when()
				.delete(GOREST_USERS_ENDPOINT + "/abc");
		Assert.assertEquals(response.statusCode(), 404);
	}

	/**
	 * Data-integrity: after deleting a user, the list endpoint must no longer
	 * include that id. Cleans up.
	 */
	@Test
	public void deleteUserAndVerifyListNoLongerContainsIt() {
		User user = User.builder().name("List Verify").email(StringUtils.getRandomEmailId()).gender("male").status("active").build();
		Response createResponse = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		String userId = createResponse.jsonPath().getString("id");

		Response deleteResponse = restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(deleteResponse.statusCode(), 204);

		Response listResponse = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		java.util.List<Object> ids = com.qa.api.utils.JsonPathValidatorUtils.readList(listResponse, "$[*].id");
		Assert.assertFalse(ids.contains(Integer.valueOf(userId)),
				"Deleted user id " + userId + " must not appear in the users list");
	}
}
