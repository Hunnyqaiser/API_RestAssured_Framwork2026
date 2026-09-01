package com.qa.api.gorest.tests;


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


public class GetAUserTest extends BaseTest{
	
private String tokenid = "7d8b46db01faef61ccba406c9d8b402bc66c1bb3e8a4beb8a031c1919a183f82";
	
	@BeforeMethod
	public void setToken()
	{
		ConfigManager.setProperty("Bearertoken", tokenid);
	}
	
	
	@Test
	public void getAllUsers()
	{
		Response response = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertTrue(response.statusLine().contains("OK"));
	}
	
	@Test
	public void getAllUsersWithQueryParamTest()
	{
		Map<String, String> queryParaMap = new HashMap<String, String>();
		queryParaMap.put("name", "naveen");
		queryParaMap.put("status", "active");		
		Response response = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, queryParaMap, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertTrue(response.statusLine().contains("OK"));
	}
	
	@Test
	public void getSingalUserTest()
	{
		String userID = "8332781";
		Response response = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT+"/"+userID, null, null, AuthType.BEARER_TOKEN, ContentType.ANY);
		Assert.assertTrue(response.statusLine().contains("OK"));
		Assert.assertEquals(response.jsonPath().getString("id"), userID);
		
	}
	
@Test
	public void getUserByValidIdPositiveTest() {
		// positive: create a user, then GET by that id to prove a valid id resolves
		User user = User.builder().name("Get Test").email(StringUtils.getRandomEmailId()).gender("female").status("active").build();
		Response createResponse = restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		String userId = createResponse.jsonPath().getString("id");
		Assert.assertNotNull(userId);

		Response response = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertEquals(response.jsonPath().getString("id"), userId);
		Assert.assertEquals(response.jsonPath().getString("name"), "Get Test");

		// cleanup
		restClient.delete(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
	}

	@Test
	public void getUserWithInvalidId_Expects404() {
		// negative: non-existing user id must return 404 (framework get spec is 200/401, so raw call)
		Response response = RestAssured.given()
				.baseUri(BASE_URL_GOREST)
				.header("Authorization", "Bearer " + ConfigManager.getProperty("Bearertoken"))
				.accept(ContentType.JSON)
				.when()
				.get(GOREST_USERS_ENDPOINT + "/99999999");
		Assert.assertEquals(response.statusCode(), 404);
		Assert.assertEquals(response.jsonPath().getString("message"), "Resource not found");
	}

	@Test
	public void getAllUsersPaginationE2ETest() {
		// E2E: walk page 1 and page 2, both must return data and not be identical
		Map<String, String> queryPage1 = new HashMap<String, String>();
		queryPage1.put("page", "1");
		Map<String, String> queryPage2 = new HashMap<String, String>();
		queryPage2.put("page", "2");

		Response page1Response = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, queryPage1, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Response page2Response = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, queryPage2, null, AuthType.BEARER_TOKEN, ContentType.JSON);

		Assert.assertEquals(page1Response.statusCode(), 200);
		Assert.assertEquals(page2Response.statusCode(), 200);

		List<Object> page1 = com.qa.api.utils.JsonPathValidatorUtils.readList(page1Response, "$[*].id");
		List<Object> page2 = com.qa.api.utils.JsonPathValidatorUtils.readList(page2Response, "$[*].id");
		Assert.assertFalse(page1.isEmpty(), "Page 1 must contain users");
		Assert.assertFalse(page2.isEmpty(), "Page 2 must contain users");
		Assert.assertNotEquals(page1, page2, "Pagination should return different users per page");
	}

	/**
	 * Positive: filter by gender returns only records whose {@code gender} field
	 * equals the requested value.
	 */
	@Test
	public void getAllUsersWithGenderFilterTest() {
		Map<String, String> query = new HashMap<>();
		query.put("gender", "female");
		Response response = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, query, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(response.statusCode(), 200);
		List<Map<String, Object>> users = com.qa.api.utils.JsonPathValidatorUtils.readListOfMaps(response, "$[*]");
		Assert.assertFalse(users.isEmpty(), "Filter must return at least one female user");
		for (Map<String, Object> u : users) {
			Assert.assertEquals(u.get("gender"), "female", "Every record must match the gender filter");
		}
	}

	/**
	 * Positive: filter by status returns only active records.
	 */
	@Test
	public void getAllUsersWithStatusFilterTest() {
		Map<String, String> query = new HashMap<>();
		query.put("status", "active");
		Response response = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, query, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(response.statusCode(), 200);
		List<Map<String, Object>> users = com.qa.api.utils.JsonPathValidatorUtils.readListOfMaps(response, "$[*]");
		Assert.assertFalse(users.isEmpty(), "Filter must return at least one active user");
		for (Map<String, Object> u : users) {
			Assert.assertEquals(u.get("status"), "active", "Every record must match the status filter");
		}
	}

	/**
	 * Positive: combining {@code name} + {@code status} query params narrows the
	 * result set (intersection).
	 */
	@Test
	public void getAllUsersWithNameAndStatusCombinedTest() {
		Map<String, String> query = new HashMap<>();
		query.put("name", "a");
		query.put("status", "active");
		Response response = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, query, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(response.statusCode(), 200);
		List<Map<String, Object>> users = com.qa.api.utils.JsonPathValidatorUtils.readListOfMaps(response, "$[*]");
		for (Map<String, Object> u : users) {
			Assert.assertEquals(u.get("status"), "active");
			Assert.assertTrue(u.get("name").toString().toLowerCase().contains("a"),
					"name must contain 'a' (case-insensitive), got: " + u.get("name"));
		}
	}

	/**
	 * Negative: a non-numeric id must return 404 (GoRest contract).
	 */
	@Test
	public void getUserWithNonNumericId_Expects404() {
		Response response = RestAssured.given().baseUri(BASE_URL_GOREST)
				.header("Authorization", "Bearer " + ConfigManager.getProperty("Bearertoken"))
				.accept(ContentType.JSON).when().get(GOREST_USERS_ENDPOINT + "/abc");
		Assert.assertEquals(response.statusCode(), 404);
	}

	/**
	 * Boundary: extreme pagination far beyond available data must return an
	 * empty list (200, not error).
	 */
	@Test
	public void getAllUsersWithExtremePaginationTest() {
		Map<String, String> query = new HashMap<>();
		query.put("page", "99999");
		Response response = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, query, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(response.statusCode(), 200);
		List<Object> ids = com.qa.api.utils.JsonPathValidatorUtils.readList(response, "$[*].id");
		Assert.assertTrue(ids.isEmpty(), "Page 99999 must return an empty list");
	}

	/**
	 * Boundary: {@code per_page=1} returns exactly 1 record; {@code per_page=100}
	 * returns at most 100.
	 */
	@Test
	public void getAllUsersWithPerPageBoundaryTest() {
		Map<String, String> q1 = new HashMap<>();
		q1.put("per_page", "1");
		Response r1 = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, q1, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(r1.statusCode(), 200);
		List<Object> ids1 = com.qa.api.utils.JsonPathValidatorUtils.readList(r1, "$[*].id");
		Assert.assertEquals(ids1.size(), 1, "per_page=1 must return exactly 1 record");

		Map<String, String> q100 = new HashMap<>();
		q100.put("per_page", "100");
		Response r100 = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, q100, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		Assert.assertEquals(r100.statusCode(), 200);
		List<Object> ids100 = com.qa.api.utils.JsonPathValidatorUtils.readList(r100, "$[*].id");
		Assert.assertTrue(ids100.size() <= 100, "per_page=100 must return at most 100 records, got " + ids100.size());
	}

	/**
	 * NFR: the list endpoint must respond under a sane SLA.
	 */
	@Test
	public void getAllUsersResponseTimeUnderThreshold() {
		long start = System.currentTimeMillis();
		Response response = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		long elapsed = System.currentTimeMillis() - start;
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertTrue(elapsed < 5000, "List must complete under 5s, took " + elapsed + "ms");
	}

	/**
	 * Schema: response Content-Type for the users list must declare JSON.
	 */
	@Test
	public void getAllUsersContentTypeIsJson() {
		Response response = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		String ct = response.getHeader("Content-Type");
		Assert.assertNotNull(ct);
		Assert.assertTrue(ct.toLowerCase().contains("json"),
				"Content-Type must contain json, got: " + ct);
	}
}
