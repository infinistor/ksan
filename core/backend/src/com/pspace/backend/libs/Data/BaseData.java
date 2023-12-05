package com.pspace.backend.libs.Data;

import java.util.List;

// import org.bson.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface BaseData {

	@JsonIgnore
	public List<Object> getInsertDBParameters();

	// @JsonIgnore
	// public Document getInsertDBDocument();
}
