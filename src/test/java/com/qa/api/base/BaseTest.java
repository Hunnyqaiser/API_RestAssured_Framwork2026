package com.qa.api.base;

import org.testng.annotations.BeforeTest;

import com.qa.gorest.client.RestClient;

public class BaseTest {
	
	protected RestClient restClient;
	
	
	//***********API Base URLs***************//
	protected final static String BASE_URL_GOREST = "https://gorest.co.in";
	
	

	//***********API EndPoints***************//
		protected final static String GOREST_USERS_ENDPOINT = "/public/v2/users";
		
		
	@BeforeTest
	public void setup()
	{
		restClient = new RestClient();
		
	}

}
