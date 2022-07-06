package com.pspace.Portal.Data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResponseRegion {
	@JsonProperty("Name")
	public String Name;

	@JsonProperty("Address")
	public String Address;

	@JsonProperty("Port")
	public int Port;

	@JsonProperty("SSLPort")
	public int SSLPort;

	@JsonProperty("AccessKey")
	public String AccessKey;

	@JsonProperty("SecretKey")
	public String SecretKey;

	public String GetURL() {
		return String.format("http://%s:%s", Address, Port);
	}
}
