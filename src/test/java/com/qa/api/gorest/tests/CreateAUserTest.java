package com.qa.api.gorest.tests;

import java.io.File;
import java.util.HashMap;
import java.util.List;
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

public class CreateAUserTest extends BaseTest{
	
	private String tokenid = "7d8b46db01faef61ccba406c9d8b402bc66c1bb3e8a4beb8a031c1919a183f82";
	
	@BeforeMethod
	public void setToken()
	{
		ConfigManager.setProperty("Bearertoken", tokenid);
	}
	
	@Test
	public void createAUserTest() {
		User user = new User(null,"Yasir", StringUtils.getRandomEmailId(), "male", "active");
	Response response =	restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
	Assert.assertEquals(response.jsonPath().getString("name"), "Yasir");
	Assert.assertNotNull(response.jsonPath().getString("id"));
	Assert.assertEquals(response.jsonPath().getString("gender"), "male");
	Assert.assertEquals(response.jsonPath().getString("status"), "active");
	}
	
	
	//@Test
	public void createAUserTestWithStringBody() {
		String userJson = "	\"email\": \"dummyuser679@dummy.com\",\n"
				+ "	\"name\": \"Yasir Lava\",\n"
				+ "	\"gender\": \"male\",\n"
				+ "	\"status\": \"inactive\"";
		
	Response response =	restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, userJson, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
	Assert.assertEquals(response.jsonPath().getString("name"), "Yasir Lava");
	Assert.assertNotNull(response.jsonPath().getString("id"));
	Assert.assertEquals(response.jsonPath().getString("gender"), "male");
	Assert.assertEquals(response.jsonPath().getString("status"), "active");
	}
	
	public void createANewUserWithFile()
	{
		File userFile = new File("./src/test/resources/jsons/user.json");
		Response response =	restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, userFile, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(response.jsonPath().getString("name"), "Yasir Lava");
		Assert.assertNotNull(response.jsonPath().getString("id"));
		Assert.assertEquals(response.jsonPath().getString("gender"), "male");
		Assert.assertEquals(response.jsonPath().getString("status"), "active");
		
		
	}
	

@Test
	public void createAUserWithUserJsonFilePositiveTest() {
		// positive: create a user from a JSON file using the framework post() (spec 200/201)
		File userFile = new File("./src/test/resources/jsons/User.json");
		Response response = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, userFile, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(response.statusCode(), 201);
		Assert.assertEquals(response.jsonPath().getString("name"), "Yasir Lava");
		Assert.assertNotNull(response.jsonPath().getString("id"));
		Assert.assertEquals(response.jsonPath().getString("gender"), "male");

		// cleanup - delete the user created from the file so the suite is re-runnable
		String userId = response.jsonPath().getString("id");
		restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
	}

	@Test
	public void createUserWithInvalidEmail_Expects422() {
		// negative: invalid email must be rejected with 422 validation error
		// NOTE: restClient.post() can't represent 4xx codes (spec 200/201), so the
		// request is issued directly - framework untouched.
		User user = User.builder().name("Yasir").email("not-a-valid-email").gender("male").status("active").build();
		Response response = RestAssured.given()
				.baseUri(BASE_URL_GOREST)
				.header("Authorization", "Bearer " + ConfigManager.getProperty("Bearertoken"))
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(user)
				.when()
				.post(GOREST_USERS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 422);
		Assert.assertEquals(response.jsonPath().getString("[0].field"), "email");
	}

	@Test
	public void createUserWithoutAuthToken_Expects401() {
		// negative: missing bearer token must be rejected with 401
		User user = User.builder().name("Yasir").email(StringUtils.getRandomEmailId()).gender("male").status("active").build();
		Response response = RestAssured.given()
				.baseUri(BASE_URL_GOREST)
				.contentType(ContentType.JSON)
				.accept(ContentType.JSON)
				.body(user)
				.when()
				.post(GOREST_USERS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 401);
	}

