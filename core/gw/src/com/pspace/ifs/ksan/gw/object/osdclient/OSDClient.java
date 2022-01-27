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
package com.pspace.ifs.ksan.gw.object.osdclient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;

import com.pspace.ifs.ksan.gw.utils.GWConstants;
import com.pspace.ifs.ksan.osd.OSDConstants;
import com.pspace.ifs.ksan.osd.OSDData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OSDClient {
	private String host;
	private Socket socket;
	private Logger logger;
	private OutputStream byPassOut;
	private long fileSize;
	private MessageDigest md5er;

	public OSDClient(String ipAddress, int port) throws UnknownHostException, IOException {
		logger = LoggerFactory.getLogger(OSDClient.class);
		host = ipAddress;
		socket = new Socket(ipAddress, port);
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        logger.debug(GWConstants.LOG_OSDCLIENT_SOCKET_INFO, socket.toString());
	}

	public Socket getSocket() {
		return socket;
	}
	
	public void disconnect() throws IOException {
		socket.close();
	}

	public void close() {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				logger.error(GWConstants.LOG_OSDCLIENT_SOCKET_ERROR, e.getMessage());
			} finally {
				socket = null;
			}
		}
	}

	public void getInit(String path, String objId, String versionId, long fileSize, String sourceRange, OutputStream out) throws IOException {
		String header = OSDConstants.GET + GWConstants.COLON + path + GWConstants.COLON + objId + GWConstants.COLON + versionId + GWConstants.COLON + sourceRange;
		logger.debug(GWConstants.LOG_OSDCLIENT_HEADER, header);
		sendHeader(header);
		this.fileSize = fileSize;
		byPassOut = out;
	}

	public long get() throws IOException {
		byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
		int readByte = OSDConstants.MAXBUFSIZE;
		int readLength = 0;
		long readTotal = 0L;
		
		while ((readLength = socket.getInputStream().read(buffer, 0, readByte)) >= 0) {
			readTotal += readLength;
			byPassOut.write(buffer, 0, readLength);
			logger.debug(GWConstants.LOG_OSDCLIENT_WRITE, readLength);
			if (readTotal >= fileSize) {
				break;
			}
		}
		byPassOut.flush();

		return readTotal;
	}

	public void getPartInit(String path, String objId, String partNo, long fileSize, OutputStream out, MessageDigest md5er) throws IOException {
		String header = OSDConstants.GET_PART + GWConstants.COLON + path + GWConstants.COLON + objId + GWConstants.COLON + partNo;
		logger.debug(GWConstants.LOG_OSDCLIENT_HEADER, header);
		sendHeader(header);
		this.fileSize = fileSize;
		byPassOut = out;
		this.md5er = md5er;
	}

	public long getPart() throws IOException {
		byte[] buffer = new byte[OSDConstants.MAXBUFSIZE];
		int readByte = OSDConstants.MAXBUFSIZE;
		int readLength = 0;
		long readTotal = 0L;
		
		while ((readLength = socket.getInputStream().read(buffer, 0, readByte)) >= 0) {
			readTotal += readLength;
			byPassOut.write(buffer, 0, readLength);
			md5er.update(buffer, 0, readLength);
			logger.debug(GWConstants.LOG_OSDCLIENT_WRITE, readLength);
			if (readTotal >= fileSize) {
				break;
			}
		}
		byPassOut.flush();

		return readTotal;
	}

	public void putInit(String path, String objId, String versionId, long length, String replication, String replicaDiskID) throws IOException {
		String header = OSDConstants.PUT 
						+ GWConstants.COLON + path 
						+ GWConstants.COLON + objId 
						+ GWConstants.COLON + versionId 
						+ GWConstants.COLON + String.valueOf(length) 
						+ GWConstants.COLON + replication 
						+ GWConstants.COLON + replicaDiskID;
		logger.debug(GWConstants.LOG_OSDCLIENT_PUT_HEADER, header);
		sendHeader(header);
	}

	public void put(byte[] buffer, int offset, int length) throws IOException {
		socket.getOutputStream().write(buffer, offset, length);
	}

	public void putFlush() throws IOException {
		socket.getOutputStream().flush();
	}

	public void delete(String path, String objId, String versionId) throws IOException {
		String header = OSDConstants.DELETE + GWConstants.COLON + path + GWConstants.COLON + objId + GWConstants.COLON + versionId;
		logger.debug(GWConstants.LOG_OSDCLIENT_DELETE_HEADER, header);
		sendHeader(header);
	}

	public void copy(String srcPath, String srcObjId, String srcVersionId, String destPath, String destObjId, String destVersionId, String replication, String replicaDiskID) throws IOException {
		String header = OSDConstants.COPY 
						+ GWConstants.COLON + srcPath 
						+ GWConstants.COLON + srcObjId 
						+ GWConstants.COLON + srcVersionId 
						+ GWConstants.COLON + destPath 
						+ GWConstants.COLON + destObjId 
						+ GWConstants.COLON + destVersionId
						+ GWConstants.COLON + replication
						+ GWConstants.COLON + replicaDiskID;
		logger.debug(GWConstants.LOG_OSDCLIENT_COPY_HEADER, header);
		sendHeader(header);
	}

	public void partInit(String path, String objId, String partNo, long length) throws IOException {
		String header = OSDConstants.PART + GWConstants.COLON + path + GWConstants.COLON + objId + GWConstants.COLON + partNo + GWConstants.COLON + String.valueOf(length);
		logger.debug(GWConstants.LOG_OSDCLIENT_PART_HEADER, header);
		sendHeader(header);
	}

	public void part(byte[] buffer, int offset, int length) throws IOException {
		socket.getOutputStream().write(buffer, offset, length);
	}

	public void deletePart(String path, String objId, String partNo) throws IOException {
		String header = OSDConstants.DELETE_PART + GWConstants.COLON + path + GWConstants.COLON + objId + GWConstants.COLON + partNo;
		logger.debug(GWConstants.LOG_OSDCLIENT_DELETE_PART_HEADER, header);
		sendHeader(header);
	}

	public OSDData partCopy(String srcPath, String srcObjId, String srcVersionId, String copySourceRange, String destPath, String destObjId, String partNo) throws IOException {
		String header = OSDConstants.PART_COPY + GWConstants.COLON + srcPath + GWConstants.COLON + srcObjId + GWConstants.COLON + srcVersionId + GWConstants.COLON + destPath + GWConstants.COLON + destObjId + GWConstants.COLON + partNo + GWConstants.COLON + copySourceRange;
		logger.debug(GWConstants.LOG_OSDCLIENT_PART_COPY_HEADER, header);
		sendHeader(header);

		return receiveData();
	}

	public OSDData completeMultipart(String path, String objId, String versionId, String partNos) throws IOException {
		String header = OSDConstants.COMPLETE_MULTIPART + GWConstants.COLON + path + GWConstants.COLON + objId + GWConstants.COLON + versionId + GWConstants.COLON + partNos;
		logger.debug(GWConstants.LOG_OSDCLIENT_COMPLETE_MULTIPART_HEADER, header);
		sendHeader(header);

		return receiveData();
	}

	public void abortMultipart(String path, String objId, String partNos) throws IOException {
		String header = OSDConstants.ABORT_MULTIPART + GWConstants.COLON + path + GWConstants.COLON + objId + GWConstants.COLON + partNos;
		logger.debug(GWConstants.LOG_OSDCLIENT_ABORT_MULTIPART_HEADER, header);
		sendHeader(header);
	}

	private void sendHeader(String header) throws IOException {
		byte[] buffer = header.getBytes(GWConstants.CHARSET_UTF_8);
		int size = buffer.length;
		
		DataOutputStream so = new DataOutputStream(socket.getOutputStream());
		
		so.writeInt(size);
		so.write(buffer, 0, size);
        so.flush();
	}

	private OSDData receiveData() throws IOException {
		DataInputStream si = new DataInputStream(socket.getInputStream());
		int size = si.readInt();
		byte[] buffer = new byte[size];
		si.read(buffer, 0, size);
		String result = new String(buffer, 0, size);
		String[] ArrayResult = result.split(GWConstants.COLON, -1);

		OSDData data = new OSDData();
		switch (ArrayResult[0]) {
		case OSDConstants.FILE:
			data.setETag(ArrayResult[1]);
			data.setFileSize(Long.parseLong(ArrayResult[2]));
			return data;
		default:
			logger.error(GWConstants.LOG_OSDCLIENT_UNKNOWN_RESULT, ArrayResult[1]);
		}

		return null;
	}

	public String getHost() {
		return host;
	}

	public boolean isValid() {
		if (this.socket != null) {
			return this.socket.isClosed();
		}
		return false;
	}

	public void activate() {
		logger.debug(GWConstants.LOG_OSDCLIENT_ACTIVATE_SOCKET);
	}

	public void desactivate() {
		logger.debug(GWConstants.LOG_OSDCLIENT_DESACTIVATE_SOCKET);
	}
}
