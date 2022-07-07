/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.ifs.ksan.gw.identity;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3User {
    private final Logger logger = LoggerFactory.getLogger(S3User.class);
    public static final String DATA = "Data";
    public static final String ITEMS = "Items";
    public static final String USER_DISK_POOLS = "UserDiskPools";

    public static final String USER_ID = "Id";
    public static final String USER_NAME = "Name";
    public static final String USER_EMAIL = "Email";
    public static final String ACCESS_KEY = "AccessKey";
    public static final String ACCESS_SECRET = "SecretKey";
    public static final String USER_DISK_POOLS_USER_ID = "UserId";
    public static final String USER_DISK_POOLS_DISKPOOL_ID = "DiskPoolId";
    public static final String USER_DISK_POOLS_STORAGE_CLASS = "StorageClass";
    public static final String STANDARD = "STANDARD";

    private String userId;
    private String userName;
    private String userEmail;
	private String accessKey;
	private String accessSecret;
    private List<HashMap<String, String>> userDiskPools;

    public S3User() {
        userId = "";
        userName = "";
        userEmail = "";
        accessKey = "";
        accessSecret = "";
        userDiskPools = new ArrayList<HashMap<String, String>>();
    }

    public S3User(String id, String name, String email, String access, String secret, JSONArray diskpools) {
        this.userId = id;
        this.userName = name;
        this.userEmail = email;
        this.accessKey = access;
        this.accessSecret = secret;
        userDiskPools = new ArrayList<HashMap<String, String>>();
        for (int i = 0; i < diskpools.size(); i++) {
            JSONObject item = (JSONObject)diskpools.get(i);
            HashMap<String, String> map = new HashMap<String, String>();
            map.put(USER_DISK_POOLS_DISKPOOL_ID, (String)item.get(S3User.USER_DISK_POOLS_DISKPOOL_ID));
            map.put(USER_DISK_POOLS_STORAGE_CLASS, (String)item.get(S3User.USER_DISK_POOLS_STORAGE_CLASS));
            userDiskPools.add(map);
        }
    }

    public String getUserName() {
        return Strings.nullToEmpty(userName);
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return Strings.nullToEmpty(userEmail);
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getAccessKey() {
        return Strings.nullToEmpty(accessKey);
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getAccessSecret() {
        return Strings.nullToEmpty(accessSecret);
    }

    public void setAccessSecret(String accessSecret) {
        this.accessSecret = accessSecret;
    }

    public List<HashMap<String, String>> getUserDiskpools() {
        return userDiskPools;
    }

    public String getUserDefaultDiskId() {
        for (HashMap<String, String> map : userDiskPools) {
            if (map.get(USER_DISK_POOLS_STORAGE_CLASS).equals(STANDARD)) {
                return map.get(USER_DISK_POOLS_DISKPOOL_ID);
            }
        }
        return null;
    }
}
