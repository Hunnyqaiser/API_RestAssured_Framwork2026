package com.qa.api.gorest.tests;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ctc.wstx.util.StringUtil;
import com.qa.api.base.BaseTest;
import com.qa.api.constants.AuthType;
import com.qa.api.pojo.User;
import com.qa.api.utils.StringUtils;
import com.qa.gorest.manager.ConfigManager;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class UpdateAUserTest extends BaseTest
{
	
private String tokenid = "aacab625a00c2b648fcc4ba7687514a0a24dd2d2ab7ac085d4b8a8131924620e";
	
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

}
