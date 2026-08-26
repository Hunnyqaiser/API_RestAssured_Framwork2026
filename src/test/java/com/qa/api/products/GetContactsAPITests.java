package com.qa.api.products;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.qa.api.base.BaseTest;
import com.qa.api.constants.AuthType;
import com.qa.api.pojo.ContactsCredentials;
import com.qa.gorest.manager.ConfigManager;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class GetContactsAPITests extends BaseTest

{
	
	private String tokenID;
	
	@BeforeMethod
	public void getToken()
	{
	   ContactsCredentials credentials = ContactsCredentials.builder()
			   .email("Test_user@email.com")
			   .password("Test123")
			   .build();
	   
	   
	  Response response = restClient.post(BASE_URL_CONTACTS, CONTACTS_LOGIN_ENDPOINT, credentials, null, null, AuthType.NO_AUTH, ContentType.JSON);
	  		tokenID = response.jsonPath().getString("token");
	  		System.out.println("contacts login token ------->"+ tokenID);
	  		ConfigManager.setProperty("Bearertoken", tokenID);
	}
	
	@Test
	public void getAllContactsTest()
	{
		restClient.get(BASE_URL_CONTACTS, CONTACTS_ENDPOINT, null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
		
		
	}
}
