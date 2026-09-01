package com.qa.api.pojo;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(Include.NON_NULL) //this annotation will solve the null value problem if we use same POJO for serialization!!
public class User {
	private Integer id;
	private String name;
	private String email;
	private String gender;
	private String status;

}
