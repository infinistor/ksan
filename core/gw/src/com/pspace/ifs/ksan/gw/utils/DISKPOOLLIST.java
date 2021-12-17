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

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/** Represent an Amazon Versioning for a container or object. */
// CHECKSTYLE:OFF
@JacksonXmlRootElement(localName = "DISKPOOLLIST")
public final class DISKPOOLLIST {
    @JacksonXmlProperty(localName = "DISKPOOL")
    public DISKPOOL diskpool;

	public static final class DISKPOOL {
        @JacksonXmlProperty(isAttribute = true)
		private String id;
        
        @JacksonXmlProperty(isAttribute = true)
        private String name;

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "SERVER")
        private List<SERVER> servers;

        public static final class SERVER {
            @JacksonXmlProperty(isAttribute = true)
		    private String id;
            @JacksonXmlProperty(isAttribute = true)
		    private String ip;
            @JacksonXmlProperty(isAttribute = true)
		    private String status;

            @JacksonXmlElementWrapper(useWrapping = false)
            @JacksonXmlProperty(localName = "DISK")
            private List<DISK> disks;

            public static final class DISK {
                @JacksonXmlProperty(isAttribute = true)
		        private String id;
                @JacksonXmlProperty(isAttribute = true)
		        private String path;
                @JacksonXmlProperty(isAttribute = true)
		        private String mode;
                @JacksonXmlProperty(isAttribute = true)
		        private String status;

                public String getId() {
                    return id;
                }
                public void setId(String id) {
                    this.id = id;
                }
                public String getPath() {
                    return path;
                }
                public void setPath(String path) {
                    this.path = path;
                }
                public String getMode() {
                    return mode;
                }
                public void setMode(String mode) {
                    this.mode = mode;
                }
                public String getStatus() {
                    return status;
                }
                public void setStatus(String status) {
                    this.status = status;
                }
                
            }

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public String getIp() {
                return ip;
            }

            public void setIp(String ip) {
                this.ip = ip;
            }

            public String getStatus() {
                return status;
            }

            public void setStatus(String status) {
                this.status = status;
            }

            public List<DISK> getDisks() {
                return disks;
            }

            public void setDisks(List<DISK> disks) {
                this.disks = disks;
            }
            
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<SERVER> getServers() {
            return servers;
        }

        public void setServers(List<SERVER> servers) {
            this.servers = servers;
        }
        
    }

    public DISKPOOL getDiskpool() {
        return diskpool;
    }

    public void setDiskpool(DISKPOOL diskpool) {
        this.diskpool = diskpool;
    }
    
}
// CHECKSTYLE:ON
