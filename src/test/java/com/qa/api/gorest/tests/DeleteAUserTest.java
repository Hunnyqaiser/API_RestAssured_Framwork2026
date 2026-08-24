package com.qa.api.gorest.tests;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qa.api.base.BaseTest;
import com.qa.api.constants.AuthType;
import com.qa.api.pojo.User;
import com.qa.api.utils.StringUtils;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class DeleteAUserTest extends BaseTest {

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
		Assert.assertEquals(deleteResponse.jsonPath().getString("message"), "Resource not Found");
		
		// Get Call to check if user is created is in DB
				Response getUserResponse2 = restClient.get(BASE_URL_GOREST, GOREST_USERS_ENDPOINT + "/" + userId, null, null,
						AuthType.BEARER_TOKEN, ContentType.JSON);
				Assert.assertTrue(getUserResponse2.getStatusLine().contains("Not Found"));
		
	}

}
