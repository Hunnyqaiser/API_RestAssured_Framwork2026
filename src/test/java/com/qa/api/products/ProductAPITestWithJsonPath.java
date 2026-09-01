package com.qa.api.products;

import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;
import com.qa.api.base.BaseTest;
import com.qa.api.constants.AuthType;
import com.qa.api.utils.JsonPathValidatorUtils;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

public class ProductAPITestWithJsonPath extends BaseTest{
	
	
	@Test
	public void getProductTest()
	{
		Response response =restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT, null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);

		List<Number> prices = JsonPathValidatorUtils.readList(response, "$[?(@.price > 50)].price");
		System.out.println(prices);
		
		List<Number> id = JsonPathValidatorUtils.readList(response, "$[?(@.price > 50)].id");
		System.out.println(id);
		
		List<Double> rates = JsonPathValidatorUtils.readList(response, "$[?(@.price > 50)].rating.rate");
		System.out.println(rates);
		
		List<Integer> count = JsonPathValidatorUtils.readList(response, "$[?(@.price > 50)].rating.count");
		System.out.println(count);
		
		
		List<Map<String, Object>> idAndTitleLsit =JsonPathValidatorUtils.readListOfMaps(response, "$.[*].['id', 'title']");
		System.out.println(idAndTitleLsit);
		
		List<Map<String, Object>> idAndTitleAndCategoryLsit =JsonPathValidatorUtils.readListOfMaps(response, "$.[*].['id', 'title', 'category']");
		System.out.println(idAndTitleAndCategoryLsit);
		
		
		Double minPrice =JsonPathValidatorUtils.read(response, "min($[*].price)");
		System.out.println(minPrice);
	}

