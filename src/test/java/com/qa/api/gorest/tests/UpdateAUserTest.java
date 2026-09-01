package com.qa.api.gorest.tests;

import java.util.HashMap;
import java.util.Map;

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

public class UpdateAUserTest extends BaseTest
{
	
private String tokenid = "7d8b46db01faef61ccba406c9d8b402bc66c1bb3e8a4beb8a031c1919a183f82";
	
	@BeforeMethod
	public void setToken()
	{
		ConfigManager.setProperty("Bearertoken", tokenid);
	}
	
	
	@Test
	public void updateAUser()
	{
		//create a user using lombok builder 
		User user = User.builder()
				.name("Raviti Thaker")
				.email(StringUtils.getRandomEmailId())
				.status("active")
				.gender("female")
				.build();
		Response response =	restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(response.jsonPath().getString("name"), "Raviti Thaker");
		Assert.assertNotNull(response.jsonPath().getString("id"));
		
		
		
		//fetch the userid to Update
		String userId = response.jsonPath().getString("id");
		System.out.println("Newly Created User id is:"+userId);
		
		
		
		
		//Get Call to check if user is created is in DB
		Response getUserResponse = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT+"/"+userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertTrue(getUserResponse.getStatusLine().contains("OK"));
		Assert.assertEquals(getUserResponse.jsonPath().getString("id"), userId);
		
		
		
		
		// Finally put call to update user we are using name here with lambok set Method
		user.setName("Raviti Kapoor");
		user.setStatus("inactive");
		Response responsePut = restClient.put(BASE_URL_GOREST, GOREST_USERS_ENDPOINT+"/"+userId,user ,null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertTrue(responsePut.getStatusLine().contains("OK"));
		Assert.assertEquals(responsePut.jsonPath().getString("id"), userId);
		Assert.assertEquals(responsePut.jsonPath().getString("name"), "Raviti Kapoor");
		Assert.assertEquals(responsePut.jsonPath().getString("status"), "inactive");	
	}

	@Test
	public void updateUserViaPatchPositiveTest() {
		// positive: PATCH only one field (status) - partial update via framework patch() (spec 200/201)
		User user = User.builder().name("Patch User").email(StringUtils.getRandomEmailId()).gender("male").status("active").build();
		Response createResponse = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(createResponse.statusCode(), 201);
		String userId = createResponse.jsonPath().getString("id");

		Map<String, String> statusUpdate = new HashMap<String, String>();
		statusUpdate.put("status", "inactive");
		Response patchResponse = restClient.patch(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, statusUpdate, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(patchResponse.statusCode(), 200);
		Assert.assertEquals(patchResponse.jsonPath().getString("status"), "inactive");
		Assert.assertEquals(patchResponse.jsonPath().getString("name"), "Patch User", "PATCH should not touch other fields");

		// cleanup
		restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
	}

	@Test
	public void updateNonExistingUser_Expects404() {
		// negative: PUT on a user id that does not exist -> 404 (raw call, framework spec can't represent 404)
		User user = User.builder().name("Ghost").email(StringUtils.getRandomEmailId()).gender("male").status("active").build();
		Response response = RestAssured.given()
				.baseUri(BASE_URL_GOREST)
				.header("Authorization", "Bearer " + ConfigManager.getProperty("Bearertoken"))
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(user)
				.when()
				.put(GOREST_USERS_ENDPOINT + "/99999999");
		Assert.assertEquals(response.statusCode(), 404);
		Assert.assertEquals(response.jsonPath().getString("message"), "Resource not found");
	}

	@Test
	public void updateUserWithInvalidStatus_Expects422() {
		// negative: updating a user with an invalid enum value -> 422 validation error
		User user = User.builder().name("Enum User").email(StringUtils.getRandomEmailId()).gender("male").status("active").build();
		Response createResponse = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		String userId = createResponse.jsonPath().getString("id");

		User invalidUser = User.builder().name("Enum User").email(user.getEmail()).gender("male").status("invalid").build();
		Response response = RestAssured.given()
				.baseUri(BASE_URL_GOREST)
				.header("Authorization", "Bearer " + ConfigManager.getProperty("Bearertoken"))
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(invalidUser)
				.when()
				.put(GOREST_USERS_ENDPOINT + "/" + userId);
		Assert.assertEquals(response.statusCode(), 422);

		// cleanup
		restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
	}

	@Test
	public void createUpdateReadDeleteUserE2ETest() {
		// E2E: create -> read -> update (PUT) -> read back the update -> delete -> verify gone
		User user = User.builder().name("Lifecycle User").email(StringUtils.getRandomEmailId()).gender("female").status("active").build();
		Response createResponse = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(createResponse.statusCode(), 201);
		String userId = createResponse.jsonPath().getString("id");

		Response getResponse = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(getResponse.jsonPath().getString("status"), "active");

		user.setName("Lifecycle User Updated");
		user.setStatus("inactive");
		Response putResponse = restClient.put(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(putResponse.statusCode(), 200);
		Assert.assertEquals(putResponse.jsonPath().getString("name"), "Lifecycle User Updated");
		Assert.assertEquals(putResponse.jsonPath().getString("status"), "inactive");

		Response getUpdated = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(getUpdated.jsonPath().getString("status"), "inactive");

		Response deleteResponse = restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(deleteResponse.statusCode(), 204);
	}

	/**
	 * Edge: PATCH with only the {@code name} field must update name and leave
	 * gender and status untouched.
	 */
	@Test
	public void updateUserNameOnlyViaPatch_PreservesOtherFields() {
		User user = User.builder().name("Patch Name Only").email(StringUtils.getRandomEmailId()).gender("male").status("active").build();
		Response createResponse = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		String userId = createResponse.jsonPath().getString("id");
		try {
			Map<String, String> onlyName = new HashMap<>();
			onlyName.put("name", "Name Only Patched");
			Response patchResponse = restClient.patch(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, onlyName, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
			Assert.assertEquals(patchResponse.statusCode(), 200);
			Assert.assertEquals(patchResponse.jsonPath().getString("name"), "Name Only Patched");
			Assert.assertEquals(patchResponse.jsonPath().getString("gender"), "male", "PATCH must not touch gender");
			Assert.assertEquals(patchResponse.jsonPath().getString("status"), "active", "PATCH must not touch status");
		} finally {
			restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		}
	}

	/**
	 * Edge: PATCH only the {@code gender} field. Cleans up.
	 */
	@Test
	public void updateUserGenderViaPatch() {
		User user = User.builder().name("Patch Gender").email(StringUtils.getRandomEmailId()).gender("male").status("active").build();
		Response createResponse = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		String userId = createResponse.jsonPath().getString("id");
		try {
			Map<String, String> onlyGender = new HashMap<>();
			onlyGender.put("gender", "female");
			Response patchResponse = restClient.patch(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, onlyGender, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
			Assert.assertEquals(patchResponse.statusCode(), 200);
			Assert.assertEquals(patchResponse.jsonPath().getString("gender"), "female");
		} finally {
			restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		}
	}

	/**
	 * Edge: PATCH the email to a new random value. Cleans up.
	 */
	@Test
	public void updateUserEmailViaPatch_Expects200() {
		User user = User.builder().name("Patch Email").email(StringUtils.getRandomEmailId()).gender("male").status("active").build();
		Response createResponse = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		String userId = createResponse.jsonPath().getString("id");
		try {
			Map<String, String> onlyEmail = new HashMap<>();
			onlyEmail.put("email", StringUtils.getRandomEmailId());
			Response patchResponse = restClient.patch(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, onlyEmail, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
			Assert.assertEquals(patchResponse.statusCode(), 200);
			Assert.assertNotNull(patchResponse.jsonPath().getString("email"));
		} finally {
			restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		}
	}

	/**
	 * Boundary / idempotence: PUT the exact same body twice — both calls must
	 * succeed and the second must not regress the resource.
	 */
	@Test
	public void updateUserWithSamePayload_IsIdempotent() {
		User user = User.builder().name("Idempotent User").email(StringUtils.getRandomEmailId()).gender("male").status("active").build();
		Response createResponse = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		String userId = createResponse.jsonPath().getString("id");
		try {
			User payload = User.builder().name("Idempotent User").email(user.getEmail()).gender("male").status("active").build();
			Response first = restClient.put(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, payload, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
			Response second = restClient.put(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, payload, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
			Assert.assertEquals(first.statusCode(), 200);
			Assert.assertEquals(second.statusCode(), 200);
			Assert.assertEquals(second.jsonPath().getString("name"), "Idempotent User");
		} finally {
			restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		}
	}

	/**
	 * Negative: PUT with malformed email must yield 422. Cleans up.
	 */
	@Test
	public void updateUserWithInvalidEmail_Expects422() {
		User user = User.builder().name("Bad Email").email(StringUtils.getRandomEmailId()).gender("male").status("active").build();
		Response createResponse = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		String userId = createResponse.jsonPath().getString("id");
		try {
			User invalid = User.builder().name("Bad Email").email("not-a-valid-email").gender("male").status("active").build();
			Response response = RestAssured.given().baseUri(BASE_URL_GOREST)
					.header("Authorization", "Bearer " + ConfigManager.getProperty("Bearertoken"))
					.contentType(ContentType.JSON).accept(ContentType.JSON).body(invalid).when().put(GOREST_USERS_ENDPOINT + "/" + userId);
			Assert.assertEquals(response.statusCode(), 422);
			Assert.assertEquals(response.jsonPath().getString("[0].field"), "email");
		} finally {
			restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		}
	}

	/**
	 * Negative: PUT without auth must be rejected with 401.
	 */
	@Test
	public void updateUserWithoutAuthToken_Expects401() {
		User user = User.builder().name("No Auth").email(StringUtils.getRandomEmailId()).gender("male").status("active").build();
		Response response = RestAssured.given().baseUri(BASE_URL_GOREST)
				.contentType(ContentType.JSON).accept(ContentType.JSON).body(user).when().put(GOREST_USERS_ENDPOINT + "/8332781");
		Assert.assertEquals(response.statusCode(), 401);
	}

	/**
	 * Edge: PUT with an empty JSON body must not crash — GoRest treats missing
	 * fields as unchanged and returns 200.
	 */
	@Test
	public void updateUserWithEmptyBody_Expects200() {
		User user = User.builder().name("Empty Body").email(StringUtils.getRandomEmailId()).gender("male").status("active").build();
		Response createResponse = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		String userId = createResponse.jsonPath().getString("id");
		try {
			Response response = RestAssured.given().baseUri(BASE_URL_GOREST)
					.header("Authorization", "Bearer " + ConfigManager.getProperty("Bearertoken"))
					.contentType(ContentType.JSON).accept(ContentType.JSON).body("{}").when().put(GOREST_USERS_ENDPOINT + "/" + userId);
			Assert.assertEquals(response.statusCode(), 200);
			Assert.assertEquals(response.jsonPath().getString("name"), "Empty Body", "Empty PUT must not wipe the name");
		} finally {
			restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		}
	}

	/**
	 * Edge: PATCH email to one already taken by another user must yield 422
	 * with {@code field=email}. Cleans up both users.
	 */
	@Test
	public void updateUserEmailToDuplicate_Expects422() {
		String sharedEmail = StringUtils.getRandomEmailId();
		// UUID-based unique email avoids ms-resolution collisions in StringUtils.getRandomEmailId()
		String uniqueB = "testing" + java.util.UUID.randomUUID().toString().replace("-", "") + "@dummy.com";
		User a = User.builder().name("Owner A").email(sharedEmail).gender("male").status("active").build();
		User b = User.builder().name("Owner B").email(uniqueB).gender("female").status("active").build();
		Response aCreate = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, a, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Response bCreate = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, b, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		String aId = aCreate.jsonPath().getString("id");
		String bId = bCreate.jsonPath().getString("id");
		try {
			Map<String, String> collide = new HashMap<>();
			collide.put("email", sharedEmail);
			// Direct call: framework patch() spec only accepts 200/201.
			Response response = RestAssured.given().baseUri(BASE_URL_GOREST)
					.header("Authorization", "Bearer " + ConfigManager.getProperty("Bearertoken"))
					.contentType(ContentType.JSON).accept(ContentType.JSON).body(collide)
					.when().patch(GOREST_USERS_ENDPOINT + "/" + bId);
			Assert.assertEquals(response.statusCode(), 422);
			Assert.assertEquals(response.jsonPath().getString("[0].field"), "email");
		} finally {
			restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + aId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
			restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + bId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		}
	}
}
