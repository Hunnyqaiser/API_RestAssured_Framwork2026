package com.qa.api.amadeus.test;

import java.util.HashMap;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import com.qa.api.base.BaseTest;
import com.qa.api.constants.AuthType;
import com.qa.gorest.manager.ConfigManager;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class AmadeusAPITest extends BaseTest{
	
	private String token;
	
@BeforeMethod
public void getOAuth2Token(){
	// note: RestClient.post(baseUrl, endPoint, clientid, clientSecret, grantType, contentType)
	Response response =restClient.post(BASE_URL_OAUTH2_AMADEUS, AMADEUS_OAUTH_TOKEN_ENDPOINT, ConfigManager.getProperty("clientid")
			,ConfigManager.getProperty("clientseceret"),
			ConfigManager.getProperty("granttype"), ContentType.URLENC);

	token=response.jsonPath().getString("access_token");
	System.out.println("Access Token: "+token);
	
	ConfigManager.setProperty("Bearertoken", token);
}

@Test
public void flightDetailsTest() {
	// Using the OAuth2 token obtained in getOAuth2Token() as the Bearer token
	Response response = restClient.get(BASE_URL_OAUTH2_AMADEUS, AMADEUS_FLIGHT_ENDPOINT, null, null, AuthType.BEARER_TOKEN, ContentType.ANY);
	
	Assert.assertEquals(response.getStatusCode(), 200, "Expect the protected API to return 200 with a valid Bearer token");
	
	// Duende demo API echoes back the JWT claims of the presented access token
	String scope = response.jsonPath().getString("find { it.type == 'scope' }.value");
	String issuer = response.jsonPath().getString("find { it.type == 'iss' }.value");
	String scheme = response.jsonPath().getString("find { it.type == 'authorization_scheme' }.value");
	
	System.out.println("Response claims: " + response.body().asString());
	
	Assert.assertEquals(issuer, "https://demo.duendesoftware.com");
	Assert.assertEquals(scope, "api");
	Assert.assertEquals(scheme, "Bearer");
}

@Test
	public void flightDetailsWithoutToken_Expects401() {
		// negative: calling the protected API without any bearer token -> 401
		Response response = RestAssured.given()
				.baseUri(BASE_URL_OAUTH2_AMADEUS)
				.accept(ContentType.JSON)
				.when()
				.get(AMADEUS_FLIGHT_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 401);
	}

	@Test
	public void flightDetailsWithInvalidToken_Expects401() {
		// negative: garbage bearer token -> 401
		Response response = RestAssured.given()
				.baseUri(BASE_URL_OAUTH2_AMADEUS)
				.header("Authorization", "Bearer this.is.not.a.valid.jwt")
				.accept(ContentType.JSON)
				.when()
				.get(AMADEUS_FLIGHT_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 401);
	}

	@Test
	public void oauth2TokenLifecycleE2ETest() {
		// E2E: obtain token via OAuth2 client-credentials -> use it to access the protected
		// API -> verify the JWT claims echo -> assert token is reusable within its expiry
		Response tokenResponse = restClient.post(BASE_URL_OAUTH2_AMADEUS, AMADEUS_OAUTH_TOKEN_ENDPOINT,
				ConfigManager.getProperty("clientid"),
				ConfigManager.getProperty("clientseceret"),
				ConfigManager.getProperty("granttype"),
				ContentType.URLENC);
		Assert.assertEquals(tokenResponse.statusCode(), 200);
		String accessToken = tokenResponse.jsonPath().getString("access_token");
		Assert.assertNotNull(accessToken, "access_token must be returned");
		Assert.assertEquals(tokenResponse.jsonPath().getString("token_type"), "Bearer");

		// use the just-obtained token directly against the protected resource
		Response apiResponse = RestAssured.given()
				.baseUri(BASE_URL_OAUTH2_AMADEUS)
				.header("Authorization", "Bearer " + accessToken)
				.accept(ContentType.JSON)
				.when()
				.get(AMADEUS_FLIGHT_ENDPOINT);
		Assert.assertEquals(apiResponse.statusCode(), 200);
		Assert.assertEquals(apiResponse.jsonPath().getString("find { it.type == 'iss' }.value"),
				"https://demo.duendesoftware.com");
		Assert.assertEquals(apiResponse.jsonPath().getString("find { it.type == 'client_id' }.value"), "m2m");
	}

	/**
	 * Schema: the token endpoint response must declare access_token,
	 * token_type=Bearer and a positive expires_in.
	 */
	@Test
	public void oauth2TokenResponseContainsRequiredFields() {
		Response response = restClient.post(BASE_URL_OAUTH2_AMADEUS, AMADEUS_OAUTH_TOKEN_ENDPOINT,
				ConfigManager.getProperty("clientid"),
				ConfigManager.getProperty("clientseceret"),
				ConfigManager.getProperty("granttype"),
				ContentType.URLENC);
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertNotNull(response.jsonPath().getString("access_token"), "access_token must be present");
		Assert.assertEquals(response.jsonPath().getString("token_type"), "Bearer");
		Integer expiresIn = response.jsonPath().getInt("expires_in");
		Assert.assertNotNull(expiresIn, "expires_in must be present");
		Assert.assertTrue(expiresIn > 0, "expires_in must be positive, got: " + expiresIn);
	}

	/**
	 * Positive: the same access token must be reusable for multiple sequential
	 * calls without re-auth.
	 */
	@Test
	public void oauth2TokenIsReusableAcrossCalls() {
		String accessToken = token;
		Assert.assertNotNull(accessToken);
		for (int i = 0; i < 2; i++) {
			Response response = RestAssured.given()
					.baseUri(BASE_URL_OAUTH2_AMADEUS)
					.header("Authorization", "Bearer " + accessToken)
					.accept(ContentType.JSON)
					.when()
					.get(AMADEUS_FLIGHT_ENDPOINT);
			Assert.assertEquals(response.statusCode(), 200,
					"token must remain valid on call " + (i + 1));
		}
	}

	/**
	 * Boundary: Duende demo issues tokens with {@code expires_in=3600}.
	 */
	@Test
	public void oauth2TokenExpiresInIs3600() {
		Response response = restClient.post(BASE_URL_OAUTH2_AMADEUS, AMADEUS_OAUTH_TOKEN_ENDPOINT,
				ConfigManager.getProperty("clientid"),
				ConfigManager.getProperty("clientseceret"),
				ConfigManager.getProperty("granttype"),
				ContentType.URLENC);
		Assert.assertEquals(response.jsonPath().getInt("expires_in"), Integer.valueOf(3600),
				"Duende demo must issue 3600-second tokens");
	}

	/**
	 * Negative: token endpoint must reject an invalid client secret with 400.
	 */
	@Test
	public void oauth2TokenWithInvalidClientSecret_Expects400() {
		Response response = RestAssured.given()
				.baseUri(BASE_URL_OAUTH2_AMADEUS)
				.contentType("application/x-www-form-urlencoded")
				.formParam("grant_type", "client_credentials")
				.formParam("client_id", ConfigManager.getProperty("clientid"))
				.formParam("client_secret", "WRONG_SECRET")
				.when()
				.post(AMADEUS_OAUTH_TOKEN_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 400);
		Assert.assertNotNull(response.jsonPath().getString("error"));
	}

	/**
	 * Negative: token endpoint must reject a missing client_id with 400.
	 */
	@Test
	public void oauth2TokenWithMissingClientId_Expects400() {
		Response response = RestAssured.given()
				.baseUri(BASE_URL_OAUTH2_AMADEUS)
				.contentType("application/x-www-form-urlencoded")
				.formParam("grant_type", "client_credentials")
				.formParam("client_secret", ConfigManager.getProperty("clientseceret"))
				.when()
				.post(AMADEUS_OAUTH_TOKEN_ENDPOINT);
		Assert.assertEquals(response.statusCode(), 400);
	}

	/**
	 * Boundary: two consecutive token requests within seconds must both
	 * succeed (token endpoint has no rate-limit at the second granularity).
	 */
	@Test
	public void oauth2TokenEndpointAllowsBackToBackCalls() {
		Response first = restClient.post(BASE_URL_OAUTH2_AMADEUS, AMADEUS_OAUTH_TOKEN_ENDPOINT,
				ConfigManager.getProperty("clientid"),
				ConfigManager.getProperty("clientseceret"),
				ConfigManager.getProperty("granttype"),
				ContentType.URLENC);
		Response second = restClient.post(BASE_URL_OAUTH2_AMADEUS, AMADEUS_OAUTH_TOKEN_ENDPOINT,
				ConfigManager.getProperty("clientid"),
				ConfigManager.getProperty("clientseceret"),
				ConfigManager.getProperty("granttype"),
				ContentType.URLENC);
		Assert.assertEquals(first.statusCode(), 200);
		Assert.assertEquals(second.statusCode(), 200);
		Assert.assertNotNull(first.jsonPath().getString("access_token"));
		Assert.assertNotNull(second.jsonPath().getString("access_token"));
	}

	/**
	 * NFR: the protected resource must respond under a sane SLA.
	 */
	@Test
	public void protectedApiResponseTimeUnderThreshold() {
		long start = System.currentTimeMillis();
		Response response = restClient.get(BASE_URL_OAUTH2_AMADEUS, AMADEUS_FLIGHT_ENDPOINT, null, null, AuthType.BEARER_TOKEN, ContentType.ANY);
		long elapsed = System.currentTimeMillis() - start;
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertTrue(elapsed < 5000, "protected API must complete under 5s, took " + elapsed + "ms");
	}

	/**
	 * Schema: the protected API's echoed JWT must contain the documented
	 * audience claim value {@code api}.
	 */
	@Test
	public void protectedApiClaimsContainExpectedAudience() {
		Response response = restClient.get(BASE_URL_OAUTH2_AMADEUS, AMADEUS_FLIGHT_ENDPOINT, null, null, AuthType.BEARER_TOKEN, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertEquals(response.jsonPath().getString("find { it.type == 'scope' }.value"), "api");
	}
}