	@Test
	public void createAndGetUserE2ETest() {
		// E2E: create -> read back -> delete (full lifecycle)
		User user = User.builder().name("E2E User").email(StringUtils.getRandomEmailId()).gender("male").status("active").build();
		Response createResponse = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(createResponse.statusCode(), 201);
		String userId = createResponse.jsonPath().getString("id");
		Assert.assertNotNull(userId);

		Response getUserResponse = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(getUserResponse.statusCode(), 200);
		Assert.assertEquals(getUserResponse.jsonPath().getString("id"), userId);
		Assert.assertEquals(getUserResponse.jsonPath().getString("name"), "E2E User");

		// cleanup
		Response deleteResponse = restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(deleteResponse.statusCode(), 204);
	}

	/**
	 * Negative: missing required {@code name} must yield a 422 validation error
	 * with a {@code field=name} entry. Direct REST Assured call because the
	 * framework post() spec only accepts 200/201.
	 */
	@Test
	public void createUserWithMissingName_Expects422() {
		Map<String, String> body = new HashMap<>();
		body.put("email", StringUtils.getRandomEmailId());
		body.put("gender", "male");
		body.put("status", "active");
		Response response = RestAssured.given().baseUri(BASE_URL_GOREST)
				.header("Authorization", "Bearer " + ConfigManager.getProperty("Bearertoken"))
				.contentType(ContentType.JSON).accept(ContentType.JSON).body(body).when().post(GOREST_USERS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 422);
		Assert.assertEquals(response.jsonPath().getString("[0].field"), "name");
	}

	/**
	 * Negative: missing required {@code gender} must yield 422 with
	 * {@code field=gender}.
	 */
	@Test
	public void createUserWithMissingGender_Expects422() {
		Map<String, String> body = new HashMap<>();
		body.put("name", "Missing Gender");
		body.put("email", StringUtils.getRandomEmailId());
		body.put("status", "active");
		Response response = RestAssured.given().baseUri(BASE_URL_GOREST)
				.header("Authorization", "Bearer " + ConfigManager.getProperty("Bearertoken"))
				.contentType(ContentType.JSON).accept(ContentType.JSON).body(body).when().post(GOREST_USERS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 422);
		Assert.assertEquals(response.jsonPath().getString("[0].field"), "gender");
	}

	/**
	 * Negative: invalid gender enum value must be rejected with 422.
	 */
	@Test
	public void createUserWithInvalidGender_Expects422() {
		Map<String, String> body = new HashMap<>();
		body.put("name", "Alien User");
		body.put("email", StringUtils.getRandomEmailId());
		body.put("gender", "alien");
		body.put("status", "active");
		Response response = RestAssured.given().baseUri(BASE_URL_GOREST)
				.header("Authorization", "Bearer " + ConfigManager.getProperty("Bearertoken"))
				.contentType(ContentType.JSON).accept(ContentType.JSON).body(body).when().post(GOREST_USERS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 422);
		Assert.assertEquals(response.jsonPath().getString("[0].field"), "gender");
	}

	/**
	 * Negative: missing required {@code status} must yield 422 with
	 * {@code field=status}.
	 */
	@Test
	public void createUserWithMissingStatus_Expects422() {
		Map<String, String> body = new HashMap<>();
		body.put("name", "Missing Status");
		body.put("email", StringUtils.getRandomEmailId());
		body.put("gender", "male");
		Response response = RestAssured.given().baseUri(BASE_URL_GOREST)
				.header("Authorization", "Bearer " + ConfigManager.getProperty("Bearertoken"))
				.contentType(ContentType.JSON).accept(ContentType.JSON).body(body).when().post(GOREST_USERS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 422);
		Assert.assertEquals(response.jsonPath().getString("[0].field"), "status");
	}

	/**
	 * Negative: invalid status enum value must be rejected with 422.
	 */
	@Test
	public void createUserWithInvalidStatus_Expects422() {
		Map<String, String> body = new HashMap<>();
		body.put("name", "Bad Status");
		body.put("email", StringUtils.getRandomEmailId());
		body.put("gender", "male");
		body.put("status", "unknown");
		Response response = RestAssured.given().baseUri(BASE_URL_GOREST)
				.header("Authorization", "Bearer " + ConfigManager.getProperty("Bearertoken"))
				.contentType(ContentType.JSON).accept(ContentType.JSON).body(body).when().post(GOREST_USERS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 422);
		Assert.assertEquals(response.jsonPath().getString("[0].field"), "status");
	}

