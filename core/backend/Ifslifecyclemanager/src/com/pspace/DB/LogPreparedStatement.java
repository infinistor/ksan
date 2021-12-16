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
package com.pspace.DB;

import java.sql.Connection;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.sql.ParameterMetaData;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.RowId;

public class LogPreparedStatement implements java.sql.PreparedStatement {

  private List<String> parameterValues;
  private String sqlTemplate;
  private PreparedStatement wrappedStatement;

  public LogPreparedStatement(Connection connection, String sql) throws SQLException {
    wrappedStatement = connection.prepareStatement(sql);
    sqlTemplate = sql;
    parameterValues = new ArrayList<String>();
  }

  public LogPreparedStatement(Connection connection, StringBuffer sql) throws SQLException {
    this(connection, sql.toString());
  }

  public void addBatch() throws java.sql.SQLException {
    wrappedStatement.addBatch();
  }

  public void addBatch(String sql) throws java.sql.SQLException {
    wrappedStatement.addBatch(sql);
  }

  public void cancel() throws SQLException {
    wrappedStatement.cancel();
  }

  public void clearBatch() throws java.sql.SQLException {
    wrappedStatement.clearBatch();
  }

  public void clearParameters() throws java.sql.SQLException {
    wrappedStatement.clearParameters();
  }

  public void clearWarnings() throws java.sql.SQLException {
    wrappedStatement.clearWarnings();
  }

  public void close() throws java.sql.SQLException {
    wrappedStatement.close();
  }

  public boolean execute() throws java.sql.SQLException {
    return wrappedStatement.execute();
  }

  public boolean execute(String sql) throws java.sql.SQLException {
    return wrappedStatement.execute(sql);
  }

  public boolean execute(String sql, String[] strArray) throws java.sql.SQLException {
    return wrappedStatement.execute(sql, strArray);
  }

  public boolean execute(String sql, int[] intArray) throws java.sql.SQLException {
    return wrappedStatement.execute(sql, intArray);
  }

  public boolean execute(String sql, int intValue) throws java.sql.SQLException {
    return wrappedStatement.execute(sql, intValue);
  }

  public int[] executeBatch() throws java.sql.SQLException {
    return wrappedStatement.executeBatch();
  }

  public java.sql.ResultSet executeQuery() throws java.sql.SQLException {
    return wrappedStatement.executeQuery();
  }

  public java.sql.ResultSet executeQuery(String sql) throws java.sql.SQLException {
    return wrappedStatement.executeQuery(sql);
  }

  public int executeUpdate() throws java.sql.SQLException {
    return wrappedStatement.executeUpdate();
  }

  public int executeUpdate(String sql) throws java.sql.SQLException {
    return wrappedStatement.executeUpdate(sql);
  }

  public int executeUpdate(String sql, String[] strList) throws java.sql.SQLException {
    return wrappedStatement.executeUpdate(sql, strList);
  }

  public int executeUpdate(String sql, int[] intList) throws java.sql.SQLException {
    return wrappedStatement.executeUpdate(sql, intList);
  }

  public int executeUpdate(String sql, int intValue) throws java.sql.SQLException {
    return wrappedStatement.executeUpdate(sql, intValue);
  }

  public java.sql.Connection getConnection() throws java.sql.SQLException {
    return wrappedStatement.getConnection();
  }

  public int getFetchDirection() throws java.sql.SQLException {
    return wrappedStatement.getFetchDirection();
  }

  public int getFetchSize() throws java.sql.SQLException {
    return wrappedStatement.getFetchSize();
  }

  public int getMaxFieldSize() throws java.sql.SQLException {
    return wrappedStatement.getMaxFieldSize();
  }

  public ResultSet getGeneratedKeys() throws SQLException {
    return wrappedStatement.getGeneratedKeys();
  }

  public int getMaxRows() throws java.sql.SQLException {
    return wrappedStatement.getMaxRows();
  }

  public java.sql.ResultSetMetaData getMetaData() throws java.sql.SQLException {
    return wrappedStatement.getMetaData();
  }

