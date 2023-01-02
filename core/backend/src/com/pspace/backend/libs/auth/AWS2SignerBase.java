package com.pspace.backend.libs.auth;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class AWS2SignerBase {

	public static String GetBase64EncodedSHA1Hash(String Policy, String SecretKey) throws NoSuchAlgorithmException, InvalidKeyException {
		var signingKey = new SecretKeySpec(SecretKey.getBytes(), "HmacSHA1");
		Mac mac;
		mac = Mac.getInstance("HmacSHA1");
		mac.init(signingKey);

		var encoder = Base64.getEncoder();
		return encoder.encodeToString((mac.doFinal(Policy.getBytes())));
	}
}
