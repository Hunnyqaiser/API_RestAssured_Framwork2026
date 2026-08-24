package com.qa.gorest.client;


import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import java.io.File;
import java.util.Map;

import com.qa.api.constants.AuthType;
import com.qa.api.exceptions.APIException;
import com.qa.gorest.manager.ConfigManager;

public class RestClient {
	
	// Define Response Specs: 
	private ResponseSpecification responseSpec200 = expect().statusCode(200);
	private ResponseSpecification responseSpec204 = expect().statusCode(204);
	private ResponseSpecification responseSpec201 = expect().statusCode(201);
	private ResponseSpecification responseSpec400 = expect().statusCode(400);
	private ResponseSpecification responseSpec404 = expect().statusCode(404);
	private ResponseSpecification responseSpec200or201 = expect().statusCode(anyOf(equalTo(200), equalTo(201)));
	private ResponseSpecification responseSpec200or401 = expect().statusCode(anyOf(equalTo(200), equalTo(401)));
	private ResponseSpecification responseSpec200or404 = expect().statusCode(anyOf(equalTo(200), equalTo(404)));
	private ResponseSpecification responseSpec200or500 = expect().statusCode(anyOf(equalTo(200), equalTo(500)));
	private ResponseSpecification responseSpec200or501 = expect().statusCode(anyOf(equalTo(200), equalTo(501)));
	
	
	private RequestSpecification setupRequest(String baseURI, AuthType authtype, ContentType contenttype)
	{
		RequestSpecification request = RestAssured.given().log().all()
			.baseUri(baseURI)
			.contentType(contenttype)
			.accept(contenttype);
		
		switch (authtype){
		case BEARER_TOKEN:
		request.header("Authorization", "Bearer "+ConfigManager.Get("gorest_bearerToken"));
			break;
		case OAUTH2:
			request.header("Authorization", "Bearer");
			break;
		case BASIC_AUTH:
			request.header("Authorization", "Basic"+"==BasicAuth token needs to be added");
			break;
		case API_KEY:
			request.header("Authorization", "x-api-key"+"==api Key needs adding======");
			break;
		case NO_AUTH:
			System.out.println("Auth is not required.........");
			break;
		default:
			System.out.println("This Type of Auth is not supported..... Please check AuthType");
			throw new APIException("====Invalid Auth======");
		}
		
		return request;
	}
	
	private void applyParams(RequestSpecification request, Map<String, String> queryParams, Map<String, String> pathParams)
	{
		if(queryParams!=null) {
			request.queryParams(queryParams);
		}
		if(pathParams!=null)
		{
			request.pathParams(pathParams);
		}
	}
	
	
	//CURD Operations
	//GET
	/**
	 * This Method is Used to Call Get API's
	 * @param baseUrl
	 * @param endPoint
	 * @param queryParams
	 * @param PathParams
	 * @param authType
	 * @param ContentType
	 * @return it returns the GET API call response
	 */
	public Response get(String BaseUrl, String endpoint, 
			Map<String, String> queryParams,
			Map<String, String> pathParams,
			AuthType authType,
			ContentType contentType) {
		
		RequestSpecification request = setupRequest(BaseUrl, authType, contentType);
		applyParams(request, queryParams, pathParams);
			
		Response response = request.get(endpoint).then().spec(responseSpec200or401).extract().response();
		response.prettyPrint();
		return response;
		
	}
	/**
	 * 
	 * @param <T>
	 * @param BaseUrl
	 * @param endPoint
	 * @param body
	 * @param queryParams
	 * @param pathParams
	 * @param authType
	 * @param contentType
	 * @return it returns the Post API call response
	 */
	public <T>Response post(String BaseUrl, String endPoint, T body,
			Map<String, String> queryParams,
			Map<String, String> pathParams,
			AuthType authType,
			ContentType contentType) {
		
		RequestSpecification request = setupRequest(BaseUrl, authType, contentType);
		applyParams(request, queryParams, pathParams);
	Response response = request.body(body).post(endPoint).then().spec(responseSpec200or201).extract().response();
	response.prettyPrint();
	return response;
		
	}
	/**
	 * 
	 * 
	 * @param BaseUrl
	 * @param endPoint
	 * @param file
	 * @param queryParams
	 * @param pathParams
	 * @param authType
	 * @param contentType
	 * @return it returns the POST API call response
	 */
	public Response post(String BaseUrl, String endPoint, File file,
			Map<String, String> queryParams,
			Map<String, String> pathParams,
			AuthType authType,
			ContentType contentType) {
		
		RequestSpecification request = setupRequest(BaseUrl, authType, contentType);
		applyParams(request, queryParams, pathParams);
	Response response = request.body(file).post(endPoint).then().spec(responseSpec200or201).extract().response();
	response.prettyPrint();
	return response;
		
	}
	
	/**
	 * 
	 * @param <T>
	 * @param BaseUrl
	 * @param endPoint
	 * @param body
	 * @param queryParams
	 * @param pathParams
	 * @param authType
	 * @param contentType
	 * @return
	 */
	public <T>Response put(String BaseUrl, String endPoint, T body,
			Map<String, String> queryParams,
			Map<String, String> pathParams,
			AuthType authType,
			ContentType contentType) {
		
		RequestSpecification request = setupRequest(BaseUrl, authType, contentType);
		applyParams(request, queryParams, pathParams);
	Response response = request.body(body).put(endPoint).then().spec(responseSpec200or201).extract().response();
	response.prettyPrint();
	return response;
	}
	/**
	 * 
	 * @param <T>
	 * @param BaseUrl
	 * @param endPoint
	 * @param body
	 * @param queryParams
	 * @param pathParams
	 * @param authType
	 * @param contentType
	 * @return
	 */
	public <T>Response patch(String BaseUrl, String endPoint, T body,
			Map<String, String> queryParams,
			Map<String, String> pathParams,
			AuthType authType,
			ContentType contentType) {
		
		RequestSpecification request = setupRequest(BaseUrl, authType, contentType);
		applyParams(request, queryParams, pathParams);
	Response response = request.body(body).patch(endPoint).then().spec(responseSpec200or201).extract().response();
	response.prettyPrint();
	return response;
	}
	/**
	 * @param BaseUrl
	 * @param endPoint
	 * @param queryParams
	 * @param pathParams
	 * @param authType
	 * @param contentType
	 * @return
	 */
	public <T>Response delete(String BaseUrl, String endPoint,
			Map<String, String> queryParams,
			Map<String, String> pathParams,
			AuthType authType,
			ContentType contentType) {
		
		RequestSpecification request = setupRequest(BaseUrl, authType, contentType);
		applyParams(request, queryParams, pathParams);
	Response response = request.delete(endPoint).then().spec(responseSpec204).extract().response();
	response.prettyPrint();
	return response;
	}

}