  public boolean getMoreResults() throws java.sql.SQLException {
    return wrappedStatement.getMoreResults();
  }

  public boolean getMoreResults(int value) throws SQLException {
    return wrappedStatement.getMoreResults(value);
  }

  public int getQueryTimeout() throws java.sql.SQLException {
    return wrappedStatement.getQueryTimeout();
  }

  public java.sql.ResultSet getResultSet() throws java.sql.SQLException {
    return wrappedStatement.getResultSet();
  }

  public int getResultSetConcurrency() throws java.sql.SQLException {
    return wrappedStatement.getResultSetConcurrency();
  }

  public int getResultSetType() throws java.sql.SQLException {
    return wrappedStatement.getResultSetType();
  }

  public int getUpdateCount() throws java.sql.SQLException {
    return wrappedStatement.getUpdateCount();
  }

  public java.sql.SQLWarning getWarnings() throws java.sql.SQLException {
    return wrappedStatement.getWarnings();
  }

  public void setArray(int i, java.sql.Array x) throws java.sql.SQLException {
    wrappedStatement.setArray(i, x);
    saveQueryParamValue(i, x);
  }

  public void setAsciiStream(int parameterIndex, java.io.InputStream x, int length) throws java.sql.SQLException {
    wrappedStatement.setAsciiStream(parameterIndex, x, length);
    saveQueryParamValue(parameterIndex, x);
  }

  public void setBigDecimal(int parameterIndex, java.math.BigDecimal x) throws java.sql.SQLException {
    wrappedStatement.setBigDecimal(parameterIndex, x);
    saveQueryParamValue(parameterIndex, x);
  }

  public void setURL(int _int, URL uRL) throws SQLException {
    wrappedStatement.setURL(_int, uRL);
  }

  public void setBinaryStream(int parameterIndex, java.io.InputStream x, int length) throws java.sql.SQLException {
    wrappedStatement.setBinaryStream(parameterIndex, x, length);
    saveQueryParamValue(parameterIndex, x);
  }

  public void setBlob(int i, java.sql.Blob x) throws java.sql.SQLException {
    wrappedStatement.setBlob(i, x);
    saveQueryParamValue(i, x);
  }

  public void setBoolean(int parameterIndex, boolean x) throws java.sql.SQLException {
    wrappedStatement.setBoolean(parameterIndex, x);
    saveQueryParamValue(parameterIndex, x);
  }

  public void setByte(int parameterIndex, byte x) throws java.sql.SQLException {
    wrappedStatement.setByte(parameterIndex, x);
    saveQueryParamValue(parameterIndex, x);
  }

  public void setBytes(int parameterIndex, byte[] x) throws java.sql.SQLException {
    wrappedStatement.setBytes(parameterIndex, x);
    saveQueryParamValue(parameterIndex, x);
  }

  public void setCharacterStream(int parameterIndex, java.io.Reader reader, int length) throws java.sql.SQLException {
    wrappedStatement.setCharacterStream(parameterIndex, reader, length);
    saveQueryParamValue(parameterIndex, reader);
  }

  public void setClob(int i, java.sql.Clob x) throws java.sql.SQLException {
    wrappedStatement.setClob(i, x);
    saveQueryParamValue(i, x);
  }

  public void setCursorName(String name) throws java.sql.SQLException {
    wrappedStatement.setCursorName(name);
  }

  public void setDate(int parameterIndex, java.sql.Date x) throws java.sql.SQLException {
    wrappedStatement.setDate(parameterIndex, x);
    saveQueryParamValue(parameterIndex, x);
  }

  public void setDate(int parameterIndex, java.sql.Date x, java.util.Calendar cal) throws java.sql.SQLException {
    wrappedStatement.setDate(parameterIndex, x, cal);
    saveQueryParamValue(parameterIndex, x);
  }

  public void setDouble(int parameterIndex, double x) throws java.sql.SQLException {
    wrappedStatement.setDouble(parameterIndex, x);
    saveQueryParamValue(parameterIndex, x);
  }

