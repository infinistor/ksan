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
package com.pspace.backend.libs.db;

import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.sql.ParameterMetaData;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;

public class LogPreparedStatement implements java.sql.PreparedStatement {

	private List<String> parameterValues;
	private String sqlTemplate;
	private PreparedStatement logStatement;

	public LogPreparedStatement(Connection connection, String sql) throws SQLException {
		logStatement = connection.prepareStatement(sql);
		sqlTemplate = sql;
		parameterValues = new ArrayList<>();
	}

	public LogPreparedStatement(Connection connection, StringBuilder sql) throws SQLException {
		this(connection, sql.toString());
	}

	public void addBatch() throws SQLException {
		logStatement.addBatch();
	}

	public void addBatch(String sql) throws SQLException {
		logStatement.addBatch(sql);
	}

	public void cancel() throws SQLException {
		logStatement.cancel();
	}

	public void clearBatch() throws SQLException {
		logStatement.clearBatch();
	}

	public void clearParameters() throws SQLException {
		logStatement.clearParameters();
	}

	public void clearWarnings() throws SQLException {
		logStatement.clearWarnings();
	}

	public void close() throws SQLException {
		logStatement.close();
	}

	public boolean execute() throws SQLException {
		return logStatement.execute();
	}

	public boolean execute(String sql) throws SQLException {
		return logStatement.execute(sql);
	}

	public boolean execute(String sql, String[] strArray) throws SQLException {
		return logStatement.execute(sql, strArray);
	}

	public boolean execute(String sql, int[] intArray) throws SQLException {
		return logStatement.execute(sql, intArray);
	}

	public boolean execute(String sql, int intValue) throws SQLException {
		return logStatement.execute(sql, intValue);
	}

	public int[] executeBatch() throws SQLException {
		return logStatement.executeBatch();
	}

	public ResultSet executeQuery() throws SQLException {
		return logStatement.executeQuery();
	}

	public ResultSet executeQuery(String sql) throws SQLException {
		return logStatement.executeQuery(sql);
	}

	public int executeUpdate() throws SQLException {
		return logStatement.executeUpdate();
	}

	public int executeUpdate(String sql) throws SQLException {
		return logStatement.executeUpdate(sql);
	}

	public int executeUpdate(String sql, String[] strList) throws SQLException {
		return logStatement.executeUpdate(sql, strList);
	}

	public int executeUpdate(String sql, int[] intList) throws SQLException {
		return logStatement.executeUpdate(sql, intList);
	}

	public int executeUpdate(String sql, int intValue) throws SQLException {
		return logStatement.executeUpdate(sql, intValue);
	}

	public Connection getConnection() throws SQLException {
		return logStatement.getConnection();
	}

	public int getFetchDirection() throws SQLException {
		return logStatement.getFetchDirection();
	}

	public int getFetchSize() throws SQLException {
		return logStatement.getFetchSize();
	}

	public int getMaxFieldSize() throws SQLException {
		return logStatement.getMaxFieldSize();
	}

	public ResultSet getGeneratedKeys() throws SQLException {
		return logStatement.getGeneratedKeys();
	}

	public int getMaxRows() throws SQLException {
		return logStatement.getMaxRows();
	}

	public ResultSetMetaData getMetaData() throws SQLException {
		return logStatement.getMetaData();
	}

	public boolean getMoreResults() throws SQLException {
		return logStatement.getMoreResults();
	}

	public boolean getMoreResults(int value) throws SQLException {
		return logStatement.getMoreResults(value);
	}

	public int getQueryTimeout() throws SQLException {
		return logStatement.getQueryTimeout();
	}

	public ResultSet getResultSet() throws SQLException {
		return logStatement.getResultSet();
	}

	public int getResultSetConcurrency() throws SQLException {
		return logStatement.getResultSetConcurrency();
	}

	public int getResultSetType() throws SQLException {
		return logStatement.getResultSetType();
	}

	public int getUpdateCount() throws SQLException {
		return logStatement.getUpdateCount();
	}

	public SQLWarning getWarnings() throws SQLException {
		return logStatement.getWarnings();
	}

	public void setArray(int i, Array x) throws SQLException {
		logStatement.setArray(i, x);
		saveQueryParamValue(i, x);
	}

