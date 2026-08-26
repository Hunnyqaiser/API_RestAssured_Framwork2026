package com.qa.api.gorest.tests;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.qa.api.base.BaseTest;
import com.qa.api.constants.AuthType;
import com.qa.api.pojo.User;
import com.qa.api.utils.StringUtils;
import com.qa.gorest.manager.ConfigManager;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class DeleteAUserTest extends BaseTest {
	
private String tokenid = "aacab625a00c2b648fcc4ba7687514a0a24dd2d2ab7ac085d4b8a8131924620e";
	
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
		
		// Get Call to check if user is created is in DB
				Response getUserResponse2 = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null,
						AuthType.BEARER_TOKEN, ContentType.JSON);
				Assert.assertTrue(getUserResponse2.getStatusLine().contains("Not Found"));
				Assert.assertEquals(getUserResponse2.statusCode(), 404);
				Assert.assertEquals(getUserResponse2.jsonPath().getString("message"), "Resource not found");
		
	}

}
