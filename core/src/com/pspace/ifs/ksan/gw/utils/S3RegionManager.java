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
package com.pspace.ifs.ksan.gw.utils;

import java.util.HashSet;
import java.util.Set;

import com.pspace.ifs.ksan.gw.identity.S3Region;
import com.pspace.ifs.ksan.gw.identity.S3User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3RegionManager {
    private static final Logger logger = LoggerFactory.getLogger(S3RegionManager.class);
    private Set<S3Region> regions;

    public static S3RegionManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder {
        private static final S3RegionManager INSTANCE = new S3RegionManager();
    }

    private S3RegionManager() {
        regions = new HashSet<S3Region>();
    }

    public void addRegion(S3Region region) {
        regions.add(region);
    }

    public void removeRegion(S3Region region) {
        regions.remove(region);
    }

    public S3Region getRegionByName(String name) {
        for (S3Region region : regions) {
			if (region.getName().equals(name)) {
				return region;
			}
		}

        return null;
    }

    public S3Region getRegionByKey(String accessKey) {
        for (S3Region region : regions) {
			if (region.getAccessKey().equals(accessKey)) {
				return region;
			}
		}

        return null;
    }

    public String findNameWithAccessKey(String accessKey) {
        for (S3Region region : regions) {
            if (region.getAccessKey().equals(accessKey)) {
                return region.getName();
            }
        }

        return null;
    }

    public S3User getUserInfoByKey(String accessKey) {
        for (S3Region region : regions) {
            if (region.getAccessKey().equals(accessKey)) {
                S3User user = new S3User();
                user.setAccessKey(region.getAccessKey());
                user.setAccessSecret(region.getAccessSecret());
                user.setUserName(region.getName());
                return user;
            }
        }

        return null;
    }

    public void printRegions() {
        for (S3Region region : regions) {
            logger.info("Name:{}, Address:{}, port:{}, sslport:{}, AccessKey:{}, SecretKey:{}", region.getName(), region.getAddress(), region.getPort(), region.getSslPort(), region.getAccessKey(), region.getAccessSecret());
        }
    }
}
