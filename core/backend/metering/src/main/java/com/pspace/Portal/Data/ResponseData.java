package com.pspace.Portal.Data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResponseData <T> {
	@JsonProperty("IsNeedLogin")
	public boolean IsNeedLogin;

	@JsonProperty("AccessDenied")
	public boolean AccessDenied;

	@JsonProperty("Result")
	public String Result;

	@JsonProperty("Code")
	public String Code;

	@JsonProperty("Message")
	public String Message;

	@JsonProperty("Data")
	public T Data;
}
