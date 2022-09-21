package com.pspace.backend.Heartbeat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RequestServiceState {

	public String Id;
	public String State;

	public RequestServiceState(String Id, String State) {
		this.Id = Id;
		this.State = State;
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
