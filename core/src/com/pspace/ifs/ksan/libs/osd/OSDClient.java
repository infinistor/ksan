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
package com.pspace.ifs.ksan.libs.osd;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import com.pspace.ifs.ksan.libs.Constants;
import com.pspace.ifs.ksan.libs.data.OsdData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OSDClient {
	private boolean isUsed;
	private String host;
	private Socket socket;
	private Logger logger;
	private OutputStream byPassOut;
	private long fileSize;
	private MessageDigest md5er;

	public OSDClient(String ipAddress, int port) throws UnknownHostException, IOException {
		isUsed = false;
		logger = LoggerFactory.getLogger(OSDClient.class);
		host = ipAddress;
		socket = new Socket(ipAddress, port);
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        logger.debug(Constants.LOG_OSDCLIENT_SOCKET_INFO, socket.toString());
	}

	public boolean isUsed() {
		return isUsed;
	}

	public void setUsed(boolean isUsed) {
		this.isUsed = isUsed;
	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}
	
	public void disconnect() throws IOException {
		socket.close();
	}

	public void close() {
		logger.debug(Constants.LOG_OSDCLIENT_CLOSE_SOCKET_INFO, socket.toString());
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				logger.error(Constants.LOG_OSDCLIENT_SOCKET_ERROR, e.getMessage());
			} finally {
				socket = null;
			}
		}
	}

	public void getInit(String path, String objId, String versionId, long fileSize, String sourceRange, OutputStream out, String key) throws IOException {
		String header = OsdData.GET 
						+ OsdData.DELIMITER + path 
						+ OsdData.DELIMITER + objId 
						+ OsdData.DELIMITER + versionId 
						+ OsdData.DELIMITER + sourceRange
						+ OsdData.DELIMITER + key;
		logger.debug(Constants.LOG_OSDCLIENT_GET_HEADER, header);
		sendHeader(header);
		this.fileSize = fileSize;
		byPassOut = out;
	}

	public void getInitWithMD5(String path, String objId, String versionId, long fileSize, String sourceRange, OutputStream out, MessageDigest md5er, String key) throws IOException {
		String header = OsdData.GET 
						+ OsdData.DELIMITER + path 
						+ OsdData.DELIMITER + objId 
						+ OsdData.DELIMITER + versionId 
						+ OsdData.DELIMITER + sourceRange
						+ OsdData.DELIMITER + key;
		logger.debug(Constants.LOG_OSDCLIENT_GET_HEADER, header);
		sendHeader(header);
		this.fileSize = fileSize;
		byPassOut = out;
		this.md5er = md5er;
	}

	public long get() throws IOException {
		byte[] buffer = new byte[Constants.MAXBUFSIZE];
		int readByte = Constants.MAXBUFSIZE;
		int readLength = 0;
		long readTotal = 0L;
		
		while ((readLength = socket.getInputStream().read(buffer, 0, readByte)) != -1) {
			readTotal += readLength;
			byPassOut.write(buffer, 0, readLength);
			if (readTotal >= fileSize) {
				break;
			}
		}
		byPassOut.flush();
		// byPassOut.close();
		logger.info(Constants.LOG_OSDCLIENT_READ, readTotal);
		return readTotal;
	}

	public int get(byte[] b, int off, int len) throws IOException {
		return socket.getInputStream().read(b, off, len);
	}

	public long getWithMD5() throws IOException {
		byte[] buffer = new byte[Constants.MAXBUFSIZE];
		int readByte = Constants.MAXBUFSIZE;
		int readLength = 0;
		long readTotal = 0L;
		
		while ((readLength = socket.getInputStream().read(buffer, 0, readByte)) != -1) {
			readTotal += readLength;
			byPassOut.write(buffer, 0, readLength);
			md5er.update(buffer, 0, readLength);
			if (readTotal >= fileSize) {
				break;
			}
		}
		byPassOut.flush();
		// byPassOut.close();
		logger.info(Constants.LOG_OSDCLIENT_READ, readTotal);
		return readTotal;
	}

	// public void getPartInit(String path, String objId, String partNo, long fileSize, String range, OutputStream out) throws IOException {
	// 	String header = OsdData.GET_PART 
	// 					+ OsdData.DELIMITER + path 
	// 					+ OsdData.DELIMITER + objId 
	// 					+ OsdData.DELIMITER + partNo
	// 					+ OsdData.DELIMITER + range;
	// 	logger.debug(Constants.LOG_OSDCLIENT_GET_HEADER, header);
	// 	sendHeader(header);
	// 	this.fileSize = fileSize;
	// 	byPassOut = out;
	// 	// this.md5er = md5er;
	// }

	public void getPartInit(String path, long size, String range, OutputStream out) throws IOException {
		String header = OsdData.GET_PART 
						+ OsdData.DELIMITER + path 
						+ OsdData.DELIMITER + range;
		logger.debug(Constants.LOG_OSDCLIENT_GET_HEADER, header);
		sendHeader(header);
		this.fileSize = size;
		byPassOut = out;
	}

	public long getPart() throws IOException {
		byte[] buffer = new byte[Constants.MAXBUFSIZE];
		int readByte = Constants.MAXBUFSIZE;
		int readLength = 0;
		long readTotal = 0L;
		
		while ((readLength = socket.getInputStream().read(buffer, 0, readByte)) != -1) {
			readTotal += readLength;
			byPassOut.write(buffer, 0, readLength);
			if (readTotal >= fileSize) {
				break;
			}
		}
		byPassOut.flush();
		logger.debug(Constants.LOG_OSDCLIENT_READ, readTotal);

		return readTotal;
	}

	public int getPart(byte[] b, int off, int len) throws IOException {
		return socket.getInputStream().read(b, off, len);
	}

	public void putInit(String path, String objId, String versionId, long length, String replication, String replicaDiskID, String key, String mode) throws IOException {
		String header = OsdData.PUT 
						+ OsdData.DELIMITER + path 
						+ OsdData.DELIMITER + objId 
						+ OsdData.DELIMITER + versionId 
						+ OsdData.DELIMITER + String.valueOf(length) 
						+ OsdData.DELIMITER + replication 
						+ OsdData.DELIMITER + replicaDiskID
						+ OsdData.DELIMITER + key
						+ OsdData.DELIMITER + mode;
		logger.debug(Constants.LOG_OSDCLIENT_PUT_HEADER, header);
		sendHeader(header);
	}

	public void put(byte[] buffer, int offset, int length) throws IOException {
		socket.getOutputStream().write(buffer, offset, length);
	}

	public void putFlush() throws IOException {
		socket.getOutputStream().flush();
	}

	public void putRangeInit(String path, String objId, String versionId, long offset, long length, String replication, String replicaDiskID, String key, String mode) throws IOException {
		String header = OsdData.PUT_RANGE 
						+ OsdData.DELIMITER + path 
						+ OsdData.DELIMITER + objId 
						+ OsdData.DELIMITER + versionId
						+ OsdData.DELIMITER + String.valueOf(offset)
						+ OsdData.DELIMITER + String.valueOf(length) 
						+ OsdData.DELIMITER + replication 
						+ OsdData.DELIMITER + replicaDiskID
						+ OsdData.DELIMITER + key
						+ OsdData.DELIMITER + mode;
		logger.debug(Constants.LOG_OSDCLIENT_PUT_HEADER, header);
		sendHeader(header);
	}

	public void delete(String path, String objId, String versionId) throws IOException {
		String header = OsdData.DELETE 
						+ OsdData.DELIMITER + path 
						+ OsdData.DELIMITER + objId 
						+ OsdData.DELIMITER + versionId;
		logger.debug(Constants.LOG_OSDCLIENT_DELETE_HEADER, header);
		sendHeader(header);
	}

	public void deleteReplica(String path) throws IOException {
		String header = OsdData.DELETE_REPLICA
						+ OsdData.DELIMITER + path;
		logger.debug(Constants.LOG_OSDCLIENT_DELETE_REPLICA_HEADER, header);
		sendHeader(header);
	}

	public void deleteECPart(String path) throws IOException {
		String header = OsdData.DELETE_EC_PART
						+ OsdData.DELIMITER + path;
		logger.debug(Constants.LOG_OSDCLIENT_DELETE_EC_PART_HEADER, header);
		sendHeader(header);
	}

	public void copy(String srcPath, String srcObjId, String srcVersionId, String destPath, String destObjId, String destVersionId, String replication, String replicaDiskID) throws IOException {
		String header = OsdData.COPY 
						+ OsdData.DELIMITER + srcPath 
						+ OsdData.DELIMITER + srcObjId 
						+ OsdData.DELIMITER + srcVersionId 
						+ OsdData.DELIMITER + destPath 
						+ OsdData.DELIMITER + destObjId 
						+ OsdData.DELIMITER + destVersionId
						+ OsdData.DELIMITER + replication
						+ OsdData.DELIMITER + replicaDiskID;
		logger.debug(Constants.LOG_OSDCLIENT_COPY_HEADER, header);
		sendHeader(header);
	}

	public void partInit(String path, String objId, String uploadId, String partNo, long length, String key) throws IOException {
		String header = OsdData.PART 
						+ OsdData.DELIMITER + path 
						+ OsdData.DELIMITER + objId 
						+ OsdData.DELIMITER + uploadId 
						+ OsdData.DELIMITER + partNo 
						+ OsdData.DELIMITER + String.valueOf(length)
						+ OsdData.DELIMITER + key;
		logger.debug(Constants.LOG_OSDCLIENT_PART_HEADER, header);
		sendHeader(header);
	}

	public void part(byte[] buffer, int offset, int length) throws IOException {
		socket.getOutputStream().write(buffer, offset, length);
	}

	public void deletePart(String path, String objId, String uploadId, String partNo) throws IOException {
		String header = OsdData.DELETE_PART 
						+ OsdData.DELIMITER + path 
						+ OsdData.DELIMITER + objId 
						+ OsdData.DELIMITER + uploadId 
						+ OsdData.DELIMITER + partNo;
		logger.debug(Constants.LOG_OSDCLIENT_DELETE_PART_HEADER, header);
		sendHeader(header);
	}

	public OsdData partCopy(String srcDiskId, String srcObjId, String srcVersionId, String srcLength, String copySourceRange, String destPath, String destObjId, String partNo) throws IOException {
		String header = OsdData.PART_COPY 
						+ OsdData.DELIMITER + srcDiskId 
						+ OsdData.DELIMITER + srcObjId 
						+ OsdData.DELIMITER + srcVersionId 
						+ OsdData.DELIMITER + destPath 
						+ OsdData.DELIMITER + destObjId 
						+ OsdData.DELIMITER + partNo 
						+ OsdData.DELIMITER + copySourceRange
						+ OsdData.DELIMITER + srcLength;
		logger.debug(Constants.LOG_OSDCLIENT_PART_COPY_HEADER, header);
		sendHeader(header);

		return receiveData();
	}

	public void getMultipartInit(String path, String objId, String versionId, long fileSize, String sourceRange, OutputStream out, String key) throws IOException {
		String header = OsdData.GET_MULTIPART 
						+ OsdData.DELIMITER + path 
						+ OsdData.DELIMITER + objId 
						+ OsdData.DELIMITER + versionId 
						+ OsdData.DELIMITER + sourceRange
						+ OsdData.DELIMITER + key;
		logger.debug(Constants.LOG_OSDCLIENT_GET_MULTIPART_HEADER, header);
		sendHeader(header);
		this.fileSize = fileSize;
		byPassOut = out;
	}

	public long getMultipart() throws IOException {
		byte[] buffer = new byte[Constants.MAXBUFSIZE];
		int readByte = Constants.MAXBUFSIZE;
		int readLength = 0;
		long readTotal = 0L;
		logger.debug("fileSize: {}", fileSize);
		while ((readLength = socket.getInputStream().read(buffer, 0, readByte)) != -1) {
			readTotal += readLength;
			byPassOut.write(buffer, 0, readLength);
			if (readTotal >= fileSize) {
				break;
			}
		}
		byPassOut.flush();
		logger.info(Constants.LOG_OSDCLIENT_READ, readTotal);
		return readTotal;
	}

	public OsdData completeMultipart(String path, String objId, String versionId, String key, String replication, String replicaDiskID, String partNos) throws IOException {
		String header = OsdData.COMPLETE_MULTIPART 
						+ OsdData.DELIMITER + path 
						+ OsdData.DELIMITER + objId 
						+ OsdData.DELIMITER + versionId 
						+ OsdData.DELIMITER + key
						+ OsdData.DELIMITER + replication 
						+ OsdData.DELIMITER + replicaDiskID
						+ OsdData.DELIMITER + partNos;
		logger.debug(Constants.LOG_OSDCLIENT_COMPLETE_MULTIPART_HEADER, header);
		sendHeader(header);

		return receiveData();
	}

	public void abortMultipart(String path, String objId, String partNos) throws IOException {
		String header = OsdData.ABORT_MULTIPART 
						+ OsdData.DELIMITER + path 
						+ OsdData.DELIMITER + objId 
						+ OsdData.DELIMITER + partNos;
		logger.debug(Constants.LOG_OSDCLIENT_ABORT_MULTIPART_HEADER, header);
		sendHeader(header);
	}

	public void getECPartInit(String path, OutputStream out) throws IOException {
		String header = OsdData.GET_EC_PART
						+ OsdData.DELIMITER + path;
		logger.debug(Constants.LOG_OSDCLIENT_GET_EC_PART_HEADER, header);
		sendHeader(header);
		byPassOut = out;
	}

	public long getECPart() throws IOException {
		byte[] buffer = new byte[Constants.MAXBUFSIZE];
		int readByte = Constants.MAXBUFSIZE;
		int readLength = 0;
		long readTotal = 0L;
		
		while ((readLength = socket.getInputStream().read(buffer, 0, readByte)) != -1) {
			readTotal += readLength;
			byPassOut.write(buffer, 0, readLength);
		}
		byPassOut.flush();
		// byPassOut.close();
		logger.info(Constants.LOG_OSDCLIENT_READ, readTotal);
		return readTotal;
	} 

	public void putECPartInit(String path, long length) throws IOException {
		String header = OsdData.PUT_EC_PART
						+ OsdData.DELIMITER + path
						+ OsdData.DELIMITER + String.valueOf(length);
		logger.debug(Constants.LOG_OSDCLIENT_PUT_EC_PART_HEADER, header);
		sendHeader(header);
	}

	public void putECPart(byte[] buffer, int offset, int length) throws IOException {
		socket.getOutputStream().write(buffer, offset, length);
	}

	public void putECPartFlush() throws IOException {
		socket.getOutputStream().flush();
	}

	private void sendHeader(String header) throws IOException {
		byte[] buffer = header.getBytes(StandardCharsets.UTF_8);
        String strLength = Integer.toString(buffer.length);
        byte[] lengthBuffer = strLength.getBytes(StandardCharsets.UTF_8);

		byte length = (byte)lengthBuffer.length;
		logger.debug("socket : {} send header size : {}", socket.getRemoteSocketAddress().toString(), buffer.length);
		socket.getOutputStream().write(length);
        socket.getOutputStream().write(lengthBuffer, 0, length);
		
		socket.getOutputStream().write(buffer, 0, buffer.length);
		socket.getOutputStream().flush();
		logger.info("send header size : {}", buffer.length);
	}

	private OsdData receiveData() throws IOException {
		int length = socket.getInputStream().read();
		if (length == -1) {
			logger.info("socket {} EOF ...", socket.getRemoteSocketAddress().toString());
			return null;
		}
		byte[] lengthBuffer = new byte[length];
		socket.getInputStream().read(lengthBuffer, 0, length);
		String strLength = new String(lengthBuffer, StandardCharsets.UTF_8);

		length = Integer.parseInt(strLength);
		byte[] buffer = new byte[length];
		socket.getInputStream().read(buffer, 0, length);
		String result = new String(buffer, 0, length, StandardCharsets.UTF_8);
		String[] ArrayResult = result.split(Constants.COLON, -1);

		OsdData data = new OsdData();
		switch (ArrayResult[0]) {
		case OsdData.FILE:
			data.setETag(ArrayResult[1]);
			data.setFileSize(Long.parseLong(ArrayResult[2]));
			return data;
		default:
			logger.error(Constants.LOG_OSDCLIENT_UNKNOWN_RESULT, ArrayResult[1]);
		}

		return null;

		// DataInputStream si = new DataInputStream(socket.getInputStream());
		// int size = si.readInt();
		// byte[] buffer = new byte[size];
		// si.read(buffer, 0, size);
		// String result = new String(buffer, 0, size);
		// String[] ArrayResult = result.split(Constants.COLON, -1);

		// OsdData data = new OsdData();
		// switch (ArrayResult[0]) {
		// case OsdData.FILE:
		// 	data.setETag(ArrayResult[1]);
		// 	data.setFileSize(Long.parseLong(ArrayResult[2]));
		// 	return data;
		// default:
		// 	logger.error(Constants.LOG_OSDCLIENT_UNKNOWN_RESULT, ArrayResult[1]);
		// }

		// return null;
	}

	public String getHost() {
		return host;
	}

	public boolean isValid() {
		if (this.socket != null) {
			return !this.socket.isClosed() && socket.isConnected();
		}
		return false;
	}

	public void activate() {
		logger.debug(Constants.LOG_OSDCLIENT_ACTIVATE_SOCKET);
	}

	public void desactivate() {
		logger.debug(Constants.LOG_OSDCLIENT_DESACTIVATE_SOCKET);
	}

	public void getECInit(String path, String objId, String versionId, long fileSize, String sourceRange, OutputStream out, String key) throws IOException {
		String header = OsdData.GET_EC_PART 
						+ OsdData.DELIMITER + path 
						+ OsdData.DELIMITER + objId 
						+ OsdData.DELIMITER + versionId 
						+ OsdData.DELIMITER + sourceRange
						+ OsdData.DELIMITER + key;
		logger.debug(Constants.LOG_OSDCLIENT_GET_HEADER, header);
		sendHeader(header);
		this.fileSize = fileSize;
		byPassOut = out;
	}

	public void putECInit(String path, String objId, String versionId, long length, String replication, String replicaDiskID, String key, String mode) throws IOException {
		String header = OsdData.PUT_EC_PART
						+ OsdData.DELIMITER + path 
						+ OsdData.DELIMITER + objId 
						+ OsdData.DELIMITER + versionId 
						+ OsdData.DELIMITER + String.valueOf(length) 
						+ OsdData.DELIMITER + replication 
						+ OsdData.DELIMITER + replicaDiskID
						+ OsdData.DELIMITER + key
						+ OsdData.DELIMITER + mode;
		logger.debug(Constants.LOG_OSDCLIENT_PUT_HEADER, header);
		sendHeader(header);
	}
}