	/**
	 * Edge: empty JSON body must surface multiple field-level validation errors.
	 */
	@Test
	public void createUserWithEmptyBody_Expects422() {
		Response response = RestAssured.given().baseUri(BASE_URL_GOREST)
				.header("Authorization", "Bearer " + ConfigManager.getProperty("Bearertoken"))
				.contentType(ContentType.JSON).accept(ContentType.JSON).body("{}").when().post(GOREST_USERS_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 422);
		List<Map<String, Object>> errors = response.jsonPath().getList("$");
		Assert.assertTrue(errors.size() >= 4, "Empty body must yield at least one error per required field");
	}

	/**
	 * Edge / data-integrity: posting the same email twice must yield 422 with
	 * {@code field=email} on the second call. Cleans up the first user.
	 */
	@Test
	public void createUserWithDuplicateEmail_Expects422() {
		String email = StringUtils.getRandomEmailId();
		User user = User.builder().name("Dup").email(email).gender("male").status("active").build();
		Response first = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(first.statusCode(), 201);
		String userId = first.jsonPath().getString("id");
		try {
			Response second = RestAssured.given().baseUri(BASE_URL_GOREST)
					.header("Authorization", "Bearer " + ConfigManager.getProperty("Bearertoken"))
					.contentType(ContentType.JSON).accept(ContentType.JSON).body(user).when().post(GOREST_USERS_ENDPOINT);
			Assert.assertEquals(second.statusCode(), 422);
			Assert.assertEquals(second.jsonPath().getString("[0].field"), "email");
		} finally {
			restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		}
	}

	/**
	 * Boundary: 200-character name must still be accepted (GoRest documented
	 * max). Cleans up.
	 */
	@Test
	public void createUserWithVeryLongName_Accepts201() {
		String longName = new String(new char[200]).replace('\0', 'A');
		User user = User.builder().name(longName).email(StringUtils.getRandomEmailId()).gender("male").status("active").build();
		Response response = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(response.statusCode(), 201);
		Assert.assertEquals(response.jsonPath().getString("name").length(), 200);
		restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + response.jsonPath().getString("id"), null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
	}

	/**
	 * Edge / i18n: unicode name must round-trip correctly through the API.
	 * Cleans up.
	 */
	@Test
	public void createUserWithUnicodeName_Expects201() {
		User user = User.builder().name("测试用户").email(StringUtils.getRandomEmailId()).gender("male").status("active").build();
		Response response = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(response.statusCode(), 201);
		Assert.assertEquals(response.jsonPath().getString("name"), "测试用户");
		restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + response.jsonPath().getString("id"), null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
	}

	/**
	 * Schema: response Content-Type must declare JSON for the create endpoint.
	 */
	@Test
	public void createUserResponseContentTypeIsJson() {
		User user = User.builder().name("CT User").email(StringUtils.getRandomEmailId()).gender("male").status("active").build();
		Response response = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		String contentType = response.getHeader("Content-Type");
		Assert.assertNotNull(contentType, "Content-Type header must be present");
		Assert.assertTrue(contentType.toLowerCase().contains("json"),
				"Content-Type must declare JSON, got: " + contentType);
		restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + response.jsonPath().getString("id"), null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
	}

	/**
	 * NFR: create response time must be under a sane SLA threshold for a public
	 * API.
	 */
	@Test
	public void createUserResponseTimeUnderThreshold() {
		long start = System.currentTimeMillis();
		User user = User.builder().name("NFR User").email(StringUtils.getRandomEmailId()).gender("male").status("active").build();
		Response response = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		long elapsed = System.currentTimeMillis() - start;
		Assert.assertEquals(response.statusCode(), 201);
		Assert.assertTrue(elapsed < 5000, "create must complete under 5s, took " + elapsed + "ms");
		restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + response.jsonPath().getString("id"), null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
	}
}
