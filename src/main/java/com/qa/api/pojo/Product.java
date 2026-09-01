package com.qa.api.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Product {
	
	private Integer id;
	private String title;
	private Double price;
	private String description;
	private String category;
	private String image;
	private Rating rating;
	
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@Data
	
	public static class Rating{
		
		private Double rate;
		private Integer count;
	}
	
	
	

}
