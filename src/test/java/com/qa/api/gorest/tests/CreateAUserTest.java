package com.qa.api.gorest.tests;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.Test;
import com.qa.api.base.BaseTest;
import com.qa.api.constants.AuthType;
import com.qa.api.pojo.User;
import com.qa.api.utils.StringUtils;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class CreateAUserTest extends BaseTest{
	
	@Test
	public void createAUserTest() {
		User user = new User("Yasir", StringUtils.getRandomEmailId(), "male", "active");
	Response response =	restClient.post(BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
	Assert.assertEquals(response.jsonPath().getString("name"), "Yasir");
	Assert.assertNotNull(response.jsonPath().getString("id"));
	Assert.assertEquals(response.jsonPath().getString("gender"), "male");
	Assert.assertEquals(response.jsonPath().getString("status"), "active");
	}
	
	
	@Test
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
	

}
