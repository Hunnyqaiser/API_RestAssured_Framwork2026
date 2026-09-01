package com.qa.api.reqres.tests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qa.api.base.BaseTest;
import com.qa.api.constants.AuthType;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class ReqResTest extends BaseTest {
	
	@Test
	public void getUserTest()
	{
		Map<String, String> queryMap = new HashMap<String, String>();
		queryMap.put("page", "2");
		
		
	 Response response = restClient.get(BASE_URL_REQRES, REQRES_ENDPOINT, queryMap, null, AuthType.NO_AUTH, ContentType.ANY);
		
		
	}

@Test
	public void getSingleUserPositiveTest() {
		// positive: GET a single known reqres user -> 200 with populated data
		Response response = restClient.get(BASE_URL_REQRES, REQRES_ENDPOINT + "/1", null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertEquals(response.jsonPath().getString("data.id"), "1");
		Assert.assertNotNull(response.jsonPath().getString("data.email"));
		Assert.assertNotNull(response.jsonPath().getString("data.first_name"));
	}

	@Test
	public void createUserPositiveTest() {
		// positive: POST create -> 201 with an id returned (reqres returns 201)
		Response response = restClient.post(BASE_URL_REQRES, REQRES_ENDPOINT,
				"{\"name\": \"QA Engineer\", \"job\": \"SDET\"}", null, null,
				AuthType.NO_AUTH, ContentType.JSON);
		Assert.assertEquals(response.statusCode(), 201);
		Assert.assertNotNull(response.jsonPath().getString("id"));
		Assert.assertNotNull(response.jsonPath().getString("createdAt"));
		Assert.assertEquals(response.jsonPath().getString("name"), "QA Engineer");
		Assert.assertEquals(response.jsonPath().getString("job"), "SDET");
	}

	@Test
	public void getNonExistingUser_Expects404() {
		// negative: unknown reqres user id -> 404 (raw call - framework get spec only accepts 200/401)
		Response response = RestAssured.given()
				.baseUri(BASE_URL_REQRES)
				.accept(ContentType.JSON)
				.when()
				.get(REQRES_ENDPOINT + "/99999");
		Assert.assertEquals(response.statusCode(), 404);
	}

	@Test
	public void userLifeCycleE2ETest() {
		// E2E: POST -> PUT -> PATCH -> DELETE against reqres (mock persists nothing, so we
		// exercise the full REST verb lifecycle and assert each documented status code)
		String userId = "2";

		// POST create -> 201
		Response createResponse = restClient.post(BASE_URL_REQRES, REQRES_ENDPOINT,
				"{\"name\": \"E2E User\", \"job\": \"QA\"}", null, null,
				AuthType.NO_AUTH, ContentType.JSON);
		Assert.assertEquals(createResponse.statusCode(), 201);

		// PUT full update -> 200
		Response putResponse = restClient.put(BASE_URL_REQRES, REQRES_ENDPOINT + "/" + userId,
				"{\"name\": \"E2E User Updated\", \"job\": \"Senior QA\"}", null, null,
				AuthType.NO_AUTH, ContentType.JSON);
		Assert.assertEquals(putResponse.statusCode(), 200);
		Assert.assertEquals(putResponse.jsonPath().getString("job"), "Senior QA");

		// PATCH partial update -> 200
		Response patchResponse = restClient.patch(BASE_URL_REQRES, REQRES_ENDPOINT + "/" + userId,
				"{\"job\": \"QA Manager\"}", null, null,
				AuthType.NO_AUTH, ContentType.JSON);
		Assert.assertEquals(patchResponse.statusCode(), 200);
		Assert.assertEquals(patchResponse.jsonPath().getString("job"), "QA Manager");

		// DELETE -> 204
		Response deleteResponse = restClient.delete(BASE_URL_REQRES, REQRES_ENDPOINT + "/" + userId,
				null, null, AuthType.NO_AUTH, ContentType.JSON);
		Assert.assertEquals(deleteResponse.statusCode(), 204);
	}

	/**
	 * Schema: every user entry in the list must carry the documented
	 * {id, email, first_name, last_name, avatar} field set.
	 */
	@Test
	public void getUsersListHasExpectedStructureTest() {
		Response response = restClient.get(BASE_URL_REQRES, REQRES_ENDPOINT + "?page=1", null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);
		List<Map<String, Object>> data = com.qa.api.utils.JsonPathValidatorUtils.readListOfMaps(response, "$.data[*]");
		Assert.assertFalse(data.isEmpty(), "data[] must not be empty");
		for (Map<String, Object> user : data) {
			Assert.assertNotNull(user.get("id"), "id must be present");
			Assert.assertNotNull(user.get("email"), "email must be present");
			Assert.assertNotNull(user.get("first_name"), "first_name must be present");
			Assert.assertNotNull(user.get("last_name"), "last_name must be present");
			Assert.assertNotNull(user.get("avatar"), "avatar must be present");
		}
	}

	/**
	 * Boundary: every email in the users list must match a basic email
	 * format.
	 */
	@Test
	public void getUsersListEmailFormatValidTest() {
		Response response = restClient.get(BASE_URL_REQRES, REQRES_ENDPOINT + "?page=1", null, null, AuthType.NO_AUTH, ContentType.ANY);
		List<Map<String, Object>> data = com.qa.api.utils.JsonPathValidatorUtils.readListOfMaps(response, "$.data[*]");
		java.util.regex.Pattern emailPattern = java.util.regex.Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
		for (Map<String, Object> user : data) {
			String email = (String) user.get("email");
			Assert.assertNotNull(email, "email must be present");
			Assert.assertTrue(emailPattern.matcher(email).matches(),
					"email must be valid format, got: " + email);
		}
	}

	/**
	 * Boundary: pages 1 and 2 must have data; page 99999 must return an empty
	 * {@code data} array without erroring.
	 */
	@Test
	public void getUsersPaginationEdgeTest() {
		Response p1 = restClient.get(BASE_URL_REQRES, REQRES_ENDPOINT + "?page=1", null, null, AuthType.NO_AUTH, ContentType.ANY);
		Response p2 = restClient.get(BASE_URL_REQRES, REQRES_ENDPOINT + "?page=2", null, null, AuthType.NO_AUTH, ContentType.ANY);
		Response pBig = restClient.get(BASE_URL_REQRES, REQRES_ENDPOINT + "?page=99999", null, null, AuthType.NO_AUTH, ContentType.ANY);

		Assert.assertEquals(p1.statusCode(), 200);
		Assert.assertEquals(p2.statusCode(), 200);
		Assert.assertEquals(pBig.statusCode(), 200);

		List<Object> d1 = com.qa.api.utils.JsonPathValidatorUtils.readList(p1, "$.data[*].id");
		List<Object> d2 = com.qa.api.utils.JsonPathValidatorUtils.readList(p2, "$.data[*].id");
		List<Object> dBig = com.qa.api.utils.JsonPathValidatorUtils.readList(pBig, "$.data[*].id");

		Assert.assertFalse(d1.isEmpty(), "page=1 must have data");
		Assert.assertFalse(d2.isEmpty(), "page=2 must have data");
		Assert.assertTrue(dBig.isEmpty(), "page=99999 must return empty data[]");
	}

	/**
	 * Edge: reqres mock accepts an empty body and returns 201 (mock contract).
	 */
	@Test
	public void createUserWithEmptyBody_Expects201() {
		Response response = restClient.post(BASE_URL_REQRES, REQRES_ENDPOINT, "{}", null, null, AuthType.NO_AUTH, ContentType.JSON);
		Assert.assertEquals(response.statusCode(), 201);
		Assert.assertNotNull(response.jsonPath().getString("id"));
	}

	/**
	 * Edge: reqres mock accepts an empty {@code name} value and returns 201.
	 */
	@Test
	public void createUserWithEmptyName_Expects201() {
		Response response = restClient.post(BASE_URL_REQRES, REQRES_ENDPOINT,
				"{\"name\":\"\",\"job\":\"x\"}", null, null, AuthType.NO_AUTH, ContentType.JSON);
		Assert.assertEquals(response.statusCode(), 201);
		Assert.assertEquals(response.jsonPath().getString("name"), "");
	}

	/**
	 * Edge: reqres mock returns 204 for DELETE on a non-existing id (mock
	 * contract). Documents the live behavior.
	 */
	@Test
	public void deleteUserNonExistingId_Expects204() {
		Response response = restClient.delete(BASE_URL_REQRES, REQRES_ENDPOINT + "/99999", null, null, AuthType.NO_AUTH, ContentType.JSON);
		Assert.assertEquals(response.statusCode(), 204);
	}

	/**
	 * NFR: list endpoint must respond under a sane SLA.
	 */
	@Test
	public void getUsersResponseTimeUnderThreshold() {
		long start = System.currentTimeMillis();
		Response response = restClient.get(BASE_URL_REQRES, REQRES_ENDPOINT + "?page=1", null, null, AuthType.NO_AUTH, ContentType.ANY);
		long elapsed = System.currentTimeMillis() - start;
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertTrue(elapsed < 5000, "list must complete under 5s, took " + elapsed + "ms");
	}
}
