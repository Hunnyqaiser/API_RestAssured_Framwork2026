package com.qa.api.gorest.tests;


import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qa.api.base.BaseTest;
import com.qa.api.constants.AuthType;

import io.restassured.http.ContentType;
import io.restassured.response.Response;


public class GetAUserTest extends BaseTest{
	
	
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
	

}