  public void setEscapeProcessing(boolean enable) throws java.sql.SQLException {
    wrappedStatement.setEscapeProcessing(enable);
  }

  public void setFetchDirection(int direction) throws java.sql.SQLException {
    wrappedStatement.setFetchDirection(direction);
  }

  public void setFetchSize(int rows) throws java.sql.SQLException {
    wrappedStatement.setFetchSize(rows);
  }

  public void setFloat(int parameterIndex, float x) throws java.sql.SQLException {
    wrappedStatement.setFloat(parameterIndex, x);
    saveQueryParamValue(parameterIndex, x);

  }

  public void setInt(int parameterIndex, int x) throws java.sql.SQLException {
    wrappedStatement.setInt(parameterIndex, x);
    saveQueryParamValue(parameterIndex, x);
  }

  public void setLong(int parameterIndex, long x) throws java.sql.SQLException {
    wrappedStatement.setLong(parameterIndex, x);
    saveQueryParamValue(parameterIndex, x);
  }

  public void setMaxFieldSize(int max) throws java.sql.SQLException {
    wrappedStatement.setMaxFieldSize(max);
  }

  public void setMaxRows(int max) throws java.sql.SQLException {
    wrappedStatement.setMaxRows(max);
  }

  public void setNull(int parameterIndex, int sqlType) throws java.sql.SQLException {
    wrappedStatement.setNull(parameterIndex, sqlType);
    saveQueryParamValue(parameterIndex, null);
  }

  public void setNull(int paramIndex, int sqlType, String typeName) throws java.sql.SQLException {
    wrappedStatement.setNull(paramIndex, sqlType, typeName);
    saveQueryParamValue(paramIndex, null);
  }

  public void setObject(int parameterIndex, Object x) throws java.sql.SQLException {
    wrappedStatement.setObject(parameterIndex, x);
    saveQueryParamValue(parameterIndex, x);
  }

  public void setObject(int parameterIndex, Object x, int targetSqlType) throws java.sql.SQLException {
    wrappedStatement.setObject(parameterIndex, x, targetSqlType);
    saveQueryParamValue(parameterIndex, x);
  }

  public void setObject(int parameterIndex, Object x, int targetSqlType, int scale) throws java.sql.SQLException {
    wrappedStatement.setObject(parameterIndex, x, targetSqlType, scale);
    saveQueryParamValue(parameterIndex, x);
  }

  public void setQueryTimeout(int seconds) throws java.sql.SQLException {
    wrappedStatement.setQueryTimeout(seconds);
  }

  public void setRef(int i, java.sql.Ref x) throws java.sql.SQLException {
    wrappedStatement.setRef(i, x);
    saveQueryParamValue(i, x);
  }

  public void setShort(int parameterIndex, short x) throws java.sql.SQLException {
    wrappedStatement.setShort(parameterIndex, x);
    saveQueryParamValue(parameterIndex, x);
  }

  public void setString(int parameterIndex, String x) throws java.sql.SQLException {
    wrappedStatement.setString(parameterIndex, x);
    saveQueryParamValue(parameterIndex, x);
  }

  public void setTime(int parameterIndex, java.sql.Time x) throws java.sql.SQLException {
    wrappedStatement.setTime(parameterIndex, x);
    saveQueryParamValue(parameterIndex, x);
  }

  public void setTime(int parameterIndex, java.sql.Time x, java.util.Calendar cal) throws java.sql.SQLException {
    wrappedStatement.setTime(parameterIndex, x, cal);
    saveQueryParamValue(parameterIndex, x);
  }

  public void setTimestamp(int parameterIndex, java.sql.Timestamp x) throws java.sql.SQLException {
    wrappedStatement.setTimestamp(parameterIndex, x);
    saveQueryParamValue(parameterIndex, x);
  }

  public void setTimestamp(int parameterIndex, java.sql.Timestamp x, java.util.Calendar cal)
      throws java.sql.SQLException {
    wrappedStatement.setTimestamp(parameterIndex, x, cal);
    saveQueryParamValue(parameterIndex, x);
  }

