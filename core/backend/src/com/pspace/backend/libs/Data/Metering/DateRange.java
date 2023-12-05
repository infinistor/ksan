package com.pspace.backend.libs.Data.Metering;
public class DateRange {
	
	public String start;
	public String end;

	public DateRange(String start, String end){
		this.start = start;
		this.end = end;
	}

	@Override
	public String toString() {
		return "DateRange [start=" + start + ", end=" + end + "]";
	}
}
