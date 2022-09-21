package com.pspace.backend.Data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LifecycleConfig {

	@JsonProperty("objM.db_repository")
	public String DBType;

	@JsonProperty("objM.db_host")
	public String Host;

	@JsonProperty("objM.db_port")
	public int Port;

	@JsonProperty("objM.db_name")
	public String DatabaseName;

	@JsonProperty("objM.db_user")
	public String User;

	@JsonProperty("objM.db_password")
	public String Password;

	@JsonProperty("ksan.region")
	public String Region;

	@JsonProperty("lifecycle.schedule")
	public String Schedule;

	@JsonProperty("lifecycle.check_interval")
	public long CheckInterval;

	@Override
	public String toString() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return "";
		}
	}
}
