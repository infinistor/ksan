package com.pspace.backend.Data.Portal;

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
