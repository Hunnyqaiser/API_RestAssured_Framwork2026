package com.qa.api.basicAuth.test;



import java.util.Base64;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qa.api.base.BaseTest;
import com.qa.api.constants.AuthType;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class BasicAuthTest extends BaseTest {
	
	
	@Test
	public void basicAuthTest()
	{
		Response response =	restClient.get(BASE_URL_BASIC_AUTH,BASIC_AUTH_ENDPOINT, null, null,AuthType.BASIC_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertTrue(response.body().asString().contains("Congratulations! You must have the proper credentials."));
		
		
	}

@Test
	public void basicAuthWithWrongCredentials_Expects401() {
		// negative: wrong password -> 401 (raw call with explicit wrong credentials)
		String wrongCreds = Base64.getEncoder().encodeToString("admin:wrongpass".getBytes());
		Response response = RestAssured.given()
				.baseUri(BASE_URL_BASIC_AUTH)
				.header("Authorization", "Basic " + wrongCreds)
				.when()
				.get(BASIC_AUTH_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 401);
	}

	@Test
	public void basicAuthWithoutCredentials_Expects401() {
		// negative: no credentials at all -> 401
		Response response = RestAssured.given()
				.baseUri(BASE_URL_BASIC_AUTH)
				.when()
				.get(BASIC_AUTH_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 401);
	}

	@Test
	public void basicAuthAccessE2ETest() {
		// E2E: without credentials -> 401; with valid basic credentials -> 200 + success message
		Response denied = RestAssured.given()
				.baseUri(BASE_URL_BASIC_AUTH)
				.when()
				.get(BASIC_AUTH_ENDPOINT);
		Assert.assertEquals(denied.statusCode(), 401, "Anonymous request must be rejected");

		Response allowed = restClient.get(BASE_URL_BASIC_AUTH, BASIC_AUTH_ENDPOINT, null, null,
				AuthType.BASIC_AUTH, ContentType.ANY);
		Assert.assertEquals(allowed.statusCode(), 200);
		Assert.assertTrue(allowed.body().asString().contains("Congratulations! You must have the proper credentials."));
	}

	/**
	 * Security: a malformed Base64 in the {@code Authorization} header must
	 * be rejected with 401.
	 */
	@Test
	public void basicAuthWithMalformedBase64_Expects401() {
		Response response = RestAssured.given()
				.baseUri(BASE_URL_BASIC_AUTH)
				.header("Authorization", "Basic !!!notbase64!!!")
				.when()
				.get(BASIC_AUTH_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 401);
	}

	/**
	 * Edge: empty authentication header value must yield 401.
	 */
	@Test
	public void basicAuthWithEmptyAuthHeader_Expects401() {
		Response response = RestAssured.given()
				.baseUri(BASE_URL_BASIC_AUTH)
				.header("Authorization", "Basic ")
				.when()
				.get(BASIC_AUTH_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 401);
	}

	/**
	 * Edge: empty password ({@code admin:}) must yield 401.
	 */
	@Test
	public void basicAuthWithEmptyPassword_Expects401() {
		String encoded = Base64.getEncoder().encodeToString("admin:".getBytes());
		Response response = RestAssured.given()
				.baseUri(BASE_URL_BASIC_AUTH)
				.header("Authorization", "Basic " + encoded)
				.when()
				.get(BASIC_AUTH_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 401);
	}

	/**
	 * Edge: empty username ({@code :admin}) must yield 401.
	 */
	@Test
	public void basicAuthWithEmptyUsername_Expects401() {
		String encoded = Base64.getEncoder().encodeToString(":admin".getBytes());
		Response response = RestAssured.given()
				.baseUri(BASE_URL_BASIC_AUTH)
				.header("Authorization", "Basic " + encoded)
				.when()
				.get(BASIC_AUTH_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 401);
	}

	/**
	 * Schema: the success body must contain the documented success marker.
	 */
	@Test
	public void basicAuthSuccessBodyContainsMarkerTest() {
		Response response = restClient.get(BASE_URL_BASIC_AUTH, BASIC_AUTH_ENDPOINT, null, null,
				AuthType.BASIC_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);
		String body = response.body().asString();
		Assert.assertTrue(body.contains("Congratulations!"), "body must contain the success marker");
		Assert.assertTrue(body.contains("proper credentials"), "body must explain why access was granted");
	}

	/**
	 * NFR: successful auth response must be returned under a sane SLA.
	 */
	@Test
	public void basicAuthResponseTimeUnderThreshold() {
		long start = System.currentTimeMillis();
		Response response = restClient.get(BASE_URL_BASIC_AUTH, BASIC_AUTH_ENDPOINT, null, null,
				AuthType.BASIC_AUTH, ContentType.ANY);
		long elapsed = System.currentTimeMillis() - start;
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertTrue(elapsed < 5000, "Auth must complete under 5s, took " + elapsed + "ms");
	}
}
