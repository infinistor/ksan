package com.pspace.backend.Data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pspace.DB.DBConfig;

public class LifecycleConfig {
	@JsonProperty("Host")
	public String Host;
	
	@JsonProperty("Port")
	public int Port;

	@JsonProperty("User")
	public String User;

	@JsonProperty("Password")
	public String Password;

	@JsonProperty("DatabaseName")
	public String DatabaseName;

	@JsonProperty("Region")
	public String Region;

	public DBConfig GetDBConfig() {
		return new DBConfig(Host, Port, DatabaseName, User, Password);
	}
	
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