	public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
		logStatement.setAsciiStream(parameterIndex, x, length);
		saveQueryParamValue(parameterIndex, x);
	}

	public void setBigDecimal(int parameterIndex, java.math.BigDecimal x) throws SQLException {
		logStatement.setBigDecimal(parameterIndex, x);
		saveQueryParamValue(parameterIndex, x);
	}

	public void setURL(int port, URL url) throws SQLException {
		logStatement.setURL(port, url);
	}

	public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
		logStatement.setBinaryStream(parameterIndex, x, length);
		saveQueryParamValue(parameterIndex, x);
	}

	public void setBlob(int i, Blob x) throws SQLException {
		logStatement.setBlob(i, x);
		saveQueryParamValue(i, x);
	}

	public void setBoolean(int parameterIndex, boolean x) throws SQLException {
		logStatement.setBoolean(parameterIndex, x);
		saveQueryParamValue(parameterIndex, x);
	}

	public void setByte(int parameterIndex, byte x) throws SQLException {
		logStatement.setByte(parameterIndex, x);
		saveQueryParamValue(parameterIndex, x);
	}

	public void setBytes(int parameterIndex, byte[] x) throws SQLException {
		logStatement.setBytes(parameterIndex, x);
		saveQueryParamValue(parameterIndex, x);
	}

	public void setCharacterStream(int parameterIndex, java.io.Reader reader, int length) throws SQLException {
		logStatement.setCharacterStream(parameterIndex, reader, length);
		saveQueryParamValue(parameterIndex, reader);
	}

	public void setClob(int i, Clob x) throws SQLException {
		logStatement.setClob(i, x);
		saveQueryParamValue(i, x);
	}

	public void setCursorName(String name) throws SQLException {
		logStatement.setCursorName(name);
	}

	public void setDate(int parameterIndex, java.sql.Date x) throws SQLException {
		logStatement.setDate(parameterIndex, x);
		saveQueryParamValue(parameterIndex, x);
	}

	public void setDate(int parameterIndex, java.sql.Date x, java.util.Calendar cal) throws SQLException {
		logStatement.setDate(parameterIndex, x, cal);
		saveQueryParamValue(parameterIndex, x);
	}

	public void setDouble(int parameterIndex, double x) throws SQLException {
		logStatement.setDouble(parameterIndex, x);
		saveQueryParamValue(parameterIndex, x);
	}

	public void setEscapeProcessing(boolean enable) throws SQLException {
		logStatement.setEscapeProcessing(enable);
	}

	public void setFetchDirection(int direction) throws SQLException {
		logStatement.setFetchDirection(direction);
	}

	public void setFetchSize(int rows) throws SQLException {
		logStatement.setFetchSize(rows);
	}

	public void setFloat(int parameterIndex, float x) throws SQLException {
		logStatement.setFloat(parameterIndex, x);
		saveQueryParamValue(parameterIndex, x);

	}

	public void setInt(int parameterIndex, int x) throws SQLException {
		logStatement.setInt(parameterIndex, x);
		saveQueryParamValue(parameterIndex, x);
	}

	public void setLong(int parameterIndex, long x) throws SQLException {
		logStatement.setLong(parameterIndex, x);
		saveQueryParamValue(parameterIndex, x);
	}

	public void setMaxFieldSize(int max) throws SQLException {
		logStatement.setMaxFieldSize(max);
	}

	public void setMaxRows(int max) throws SQLException {
		logStatement.setMaxRows(max);
	}

	public void setNull(int parameterIndex, int sqlType) throws SQLException {
		logStatement.setNull(parameterIndex, sqlType);
		saveQueryParamValue(parameterIndex, null);
	}

	public void setNull(int paramIndex, int sqlType, String typeName) throws SQLException {
		logStatement.setNull(paramIndex, sqlType, typeName);
		saveQueryParamValue(paramIndex, null);
	}

	public void setObject(int parameterIndex, Object x) throws SQLException {
		logStatement.setObject(parameterIndex, x);
		saveQueryParamValue(parameterIndex, x);
	}

	public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
		logStatement.setObject(parameterIndex, x, targetSqlType);
		saveQueryParamValue(parameterIndex, x);
	}

	public void setObject(int parameterIndex, Object x, int targetSqlType, int scale) throws SQLException {
		logStatement.setObject(parameterIndex, x, targetSqlType, scale);
		saveQueryParamValue(parameterIndex, x);
	}

	public void setQueryTimeout(int seconds) throws SQLException {
		logStatement.setQueryTimeout(seconds);
	}

	public void setRef(int i, Ref x) throws SQLException {
		logStatement.setRef(i, x);
		saveQueryParamValue(i, x);
	}

	public void setShort(int parameterIndex, short x) throws SQLException {
		logStatement.setShort(parameterIndex, x);
		saveQueryParamValue(parameterIndex, x);
	}

	public void setString(int parameterIndex, String x) throws SQLException {
		logStatement.setString(parameterIndex, x);
		saveQueryParamValue(parameterIndex, x);
	}

	public void setTime(int parameterIndex, Time x) throws SQLException {
		logStatement.setTime(parameterIndex, x);
		saveQueryParamValue(parameterIndex, x);
	}

	public void setTime(int parameterIndex, Time x, java.util.Calendar cal) throws SQLException {
		logStatement.setTime(parameterIndex, x, cal);
		saveQueryParamValue(parameterIndex, x);
	}

	public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
		logStatement.setTimestamp(parameterIndex, x);
		saveQueryParamValue(parameterIndex, x);
	}

	public void setTimestamp(int parameterIndex, Timestamp x, java.util.Calendar cal)
			throws SQLException {
		logStatement.setTimestamp(parameterIndex, x, cal);
		saveQueryParamValue(parameterIndex, x);
	}

	public ParameterMetaData getParameterMetaData() throws SQLException {
		return logStatement.getParameterMetaData();
	}

	public int getResultSetHoldability() throws SQLException {
		return logStatement.getResultSetHoldability();
	}

	private void saveQueryParamValue(int position, Object obj) {
		String value;
		if (obj instanceof String || obj instanceof Date) {
			value = "'" + obj + "'";
		} else {
			if (obj == null) {
				value = "null";
			} else {
				value = obj.toString();
			}
		}

		while (position >= parameterValues.size()) {
			parameterValues.add(null);
		}

		parameterValues.set(position, value);
	}

	public String toString() {
		StringBuilder buf = new StringBuilder();
		int markCount = 0;
		StringTokenizer tok = new StringTokenizer(sqlTemplate + " ", "?");
		while (tok.hasMoreTokens()) {
			String oneChunk = tok.nextToken();
			buf.append(oneChunk);

			try {
				Object value;
				if (parameterValues.size() > 1 + markCount) {
					value = parameterValues.get(1 + markCount++);
				} else {
					if (tok.hasMoreTokens()) {
						value = null;
					} else {
						value = "";
					}
				}
				buf.append("" + value);
			} catch (Throwable e) {
				buf.append("Error : " + e.toString());
			}
		}
		return buf.toString().trim();
	}

	@Override
	public boolean isClosed() throws SQLException {
		return logStatement.isClosed();
	}

	@Override
	public void setPoolable(boolean poolable) throws SQLException {
		logStatement.setPoolable(poolable);
	}

	@Override
	public boolean isPoolable() throws SQLException {
		return logStatement.isPoolable();
	}

	@Override
	public void closeOnCompletion() throws SQLException {
		logStatement.closeOnCompletion();
	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException {
		return logStatement.isCloseOnCompletion();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return logStatement.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return logStatement.isWrapperFor(iface);
	}

	@Override
	public void setRowId(int parameterIndex, RowId x) throws SQLException {
		logStatement.setRowId(parameterIndex, x);

	}

	@Override
	public void setNString(int parameterIndex, String value) throws SQLException {
		logStatement.setNString(parameterIndex, value);

	}

	@Override
	public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
		logStatement.setNCharacterStream(parameterIndex, value);

	}

	@Override
	public void setNClob(int parameterIndex, NClob value) throws SQLException {
		logStatement.setNClob(parameterIndex, value);

	}

	@Override
	public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
		logStatement.setClob(parameterIndex, reader, length);

	}

	@Override
	public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
		logStatement.setBlob(parameterIndex, inputStream, length);

	}

	@Override
	public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
		logStatement.setNClob(parameterIndex, reader, length);

	}

	@Override
	public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
		logStatement.setSQLXML(parameterIndex, xmlObject);

	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
		logStatement.setAsciiStream(parameterIndex, x, length);

	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
		logStatement.setBinaryStream(parameterIndex, x, length);

	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
		logStatement.setCharacterStream(parameterIndex, reader, length);

	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
		logStatement.setAsciiStream(parameterIndex, x);

	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
		logStatement.setBinaryStream(parameterIndex, x);

	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
		logStatement.setCharacterStream(parameterIndex, reader);

	}

	@Override
	public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
		logStatement.setNCharacterStream(parameterIndex, value);
	}

	@Override
	public void setClob(int parameterIndex, Reader reader) throws SQLException {
		logStatement.setClob(parameterIndex, reader);

	}

	@Override
	public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
		logStatement.setBlob(parameterIndex, inputStream);

	}

	@Override
	public void setNClob(int parameterIndex, Reader reader) throws SQLException {
		logStatement.setNClob(parameterIndex, reader);

	}

	/**
	 * @deprecated Use {@code setNCharacterStream} which can handle all
	 */
	@Override
	@Deprecated(since = "1.2")
	public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
		logStatement.setUnicodeStream(parameterIndex, x, length);
	}

}
