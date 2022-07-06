package com.pspace.Portal.Data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResponseConfig {
	@JsonProperty("Type")
	public String Type;

	@JsonProperty("Version")
	public int Version;

	@JsonProperty("Config")
	public String Config;

	@JsonProperty("RegDate")
	public String RegDate;
}