@Test
	public void getProductsAbovePriceThresholdPositiveTest() {
		// positive: JSONPath filter for price > 100 returns a non-empty list of prices
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT, null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);

		List<Number> prices = JsonPathValidatorUtils.readList(response, "$[?(@.price > 100)].price");
		Assert.assertFalse(prices.isEmpty(), "There must be products priced above 100");
		List<Number> ids = JsonPathValidatorUtils.readList(response, "$[?(@.price > 100)].id");
		Assert.assertEquals(prices.size(), ids.size(), "price and id lists must be aligned");
	}

	@Test
	public void getProductsBelowPriceFilter_NoMatchReturnsEmptyListTest() {
		// negative: JSONPath filter with no matching product must return an empty list
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT, null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);

		List<Number> prices = JsonPathValidatorUtils.readList(response, "$[?(@.price > 100000)].price");
		Assert.assertTrue(prices.isEmpty(), "No product price is above 100000, the filter must be empty");
	}

	@Test
	public void catalogPricingAggregationE2ETest() {
		// E2E: aggregate pricing facts across the whole catalog must stay consistent
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT, null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);

		List<Number> prices = JsonPathValidatorUtils.readList(response, "$[*].price");
		List<String> categories = JsonPathValidatorUtils.readList(response, "$[*].category");

		Double minPrice = JsonPathValidatorUtils.read(response, "min($[*].price)");
		Double maxPrice = JsonPathValidatorUtils.read(response, "max($[*].price)");

		Assert.assertNotNull(minPrice);
		Assert.assertNotNull(maxPrice);
		Assert.assertTrue(minPrice > 0, "min price must be positive");
		Assert.assertTrue(maxPrice >= minPrice, "max price must be >= min price");

		// every individual price must lie inside [min,max]
		for (Number n : prices) {
			double p = n.doubleValue();
			Assert.assertTrue(p >= minPrice && p <= maxPrice, "price " + p + " must be within catalog range");
		}

		// catalog must span several distinct categories
		Assert.assertTrue(categories.stream().distinct().count() >= 4,
				"Catalog should contain at least 4 distinct categories");
	}

	/**
	 * Edge: filtering by a closed numeric range must return only products
	 * whose price falls inside the range.
	 */
	@Test
	public void getProductsPriceRangeFilterTest() {
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT, null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);

		List<Double> prices = JsonPathValidatorUtils.readList(response, "$[?(@.price >= 10 && @.price <= 50)].price");
		Assert.assertFalse(prices.isEmpty(), "Range [10,50] must match some products");
		for (Double p : prices) {
			Assert.assertTrue(p >= 10.0 && p <= 50.0,
					"Filtered price must be in [10,50], got: " + p);
		}
	}

	/**
	 * Positive: JSONPath projection of {@code [id,title,price]} must return a
	 * list of maps with those three keys.
	 */
	@Test
	public void getProductsProjectionReturnsSubsetOfFieldsTest() {
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT, null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);

		List<Map<String, Object>> projection = JsonPathValidatorUtils.readListOfMaps(response, "$.[*].['id','title','price']");
		Assert.assertFalse(projection.isEmpty(), "Projection must not be empty");
		for (Map<String, Object> row : projection) {
			Assert.assertNotNull(row.get("id"), "Projection row must carry id");
			Assert.assertNotNull(row.get("title"), "Projection row must carry title");
			Assert.assertNotNull(row.get("price"), "Projection row must carry price");
		}
	}

	/**
	 * Positive: filtering by category must yield only items whose
	 * {@code category} matches the filter.
	 */
	@Test
	public void getProductsCategoryFilterTest() {
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT, null, null, AuthType.NO_AUTH, ContentType.ANY);
		Assert.assertEquals(response.statusCode(), 200);

		List<Map<String, Object>> electronics = JsonPathValidatorUtils.readListOfMaps(response, "$[?(@.category == 'electronics')]");
		Assert.assertFalse(electronics.isEmpty(), "Catalog must contain electronics");
		for (Map<String, Object> p : electronics) {
			Assert.assertEquals(p.get("category"), "electronics");
		}
	}

	/**
	 * Aggregation consistency: {@code sum($[*].price)} must equal the manually
	 * summed list of prices. fakestore sends some prices as integers (no
	 * decimal), so we accept {@code Number} to avoid ClassCastException on
	 * iteration.
	 */
	@Test
	public void getProductsSumPriceAggregationConsistentTest() {
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT, null, null, AuthType.NO_AUTH, ContentType.ANY);
		List<Number> prices = JsonPathValidatorUtils.readList(response, "$[*].price");
		Double sum = JsonPathValidatorUtils.read(response, "sum($[*].price)");
		Assert.assertNotNull(sum);
		double manual = prices.stream().mapToDouble(Number::doubleValue).sum();
		Assert.assertEquals(sum.doubleValue(), manual, 0.01, "sum() must equal manual sum");
	}

	/**
	 * Aggregation: {@code avg($[*].price)} must lie inside [min, max].
	 */
	@Test
	public void getProductsAvgPriceWithinRangeTest() {
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT, null, null, AuthType.NO_AUTH, ContentType.ANY);
		Double min = JsonPathValidatorUtils.read(response, "min($[*].price)");
		Double max = JsonPathValidatorUtils.read(response, "max($[*].price)");
		Double avg = JsonPathValidatorUtils.read(response, "avg($[*].price)");
		Assert.assertNotNull(avg);
		Assert.assertTrue(avg >= min && avg <= max,
				"avg must be inside [min,max]: avg=" + avg + " min=" + min + " max=" + max);
	}

	/**
	 * Edge: every product whose {@code title} is longer than 20 chars must be
	 * discoverable from the catalog via JSONPath. fakestore titles are mostly
	 * long descriptions, so this should yield a meaningful count.
	 */
	@Test
	public void getProductsTitleLengthFilterTest() {
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT, null, null, AuthType.NO_AUTH, ContentType.ANY);
		List<String> titles = JsonPathValidatorUtils.readList(response, "$[*].title");
		Assert.assertFalse(titles.isEmpty(), "Catalog must have titles");
		long longTitles = titles.stream().filter(t -> t != null && t.length() > 20).count();
		Assert.assertTrue(longTitles > 0, "Catalog must contain titles longer than 20 chars");
	}

	/**
	 * Edge: every product's {@code rating.rate} must be in [0,5] (range
	 * aggregation).
	 */
	@Test
	public void getProductsRatingRateRangeTest() {
		Response response = restClient.get(BASE_URL_PRODUCTS, PRODUCTS_ENDPOINT, null, null, AuthType.NO_AUTH, ContentType.ANY);
		Double minRate = JsonPathValidatorUtils.read(response, "min($[*].rating.rate)");
		Double maxRate = JsonPathValidatorUtils.read(response, "max($[*].rating.rate)");
		Assert.assertNotNull(minRate);
		Assert.assertNotNull(maxRate);
		Assert.assertTrue(minRate >= 0.0, "min rate must be >= 0");
		Assert.assertTrue(maxRate <= 5.0, "max rate must be <= 5");
	}
}