  public ParameterMetaData getParameterMetaData() throws SQLException {
    return wrappedStatement.getParameterMetaData();
  }

  public int getResultSetHoldability() throws SQLException {
    return wrappedStatement.getResultSetHoldability();
  }

  public String getQueryString() {
    StringBuffer buf = new StringBuffer();
    int qMarkCount = 0;
    StringTokenizer tok = new StringTokenizer(sqlTemplate + " ", "?");
    while (tok.hasMoreTokens()) {
      String oneChunk = tok.nextToken();
      buf.append(oneChunk);

      try {
        Object value;
        if (parameterValues.size() > 1 + qMarkCount) {
          value = parameterValues.get(1 + qMarkCount++);
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

  private void saveQueryParamValue(int position, Object obj) {
    String strValue;
    if (obj instanceof String || obj instanceof Date) {
      strValue = "'" + obj + "'";
    } else {
      if (obj == null) {
        strValue = "null";
      } else {
        strValue = obj.toString();
      }
    }

    while (position >= parameterValues.size()) {
      parameterValues.add(null);
    }

    parameterValues.set(position, strValue);
  }

  public String toString() {
    return this.getQueryString();
  }

  @Override
  public boolean isClosed() throws SQLException {
    return wrappedStatement.isClosed();
  }

  @Override
  public void setPoolable(boolean poolable) throws SQLException {
    wrappedStatement.setPoolable(poolable);
  }

  @Override
  public boolean isPoolable() throws SQLException {
    return wrappedStatement.isPoolable();
  }

  @Override
  public void closeOnCompletion() throws SQLException {
    wrappedStatement.closeOnCompletion();
  }

  @Override
  public boolean isCloseOnCompletion() throws SQLException {
    return wrappedStatement.isCloseOnCompletion();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    return wrappedStatement.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return wrappedStatement.isWrapperFor(iface);
  }

  @Override
  public void setRowId(int parameterIndex, RowId x) throws SQLException {
    wrappedStatement.setRowId(parameterIndex, x);

  }

  @Override
  public void setNString(int parameterIndex, String value) throws SQLException {
    wrappedStatement.setNString(parameterIndex, value);

  }

  @Override
  public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
    wrappedStatement.setNCharacterStream(parameterIndex, value);

  }

  @Override
  public void setNClob(int parameterIndex, NClob value) throws SQLException {
    wrappedStatement.setNClob(parameterIndex, value);

  }

  @Override
  public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
    wrappedStatement.setClob(parameterIndex, reader, length);

  }

  @Override
  public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
    wrappedStatement.setBlob(parameterIndex, inputStream, length);

  }

  @Override
  public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
    wrappedStatement.setNClob(parameterIndex, reader, length);

  }

  @Override
  public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
    wrappedStatement.setSQLXML(parameterIndex, xmlObject);

  }

  @Override
  public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
    wrappedStatement.setAsciiStream(parameterIndex, x, length);

  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
    wrappedStatement.setBinaryStream(parameterIndex, x, length);

  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
    wrappedStatement.setCharacterStream(parameterIndex, reader, length);

  }

  @Override
  public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
    wrappedStatement.setAsciiStream(parameterIndex, x);

  }

  @Override
  public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
    wrappedStatement.setBinaryStream(parameterIndex, x);

  }

  @Override
  public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
    wrappedStatement.setCharacterStream(parameterIndex, reader);

  }

  @Override
  public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
    wrappedStatement.setNCharacterStream(parameterIndex, value);
  }

  @Override
  public void setClob(int parameterIndex, Reader reader) throws SQLException {
    wrappedStatement.setClob(parameterIndex, reader);

  }

  @Override
  public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
    wrappedStatement.setBlob(parameterIndex, inputStream);

  }

  @Override
  public void setNClob(int parameterIndex, Reader reader) throws SQLException {
    wrappedStatement.setNClob(parameterIndex, reader);

  }

  @Override
  @Deprecated(since = "1.2")
  public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
    wrappedStatement.setUnicodeStream(parameterIndex, x, length);
  }
}
