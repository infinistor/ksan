package com.pspace.Utility;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {
	static final Logger logger = LoggerFactory.getLogger(Util.class);

	public static boolean isRun(int Time) {
		return Time == GetNowTimetoInt();
	}

	public static int GetNowDay() {
		Calendar time = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"), Locale.KOREA);
		return time.get(Calendar.DAY_OF_YEAR);
	}

	public static int GetNowTimetoInt() {
		Calendar time = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"), Locale.KOREA);
		int Hour = time.get(Calendar.HOUR_OF_DAY);
		int Minute = time.get(Calendar.MINUTE);

		return Hour * 60 + Minute;
	}

	public static int String2Time(String Value) {
		if (Value == null || Value.isBlank())
			return -1;

		String Token[] = Value.split(":");
		if (Token.length > 1) {
			int Hour = Integer.parseInt(Token[0]);
			int Min = Integer.parseInt(Token[1]);
			return Hour * 60 + Min;
		} else
			return -1;
	}

	public static String ReadServiceId(String FilePath) {
		try {
			BufferedReader Reader = new BufferedReader(new FileReader(FilePath));
			var ServiceId = Reader.readLine();
			Reader.close();
			return ServiceId;
		} catch (Exception e) {
			return null;
		}
	}
}
