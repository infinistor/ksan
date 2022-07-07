/*
 * Copyright 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
 * KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 
 * 3 of the License.  See LICENSE.md for details
 *
 * 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
 * KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
 * KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
 */

package com.pspace.s3gw.basic;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.common.io.BaseEncoding;

import org.junit.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public class MeteringTest extends TestCase {
	//private final static Logger logger = LoggerFactory.getLogger(mybasictest.class);

	@Test
	// @Tag("버킷의 접근권한 블록 설정 확인")
	public void test_singing() {

		String stringToSign = "PUT\n" + "\n" + "\n" + // text/plain; charset=UTF-8\n" +
				"1611208399\n" + "/updown-test-ss/my-updowntest-Object-0000\n";

		// Sign string
		String credential = "e64326fd35b74bbf5f5dd36b";
		Mac mac;
		try {
			mac = Mac.getInstance("HmacSHA1");
			mac.init(new SecretKeySpec(credential.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
		} catch (InvalidKeyException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		System.out.println(BaseEncoding.base64().encode(mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8))));
	}

	@Test
	// @Tag("버킷의 접근권한 블록 설정 확인")
	public void test_singing1() {

		String stringToSign = "PUT\n" + "\n" + "\n" + // text/plain; charset=UTF-8\n" +
				"1611208399\n" + "/updown-test-ss/my-updowntest-Object-0000\n";

		// Sign string
		String credential = "e64326fd35b74bbf5f5dd36b";
		Mac mac;
		try {
			mac = Mac.getInstance("HmacSHA1");
			mac.init(new SecretKeySpec(credential.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
		} catch (InvalidKeyException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		System.out.println(BaseEncoding.base64().encode(mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8))));
	}

	@Test
	public void test_arraystring() {
		String[] path = new String[] { "/", "bucket" };
		String bucket = path[1];
		path = new String[] { "/", bucket, "object" };

		for (int i = 0; i < path.length; i++) {
			System.out.println(path[i]);
		}
	}

	@Test
	public void test_substring() {
		String header = "Content-Disposition: form-data; name=\"x-amz-meta-x-amz-meta-data123\"";
		String parseHeader = header.substring("Content-Disposition: form-data; name=\"".length());
		parseHeader = parseHeader.substring(0, parseHeader.lastIndexOf("\""));
		System.out.println(parseHeader);
	}

	@Test
	public void test_slashcase() {
		String foo = "\\$foo";
		String footxt = "\\$foo.txt";

		if (footxt.startsWith(foo)) {
			System.out.println(foo);
		}
	}

	private static byte[] signMessage(byte[] data, byte[] key, String algorithm)
			throws InvalidKeyException, NoSuchAlgorithmException {
		Mac mac = Mac.getInstance(algorithm);
		mac.init(new SecretKeySpec(key, algorithm));
		return mac.doFinal(data);
	}

	private static String getMessageDigest(byte[] payload, String algorithm) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance(algorithm);
		byte[] hash = md.digest(payload);
		return BaseEncoding.base16().lowerCase().encode(hash);
	}

	@Test
	public void test_replication() {
		String arnPath = "arn:aws:s3::kkk:";
		String[] Path1 = arnPath.split(":", -1);
		for (int i = 0; i < Path1.length; i++) {
			System.out.println(Path1[i]);
		}

		Pattern pattern = Pattern.compile(":");
		Matcher matcher = pattern.matcher(arnPath);

		int from = 0;
		int count = 0;
		while (matcher.find(from)) {
			count++;
			from = matcher.start() + 1;
		}
		System.out.println(count);
		System.out.println(Path1.length);

	}

	@Test
	public void test_v4test() throws InvalidKeyException, NoSuchAlgorithmException {
		String canonicalRequestString = "GET\n" + "/new-bucket-dd080bc9/\n" + "delimiter=%2F&max-keys=1000&prefix=\n"
				+ "host:192.168.11.229:8080\n" + "user-agent:S3 Browser 9.5.5 https://s3browser.com\n"
				+ "x-amz-content-sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\n"
				+ "x-amz-date:20211021T094124Z\n" + "\n" + "host;user-agent;x-amz-content-sha256;x-amz-date\n"
				+ "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

		// Sign string
		String credential = "f6454a51688c4a77e5d6acd6";
		String region = "us-east-1";
		String service = "s3";
		String date = "20211021";
		String date1 = "20211021T094124Z";
		byte[] dateKey = signMessage(date.getBytes(StandardCharsets.UTF_8),
				("AWS4" + credential).getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		byte[] dateRegionKey = signMessage(region.getBytes(StandardCharsets.UTF_8), dateKey, "HmacSHA256");
		byte[] dateRegionServiceKey = signMessage(service.getBytes(StandardCharsets.UTF_8), dateRegionKey,
				"HmacSHA256");
		byte[] signingKey = signMessage("aws4_request".getBytes(StandardCharsets.UTF_8), dateRegionServiceKey,
				"HmacSHA256");

		String canonicalRequest = getMessageDigest(canonicalRequestString.getBytes(StandardCharsets.UTF_8), "SHA-256");
		// System.out.println(canonicalRequest);
		String signatureString = "AWS4-HMAC-SHA256\n" + date1 + "\n" + date + "/" + region + "/s3/aws4_request\n"
				+ canonicalRequest;

		byte[] signature = signMessage(signatureString.getBytes(StandardCharsets.UTF_8), signingKey, "HmacSHA256");

		System.out.println(BaseEncoding.base16().lowerCase().encode(signature));
	}

	@Test
	public void test_nullbyte() {
		String foo = "\\$foo";
		String footxt1 = "";

		System.out.println(foo.getBytes().length);
		System.out.println(footxt1.getBytes().length);
	}

	@Test
	// @Tag("버킷의 접근권한 블록 설정 확인")
	public void test_singing2() {

		String stringToSign = "PUT\n" + "JtzyEoyd+bnscnBFtDcfsg==\n" + "application/xml\n" + // text/plain;
																								// charset=UTF-8\n" +
				"\n" + "x-amz-date:Wed, 08 Sep 2021 08:40:05 GMT\n"
				+ "/s3-test-core-k3vu8ec1ku5gt3g3hax3o6xoy6a5ou-vn0ts";

		// Sign string
		String credential = "1d62bf0566e564d12e6799e3";
		Mac mac;
		try {
			mac = Mac.getInstance("HmacSHA1");
			mac.init(new SecretKeySpec(credential.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
		} catch (InvalidKeyException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		System.out.println(BaseEncoding.base64().encode(mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8))));
	}

	@Test
	public void test_replace() {
		String ttt = "<wstxns9999AccessControlPolicy><Owner><ID>1002</ID><DisplayName>jw01</DisplayName></Owner><AccessControlList><Grant><Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"CanonicalUser\"><ID>1002</ID><DisplayName>jw01</DisplayName><EmailAddress/><URI/></Grantee><Permission>FULL_CONTROL</Permission></Grant><Grant><Grantee xmlns:wstxns80=\"http://www.w3.org/2001/XMLSchema-instance\" wstxns80:type=\"CanonicalUser\"><ID>1003</ID><DisplayName/><EmailAddress/><URI/></Grantee><Permission>READ</Permission></Grant><Grant><Grantee xmlns:wstxns3=\"http://www.w3.org/2001/XMLSchema-instance\" wstxns3:type=\"CanonicalUser\"><ID>1003</ID><DisplayName/><EmailAddress/><URI/></Grantee><Permission>WRITE</Permission></Grant><Grant><Grantee xmlns:wstxns4=\"http://www.w3.org/2001/XMLSchema-instance\" wstxns4:type=\"CanonicalUser\"><ID>1003</ID><DisplayName/><EmailAddress/><URI/></Grantee><Permission>READ_ACP</Permission></Grant><Grant><Grantee xmlns:wstxns5=\"http://www.w3.org/2001/XMLSchema-instance\" wstxns5:type=\"CanonicalUser\"><ID>1003</ID><DisplayName/><EmailAddress/><URI/></Grantee><Permission>WRITE_ACP</Permission></Grant><Grant><Grantee xmlns:wstxns6=\"http://www.w3.org/2001/XMLSchema-instance\" wstxns6:type=\"CanonicalUser\"><ID>1003</ID><DisplayName/><EmailAddress/><URI/></Grantee><Permission>FULL_CONTROL</Permission></Grant></AccessControlList></AccessControlPolicy>";
		ttt = ttt.replaceAll("wstxns[1-9]*", "xsi");

		System.out.println(ttt);
	}

	@Test
	public void test_aldb() {
		String uniqname = "zzbucket";
		String objectpath = "test/test1/test3/test4/test5/";
		String[] pathlist = objectpath.split("/");
		String path = "";

		String query = "select inoid from " + uniqname + " where binpath = '/';";
		System.out.println(query);

		for (int i = 0; i < pathlist.length; i++) {
			if (i == pathlist.length - 1 && objectpath.endsWith("/") == false) {
				path += pathlist[i];
			} else {
				path += pathlist[i] + "/";
			}

			query = "select inoid from " + uniqname + " where binpath = '" + path + "';";
			System.out.println(query);
		}
	}

	@Test
	public void test_website() {
		String uniqname = "/website/bucket/object";
		String[] ttt = uniqname.split("/", 4);

		for (String a : ttt) {
			System.out.println(a);
		}

		String[] aaa = new String[ttt.length - 1];
		for (int i = 0; i < ttt.length; i++) {
			if (i == 0) {
				aaa[i] = ttt[i];
				continue;
			}

			if (i == 1) {
				continue;
			}

			aaa[i - 1] = ttt[i];
		}

		for (String a : aaa) {
			System.out.println(a);
		}

		uniqname = "website/*";
		String[] kkk = uniqname.split("/", 2);
		System.out.println(kkk[0]);
		System.out.println(kkk[1]);
		System.out.println(kkk.length);
	}

	@Test
	public void test_bucketinfo() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><BucketInfo><BucketName>new-bucket-d9e1c51b</BucketName><AccessControlPolicy xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"><Owner><ID>1585</ID><DisplayName>test_user</DisplayName></Owner><AccessControlList><Grant><Grantee xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"CanonicalUser\"><ID>1585</ID><DisplayName>test_user</DisplayName></Grantee><Permission>FULL_CONTROL</Permission></Grant></AccessControlList></AccessControlPolicy><WebsiteConfiguration/><CORSConfiguration/><LifecycleConfiguration/><VersioningConfiguration/><PublicAccessBlockConfiguration/><Tagging/><ServerSideEncryptionConfiguration/></BucketInfo>";
		int startAcl = xml.indexOf("<AccessControlPolicy ");
		int endAcl = xml.indexOf("</AccessControlPolicy>");
		String kk = "</AccessControlPolicy>";

		System.out.println(xml.substring(startAcl, endAcl + kk.length()));
	}

	@Test
	public void test_genreqid() {

		String uuid = UUID.randomUUID().toString().substring(24).toUpperCase();
		System.out.println(uuid);
	}

	@Test
	public void test_prefix() {
		String delimiter = "k";
		String prefix1 = "New Folder";
		String prefix = "New Folder/New Folder";
		int end = prefix.lastIndexOf(delimiter) + delimiter.length();
		System.out.println(prefix.substring(prefix1.length(), prefix.length()));
		String subname = prefix.substring(prefix1.length(), prefix.length());
		System.out.println(subname.indexOf(delimiter));
		System.out.println(end);
		delimiter = "/";
		end = prefix.lastIndexOf(delimiter);
		System.out.println(prefix.substring(end, prefix.length()));
		System.out.println(end);
	}
}