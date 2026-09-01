package com.qa.api.gorest.tests;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.qa.api.base.BaseTest;
import com.qa.api.constants.AuthType;
import com.qa.api.pojo.User;
import com.qa.api.utils.JsonUtils;
import com.qa.api.utils.StringUtils;
import com.qa.gorest.manager.ConfigManager;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class GetAUserWithSerializaitonTest extends BaseTest
{
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
	
	
	String userId = response.jsonPath().getString("id");
	
	Response getUserResponse = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null,
			AuthType.BEARER_TOKEN, ContentType.JSON);
	Assert.assertTrue(getUserResponse.getStatusLine().contains("OK"));
	
	User userResponse	= JsonUtils.deserialize(getUserResponse, User.class);
	Assert.assertEquals(userResponse.getName(), user.getName());
	
	}
	
@Test
	public void deserializeUserListPositiveTest() {
		// positive: GET the users list and deserialize the response array into User[] POJOs
		Response response = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(response.statusCode(), 200);

		User[] users = JsonUtils.deserialize(response, User[].class);
		Assert.assertTrue(users.length > 0, "The users list should not be empty");
		for (User user : users) {
			Assert.assertNotNull(user.getId(), "Each deserialized user must have an id");
			Assert.assertNotNull(user.getEmail(), "Each deserialized user must have an email");
		}
	}

	@Test
	public void createGetDeleteSerializationE2ETest() {
		// E2E: create user -> serialize request -> GET -> deserialize -> DELETE (full round trip)
		User user = User.builder().name("Serialize E2E").email(StringUtils.getRandomEmailId()).gender("female").status("active").build();
		Response createResponse = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		String userId = createResponse.jsonPath().getString("id");

		Response getUserResponse = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		User deserialized = JsonUtils.deserialize(getUserResponse, User.class);
		Assert.assertEquals(deserialized.getId().toString(), userId);
		Assert.assertEquals(deserialized.getName(), user.getName());
		Assert.assertEquals(deserialized.getEmail(), user.getEmail());
		Assert.assertEquals(deserialized.getStatus(), user.getStatus());

		Response deleteResponse = restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(deleteResponse.statusCode(), 204);
	}

	/**
	 * Positive: deserialized single user must expose every documented field
	 * (id, name, email, gender, status) populated and non-null.
	 */
	@Test
	public void deserializeSingleUserAllFieldsPresentTest() {
		User user = User.builder().name("Field Round Trip").email(StringUtils.getRandomEmailId()).gender("male").status("active").build();
		Response createResponse = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		String userId = createResponse.jsonPath().getString("id");
		try {
			Response getUserResponse = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
			User deserialized = JsonUtils.deserialize(getUserResponse, User.class);
			Assert.assertNotNull(deserialized.getId(), "id must be populated");
			Assert.assertNotNull(deserialized.getName(), "name must be populated");
			Assert.assertNotNull(deserialized.getEmail(), "email must be populated");
			Assert.assertNotNull(deserialized.getGender(), "gender must be populated");
			Assert.assertNotNull(deserialized.getStatus(), "status must be populated");
			Assert.assertEquals(deserialized.getName(), "Field Round Trip");
		} finally {
			restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		}
	}

	/**
	 * Positive: deserialize the users list into a {@code User[]} array; it must
	 * be non-empty and every POJO must have id/email/gender/status populated.
	 */
	@Test
	public void deserializeUserListAllFieldsPresentTest() {
		Response response = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(response.statusCode(), 200);
		User[] users = JsonUtils.deserialize(response, User[].class);
		Assert.assertTrue(users.length > 0, "Users list must not be empty");
		for (User u : users) {
			Assert.assertNotNull(u.getId(), "id must be populated");
			Assert.assertNotNull(u.getEmail(), "email must be populated");
			Assert.assertNotNull(u.getGender(), "gender must be populated");
			Assert.assertNotNull(u.getStatus(), "status must be populated");
		}
	}

	/**
	 * E2E serialization: create → update → get → deserialize and verify the
	 * POJO reflects the updated values, proving the deserializer picks up
	 * post-create changes. Cleans up.
	 */
	@Test
	public void deserializeAfterUpdate_ReflectsNewValues() {
		User user = User.builder().name("Pre Update").email(StringUtils.getRandomEmailId()).gender("male").status("active").build();
		Response createResponse = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		String userId = createResponse.jsonPath().getString("id");
		try {
			User updated = User.builder().name("Post Update").email(user.getEmail()).gender("female").status("inactive").build();
			restClient.put(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, updated, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);

			Response getUserResponse = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
			User deserialized = JsonUtils.deserialize(getUserResponse, User.class);
			Assert.assertEquals(deserialized.getName(), "Post Update");
			Assert.assertEquals(deserialized.getGender(), "female");
			Assert.assertEquals(deserialized.getStatus(), "inactive");
		} finally {
			restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		}
	}
}
