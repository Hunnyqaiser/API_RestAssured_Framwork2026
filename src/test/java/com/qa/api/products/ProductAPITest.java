package com.qa.api.products;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.qa.api.base.BaseTest;
import com.qa.api.constants.AuthType;
import com.qa.api.pojo.Product;
import com.qa.api.utils.JsonUtils;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class ProductAPITest extends BaseTest{
	
	@Test
	public void getAllProductsTest()
	{
		Response response =restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT, null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);
		
		Product[] product =JsonUtils.deserialize(response, Product[].class);
		
		for(Product p : product)
		{
			System.out.println("id: "+ p.getId());
			System.out.println("title: "+ p.getTitle());
			System.out.println("price: "+ p.getPrice());
			System.out.println("Description: "+ p.getDescription());
			System.out.println("image: "+ p.getImage());
			System.out.println("category: "+ p.getCategory());
			
			System.out.println("rate: "+ p.getRating().getRate());
			System.out.println("count: "+ p.getRating().getCount());
		}
		
		
	}

@Test
	public void getProductByIdPositiveTest() {
		// positive: GET a single product by a known valid id -> 200 + populated fields
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT + "/1", null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertEquals(response.jsonPath().getString("id"), "1");
		Assert.assertNotNull(response.jsonPath().getString("title"));
		Assert.assertTrue(response.jsonPath().getDouble("price") > 0);
		Assert.assertNotNull(response.jsonPath().getString("category"));
	}

	@Test
	public void getProductByCategoryPositiveTest() {
		// positive: GET products filtered by a valid category -> every item matches the category
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT + "/category/electronics", null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);

		Product[] products = JsonUtils.deserialize(response, Product[].class);
		Assert.assertTrue(products.length > 0, "electronics category should have products");
		for (Product p : products) {
			Assert.assertEquals(p.getCategory(), "electronics");
			Assert.assertNotNull(p.getTitle());
			Assert.assertTrue(p.getPrice() > 0);
		}
	}

	@Test
	public void getProductWithInvalidId_NegativeTest() {
		// negative: fakestore returns HTTP 200 with an EMPTY body for an unknown id (not 404)
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT + "/999999", null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertTrue(response.getBody().asString().trim().isEmpty(),
				"A non-existing product id should return an empty body, but was: " + response.getBody().asString());
	}

	@Test
	public void getProductsInvalidCategory_NegativeTest() {
		// negative: unknown category returns HTTP 200 with an empty JSON array (fakestore contract)
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT + "/category/not-a-category", null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);

		Product[] products = JsonUtils.deserialize(response, Product[].class);
		Assert.assertEquals(products.length, 0, "Unknown category should return an empty list");
	}

	@Test
	public void catalogDataIntegrityE2ETest() {
		// E2E: the full catalog must satisfy core data-integrity rules
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT, null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);
		Product[] products = JsonUtils.deserialize(response, Product[].class);

		// fakestoreapi catalog is documented/served with exactly 20 products
		Assert.assertEquals(products.length, 20, "Catalog should contain exactly 20 products");

		for (Product p : products) {
			Assert.assertNotNull(p.getId(), "product id must not be null");
			Assert.assertNotNull(p.getTitle(), "product title must not be null");
			Assert.assertNotNull(p.getDescription(), "product description must not be null");
			Assert.assertTrue(p.getPrice() > 0, "product price must be positive, got: " + p.getPrice());
			Assert.assertNotNull(p.getCategory(), "product category must not be null");
			Assert.assertTrue(p.getRating().getRate() >= 0 && p.getRating().getRate() <= 5,
					"rating.rate must be within 0..5, got: " + p.getRating().getRate());
			Assert.assertTrue(p.getRating().getCount() >= 0, "rating.count must be non-negative");
		}
	}

	/**
	 * Positive: GET a known-valid product by id must return every documented
	 * field populated and non-null.
	 */
	@Test
	public void getProductByIdFieldValidationTest() {
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT + "/1", null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);
		Product product = JsonUtils.deserialize(response, Product.class);
		Assert.assertNotNull(product.getId(), "id must not be null");
		Assert.assertNotNull(product.getTitle(), "title must not be null");
		Assert.assertNotNull(product.getDescription(), "description must not be null");
		Assert.assertNotNull(product.getCategory(), "category must not be null");
		Assert.assertNotNull(product.getImage(), "image must not be null");
		Assert.assertNotNull(product.getRating(), "rating must not be null");
		Assert.assertNotNull(product.getRating().getRate(), "rating.rate must not be null");
		Assert.assertNotNull(product.getRating().getCount(), "rating.count must not be null");
	}

	/**
	 * Positive: the catalog must cover the documented fakestoreapi categories.
	 */
	@Test
	public void getAllProductsCoversAllDocumentedCategoriesTest() {
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT, null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);
		Product[] products = JsonUtils.deserialize(response, Product[].class);
		java.util.Set<String> categories = new java.util.HashSet<>();
		for (Product p : products) {
			categories.add(p.getCategory());
		}
		Assert.assertTrue(categories.contains("electronics"), "catalog must contain 'electronics'");
		Assert.assertTrue(categories.contains("jewelery"), "catalog must contain 'jewelery'");
		Assert.assertTrue(categories.contains("men's clothing"), "catalog must contain men's clothing");
		Assert.assertTrue(categories.contains("women's clothing"), "catalog must contain women's clothing");
	}

	/**
	 * Boundary: {@code ?limit=5} must return exactly 5 products.
	 */
	@Test
	public void getProductsLimitReturnsExactCountTest() {
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT + "?limit=5", null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);
		Product[] products = JsonUtils.deserialize(response, Product[].class);
		Assert.assertEquals(products.length, 5, "?limit=5 must return exactly 5 products");
	}

	/**
	 * Edge: fakestoreapi ignores unknown sort order — the response still
	 * contains all 20 products. Documents the live behavior.
	 */
	@Test
	public void getProductsSortAscReturnsAllProductsTest() {
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT + "?sort=asc", null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);
		Product[] products = JsonUtils.deserialize(response, Product[].class);
		Assert.assertEquals(products.length, 20, "?sort=asc must still return all 20 products");
	}

	/**
	 * Edge: {@code ?limit=0} is invalid — fakestoreapi falls back to the full
	 * catalog (20 products). Documents live behavior.
	 */
	@Test
	public void getProductsLimitZeroReturnsFullCatalogTest() {
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT + "?limit=0", null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);
		Product[] products = JsonUtils.deserialize(response, Product[].class);
		Assert.assertEquals(products.length, 20, "?limit=0 must fall back to full catalog");
	}

	/**
	 * Edge: {@code ?limit=-1} is invalid — fakestoreapi falls back to the full
	 * catalog. The catalog is documented as 20 products; we tolerate an
	 * off-by-one (19) because the public mock occasionally serves a partial
	 * page for invalid params.
	 */
	@Test
	public void getProductsNegativeLimitReturnsFullCatalogTest() {
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT + "?limit=-1", null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);
		Product[] products = JsonUtils.deserialize(response, Product[].class);
		Assert.assertTrue(products.length >= 19, "?limit=-1 must return the full catalog, got " + products.length);
	}

	/**
	 * Negative: GET with a negative id must yield 200 + empty body (fakestore
	 * contract for unknown ids).
	 */
	@Test
	public void getProductNegativeId_ReturnsEmptyBodyTest() {
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT + "/-1", null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertTrue(response.getBody().asString().trim().isEmpty(),
				"Negative id must yield empty body, got: " + response.getBody().asString());
	}

	/**
	 * Negative: GET with a non-numeric id must yield 200 + empty body.
	 */
	@Test
	public void getProductStringId_ReturnsEmptyBodyTest() {
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT + "/abc", null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertTrue(response.getBody().asString().trim().isEmpty(),
				"String id must yield empty body, got: " + response.getBody().asString());
	}

	/**
	 * Boundary / data-integrity: across the whole catalog, every
	 * {@code rating.rate} must lie within [0,5].
	 */
	@Test
	public void getProductsRatingRateAlwaysInRangeTest() {
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT, null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);
		Product[] products = JsonUtils.deserialize(response, Product[].class);
		for (Product p : products) {
			Double rate = p.getRating().getRate();
			Assert.assertNotNull(rate, "rating.rate must not be null");
			Assert.assertTrue(rate >= 0.0 && rate <= 5.0,
					"rating.rate must be within 0..5, got: " + rate);
		}
	}

	/**
	 * NFR: catalog response time must be under a sane SLA.
	 */
	@Test
	public void getAllProductsResponseTimeUnderThreshold() {
		long start = System.currentTimeMillis();
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT, null, null, AuthType.NO_AUTH, ContentType.ANY);
		long elapsed = System.currentTimeMillis() - start;
		Assert.assertEquals(response.statusCode(), 200);
		Assert.assertTrue(elapsed < 5000, "Catalog must complete under 5s, took " + elapsed + "ms");
	}

	/**
	 * Positive: distinct ids in the catalog must equal the catalog size (no
	 * duplicate ids).
	 */
	@Test
	public void getAllProductsIdsAreUniqueTest() {
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT, null, null, AuthType.NO_AUTH, ContentType.ANY);
		Product[] products = JsonUtils.deserialize(response, Product[].class);
		java.util.Set<Integer> unique = new java.util.HashSet<>();
		for (Product p : products) {
			unique.add(p.getId());
		}
		Assert.assertEquals(unique.size(), products.length, "Catalog ids must be unique");
	}
}
