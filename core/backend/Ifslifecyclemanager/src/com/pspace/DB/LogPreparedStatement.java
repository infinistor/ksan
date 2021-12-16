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

  private List<String> ParameterValues;
  private String SqlTemplate;
  private PreparedStatement LogStatement;

  public LogPreparedStatement(Connection connection, String sql) throws SQLException {
    LogStatement = connection.prepareStatement(sql);
    SqlTemplate = sql;
    ParameterValues = new ArrayList<String>();
  }

  public LogPreparedStatement(Connection connection, StringBuffer sql) throws SQLException {
    this(connection, sql.toString());
  }

  public void addBatch() throws SQLException {
    LogStatement.addBatch();
  }

  public void addBatch(String sql) throws SQLException {
    LogStatement.addBatch(sql);
  }

  public void cancel() throws SQLException {
    LogStatement.cancel();
  }

  public void clearBatch() throws SQLException {
    LogStatement.clearBatch();
  }

  public void clearParameters() throws SQLException {
    LogStatement.clearParameters();
  }

  public void clearWarnings() throws SQLException {
    LogStatement.clearWarnings();
  }

  public void close() throws SQLException {
    LogStatement.close();
  }

  public boolean execute() throws SQLException {
    return LogStatement.execute();
  }

  public boolean execute(String sql) throws SQLException {
    return LogStatement.execute(sql);
  }

  public boolean execute(String sql, String[] strArray) throws SQLException {
    return LogStatement.execute(sql, strArray);
  }

  public boolean execute(String sql, int[] intArray) throws SQLException {
    return LogStatement.execute(sql, intArray);
  }

  public boolean execute(String sql, int intValue) throws SQLException {
    return LogStatement.execute(sql, intValue);
  }

  public int[] executeBatch() throws SQLException {
    return LogStatement.executeBatch();
  }

  public ResultSet executeQuery() throws SQLException {
    return LogStatement.executeQuery();
  }

  public ResultSet executeQuery(String sql) throws SQLException {
    return LogStatement.executeQuery(sql);
  }

  public int executeUpdate() throws SQLException {
    return LogStatement.executeUpdate();
  }

  public int executeUpdate(String sql) throws SQLException {
    return LogStatement.executeUpdate(sql);
  }

  public int executeUpdate(String sql, String[] strList) throws SQLException {
    return LogStatement.executeUpdate(sql, strList);
  }

  public int executeUpdate(String sql, int[] intList) throws SQLException {
    return LogStatement.executeUpdate(sql, intList);
  }

  public int executeUpdate(String sql, int intValue) throws SQLException {
    return LogStatement.executeUpdate(sql, intValue);
  }

  public Connection getConnection() throws SQLException {
    return LogStatement.getConnection();
  }

  public int getFetchDirection() throws SQLException {
    return LogStatement.getFetchDirection();
  }

  public int getFetchSize() throws SQLException {
    return LogStatement.getFetchSize();
  }

  public int getMaxFieldSize() throws SQLException {
    return LogStatement.getMaxFieldSize();
  }

  public ResultSet getGeneratedKeys() throws SQLException {
    return LogStatement.getGeneratedKeys();
  }

  public int getMaxRows() throws SQLException {
    return LogStatement.getMaxRows();
  }

  public ResultSetMetaData getMetaData() throws SQLException {
    return LogStatement.getMetaData();
  }

  public boolean getMoreResults() throws SQLException {
    return LogStatement.getMoreResults();
  }

  public boolean getMoreResults(int Value) throws SQLException {
    return LogStatement.getMoreResults(Value);
  }

  public int getQueryTimeout() throws SQLException {
    return LogStatement.getQueryTimeout();
  }

  public ResultSet getResultSet() throws SQLException {
    return LogStatement.getResultSet();
  }

  public int getResultSetConcurrency() throws SQLException {
    return LogStatement.getResultSetConcurrency();
  }

  public int getResultSetType() throws SQLException {
    return LogStatement.getResultSetType();
  }

  public int getUpdateCount() throws SQLException {
    return LogStatement.getUpdateCount();
  }

  public SQLWarning getWarnings() throws SQLException {
    return LogStatement.getWarnings();
  }

  public void setArray(int i, Array x) throws SQLException {
    LogStatement.setArray(i, x);
    saveQueryParamValue(i, x);
  }

  public void setAsciiStream(int ParameterIndex, InputStream x, int Length) throws SQLException {
    LogStatement.setAsciiStream(ParameterIndex, x, Length);
    saveQueryParamValue(ParameterIndex, x);
  }

  public void setBigDecimal(int ParameterIndex, java.math.BigDecimal x) throws SQLException {
    LogStatement.setBigDecimal(ParameterIndex, x);
    saveQueryParamValue(ParameterIndex, x);
  }

  public void setURL(int _int, URL uRL) throws SQLException {
    LogStatement.setURL(_int, uRL);
  }

  public void setBinaryStream(int ParameterIndex, InputStream x, int Length) throws SQLException {
    LogStatement.setBinaryStream(ParameterIndex, x, Length);
    saveQueryParamValue(ParameterIndex, x);
  }

  public void setBlob(int i, Blob x) throws SQLException {
    LogStatement.setBlob(i, x);
    saveQueryParamValue(i, x);
  }

  public void setBoolean(int ParameterIndex, boolean x) throws SQLException {
    LogStatement.setBoolean(ParameterIndex, x);
    saveQueryParamValue(ParameterIndex, x);
  }

  public void setByte(int ParameterIndex, byte x) throws SQLException {
    LogStatement.setByte(ParameterIndex, x);
    saveQueryParamValue(ParameterIndex, x);
  }

  public void setBytes(int ParameterIndex, byte[] x) throws SQLException {
    LogStatement.setBytes(ParameterIndex, x);
    saveQueryParamValue(ParameterIndex, x);
  }

  public void setCharacterStream(int ParameterIndex, java.io.Reader reader, int Length) throws SQLException {
    LogStatement.setCharacterStream(ParameterIndex, reader, Length);
    saveQueryParamValue(ParameterIndex, reader);
  }

  public void setClob(int i, Clob x) throws SQLException {
    LogStatement.setClob(i, x);
    saveQueryParamValue(i, x);
  }

  public void setCursorName(String name) throws SQLException {
    LogStatement.setCursorName(name);
  }

  public void setDate(int ParameterIndex, java.sql.Date x) throws SQLException {
    LogStatement.setDate(ParameterIndex, x);
    saveQueryParamValue(ParameterIndex, x);
  }

  public void setDate(int ParameterIndex, java.sql.Date x, java.util.Calendar cal) throws SQLException {
    LogStatement.setDate(ParameterIndex, x, cal);
    saveQueryParamValue(ParameterIndex, x);
  }

  public void setDouble(int ParameterIndex, double x) throws SQLException {
    LogStatement.setDouble(ParameterIndex, x);
    saveQueryParamValue(ParameterIndex, x);
  }

  public void setEscapeProcessing(boolean enable) throws SQLException {
    LogStatement.setEscapeProcessing(enable);
  }

  public void setFetchDirection(int direction) throws SQLException {
    LogStatement.setFetchDirection(direction);
  }

  public void setFetchSize(int rows) throws SQLException {
    LogStatement.setFetchSize(rows);
  }

  public void setFloat(int ParameterIndex, float x) throws SQLException {
    LogStatement.setFloat(ParameterIndex, x);
    saveQueryParamValue(ParameterIndex, x);

  }

  public void setInt(int ParameterIndex, int x) throws SQLException {
    LogStatement.setInt(ParameterIndex, x);
    saveQueryParamValue(ParameterIndex, x);
  }

  public void setLong(int ParameterIndex, long x) throws SQLException {
    LogStatement.setLong(ParameterIndex, x);
    saveQueryParamValue(ParameterIndex, x);
  }

  public void setMaxFieldSize(int max) throws SQLException {
    LogStatement.setMaxFieldSize(max);
  }

  public void setMaxRows(int max) throws SQLException {
    LogStatement.setMaxRows(max);
  }

  public void setNull(int ParameterIndex, int sqlType) throws SQLException {
    LogStatement.setNull(ParameterIndex, sqlType);
    saveQueryParamValue(ParameterIndex, null);
  }

  public void setNull(int paramIndex, int sqlType, String typeName) throws SQLException {
    LogStatement.setNull(paramIndex, sqlType, typeName);
    saveQueryParamValue(paramIndex, null);
  }

  public void setObject(int ParameterIndex, Object x) throws SQLException {
    LogStatement.setObject(ParameterIndex, x);
    saveQueryParamValue(ParameterIndex, x);
  }

  public void setObject(int ParameterIndex, Object x, int targetSqlType) throws SQLException {
    LogStatement.setObject(ParameterIndex, x, targetSqlType);
    saveQueryParamValue(ParameterIndex, x);
  }

  public void setObject(int ParameterIndex, Object x, int targetSqlType, int scale) throws SQLException {
    LogStatement.setObject(ParameterIndex, x, targetSqlType, scale);
    saveQueryParamValue(ParameterIndex, x);
  }

  public void setQueryTimeout(int seconds) throws SQLException {
    LogStatement.setQueryTimeout(seconds);
  }

  public void setRef(int i, Ref x) throws SQLException {
    LogStatement.setRef(i, x);
    saveQueryParamValue(i, x);
  }

  public void setShort(int ParameterIndex, short x) throws SQLException {
    LogStatement.setShort(ParameterIndex, x);
    saveQueryParamValue(ParameterIndex, x);
  }

  public void setString(int ParameterIndex, String x) throws SQLException {
    LogStatement.setString(ParameterIndex, x);
    saveQueryParamValue(ParameterIndex, x);
  }

  public void setTime(int ParameterIndex, Time x) throws SQLException {
    LogStatement.setTime(ParameterIndex, x);
    saveQueryParamValue(ParameterIndex, x);
  }

  public void setTime(int ParameterIndex, Time x, java.util.Calendar cal) throws SQLException {
    LogStatement.setTime(ParameterIndex, x, cal);
    saveQueryParamValue(ParameterIndex, x);
  }

  public void setTimestamp(int ParameterIndex, Timestamp x) throws SQLException {
    LogStatement.setTimestamp(ParameterIndex, x);
    saveQueryParamValue(ParameterIndex, x);
  }

  public void setTimestamp(int ParameterIndex, Timestamp x, java.util.Calendar cal)
      throws SQLException {
    LogStatement.setTimestamp(ParameterIndex, x, cal);
    saveQueryParamValue(ParameterIndex, x);
  }

  public ParameterMetaData getParameterMetaData() throws SQLException {
    return LogStatement.getParameterMetaData();
  }

  public int getResultSetHoldability() throws SQLException {
    return LogStatement.getResultSetHoldability();
  }

  private void saveQueryParamValue(int position, Object obj) {
    String Value;
    if (obj instanceof String || obj instanceof Date) {
      Value = "'" + obj + "'";
    } else {
      if (obj == null) {
        Value = "null";
      } else {
        Value = obj.toString();
      }
    }

    while (position >= ParameterValues.size()) {
      ParameterValues.add(null);
    }

    ParameterValues.set(position, Value);
  }

  public String toString() {
    StringBuffer Buf = new StringBuffer();
    int MarkCount = 0;
    StringTokenizer tok = new StringTokenizer(SqlTemplate + " ", "?");
    while (tok.hasMoreTokens()) {
      String oneChunk = tok.nextToken();
      Buf.append(oneChunk);

      try {
        Object Value;
        if (ParameterValues.size() > 1 + MarkCount) {
          Value = ParameterValues.get(1 + MarkCount++);
        } else {
          if (tok.hasMoreTokens()) {
            Value = null;
          } else {
            Value = "";
          }
        }
        Buf.append("" + Value);
      } catch (Throwable e) {
        Buf.append("Error : " + e.toString());
      }
    }
    return Buf.toString().trim();
  }

  @Override
  public boolean isClosed() throws SQLException {
    return LogStatement.isClosed();
  }

  @Override
  public void setPoolable(boolean poolable) throws SQLException {
    LogStatement.setPoolable(poolable);
  }

  @Override
  public boolean isPoolable() throws SQLException {
    return LogStatement.isPoolable();
  }

  @Override
  public void closeOnCompletion() throws SQLException {
    LogStatement.closeOnCompletion();
  }

  @Override
  public boolean isCloseOnCompletion() throws SQLException {
    return LogStatement.isCloseOnCompletion();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    return LogStatement.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return LogStatement.isWrapperFor(iface);
  }

  @Override
  public void setRowId(int ParameterIndex, RowId x) throws SQLException {
    LogStatement.setRowId(ParameterIndex, x);

  }

  @Override
  public void setNString(int ParameterIndex, String Value) throws SQLException {
    LogStatement.setNString(ParameterIndex, Value);

  }

  @Override
  public void setNCharacterStream(int ParameterIndex, Reader Value, long Length) throws SQLException {
    LogStatement.setNCharacterStream(ParameterIndex, Value);

  }

  @Override
  public void setNClob(int ParameterIndex, NClob Value) throws SQLException {
    LogStatement.setNClob(ParameterIndex, Value);

  }

  @Override
  public void setClob(int ParameterIndex, Reader reader, long Length) throws SQLException {
    LogStatement.setClob(ParameterIndex, reader, Length);

  }

  @Override
  public void setBlob(int ParameterIndex, InputStream inputStream, long Length) throws SQLException {
    LogStatement.setBlob(ParameterIndex, inputStream, Length);

  }

  @Override
  public void setNClob(int ParameterIndex, Reader reader, long Length) throws SQLException {
    LogStatement.setNClob(ParameterIndex, reader, Length);

  }

  @Override
  public void setSQLXML(int ParameterIndex, SQLXML xmlObject) throws SQLException {
    LogStatement.setSQLXML(ParameterIndex, xmlObject);

  }

  @Override
  public void setAsciiStream(int ParameterIndex, InputStream x, long Length) throws SQLException {
    LogStatement.setAsciiStream(ParameterIndex, x, Length);

  }

  @Override
  public void setBinaryStream(int ParameterIndex, InputStream x, long Length) throws SQLException {
    LogStatement.setBinaryStream(ParameterIndex, x, Length);

  }

  @Override
  public void setCharacterStream(int ParameterIndex, Reader reader, long Length) throws SQLException {
    LogStatement.setCharacterStream(ParameterIndex, reader, Length);

  }

  @Override
  public void setAsciiStream(int ParameterIndex, InputStream x) throws SQLException {
    LogStatement.setAsciiStream(ParameterIndex, x);

  }

  @Override
  public void setBinaryStream(int ParameterIndex, InputStream x) throws SQLException {
    LogStatement.setBinaryStream(ParameterIndex, x);

  }

  @Override
  public void setCharacterStream(int ParameterIndex, Reader reader) throws SQLException {
    LogStatement.setCharacterStream(ParameterIndex, reader);

  }

  @Override
  public void setNCharacterStream(int ParameterIndex, Reader Value) throws SQLException {
    LogStatement.setNCharacterStream(ParameterIndex, Value);
  }

  @Override
  public void setClob(int ParameterIndex, Reader reader) throws SQLException {
    LogStatement.setClob(ParameterIndex, reader);

  }

  @Override
  public void setBlob(int ParameterIndex, InputStream inputStream) throws SQLException {
    LogStatement.setBlob(ParameterIndex, inputStream);

  }

  @Override
  public void setNClob(int ParameterIndex, Reader reader) throws SQLException {
    LogStatement.setNClob(ParameterIndex, reader);

  }

  @Override
  @Deprecated(since = "1.2")
  public void setUnicodeStream(int ParameterIndex, InputStream x, int Length) throws SQLException {
    LogStatement.setUnicodeStream(ParameterIndex, x, Length);
  }
}
