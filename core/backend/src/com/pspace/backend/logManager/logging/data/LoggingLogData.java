package logging.data;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

public class LoggingLogData{
	public long Index;
	public String SourceBucket;
	public String TargetBucket;
	public String TargetKey;
	public long LastIndex;
	public DateTime InDate;
	public String ErrorMessage;

	public void Init() {
		Index = 0;
		SourceBucket = "";
		TargetBucket = "";
		TargetKey = "";
		LastIndex = 0;
		InDate = null;
		ErrorMessage = "";
	}

	public List<Object> getInsertDBParameters() {
		var param = new ArrayList<Object>();
		param.add(SourceBucket);
		param.add(TargetBucket);
		param.add(TargetKey);
		param.add(LastIndex);
		param.add(ErrorMessage);
		return param;
	}
}
