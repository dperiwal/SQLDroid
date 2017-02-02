package org.sqldroid;

import android.database.Cursor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.Statement;

public class SQLDroidStatement implements Statement {

  private SQLiteDatabase db;
  private SQLDroidConnection sqldroidConnection;
  private SQLDroidResultSet rs = null;
  protected StringBuffer sqlBatch = new StringBuffer();
  private Integer maxRows = null;

  /** The update count.  We don't know this, but need to respond in such a way that:
   * (from getMoreResults) There are no more results when the following is true:
   *
   *      // stmt is a Statement object
   *      ((stmt.getMoreResults() == false) &amp;&amp; (stmt.getUpdateCount() == -1))

   * This is used by <code>getUpdateCount()</code>.  If there is a resultSet
   * then getUpdateCount will return -1.  If there is no result set, then, presumably,
   * <code>execute()</code> was called and we have one result and so can return something
   * other than -1 on the first call to getUpdateCount.   In this case, the second call to getUpdateCount
   * we should return -1;
   * We set this to zero on execute() and decrement it on getUpdateCount.  If the value of updateCount
   * is -1 then we just return it from getUpdateCount.
   */
  public int updateCount = -1;
  private boolean poolable = false;

  public SQLDroidStatement(SQLDroidConnection sqldroid) {
    this.sqldroidConnection = sqldroid;
    this.db = sqldroid.getDb();
  }

  @Override
  public void addBatch(String sql) throws SQLException {
    //sql must be a static sql
    this.sqlBatch.append(sql);
  }

  @Override
  public void cancel() throws SQLException {
    throw new SQLFeatureNotSupportedException("cancel not supported");
  }

  @Override
  public void clearBatch() throws SQLException {
    sqlBatch = new StringBuffer();
  }

  @Override
  public void clearWarnings() throws SQLException {
    // TODO Evaluate if the implementation is sufficient (if so, delete comment and log)
    Log.e(" ********************* not implemented @ " + DebugPrinter.getFileName() + " line " + DebugPrinter.getLineNumber());
  }

  @Override
  public void close() throws SQLException {
    closeResultSet();
    sqldroidConnection = null;
    db = null;
  }

  /** Close the result set (if open) and null the rs variable. */
  public void closeResultSet() throws SQLException {
    if (rs != null) {
      if (!rs.isClosed()) {
        rs.close();
      }
      rs = null;
    }
  }

  @Override
  /** Execute the SQL statement.
   * @return false if there are no result (if the request was not a select or similar).  True if a
   * result set is available.  This meets the requirement of java.sql.Statement.
   */
  public boolean execute(String sql) throws SQLException {
    updateCount = -1;  // default outcome.  If the sql is a query or any other sql fails.
    closeResultSet();
    boolean isSelect = sql.toUpperCase().matches("(?m)(?s)\\s*(SELECT|PRAGMA|EXPLAIN QUERY PLAN).*");
    if ( rs != null && !rs.isClosed() ) {
      rs.close();
    }
    if (isSelect) {
      String limitedSql = sql + (maxRows != null ? " LIMIT " + maxRows : "");
      Cursor c = db.rawQuery(limitedSql, new String[0]);
      rs = new SQLDroidResultSet(c);
      if (c.getCount()==0)
           return false;
    } else {
      db.execSQL(sql);
      rs = null;
      updateCount = sqldroidConnection.changedRowsCount();
    }

    boolean resultSetAvailable = (rs != null);

    return resultSetAvailable;
  }

  @Override
  public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
    // TODO: Could be implemented like SQLDroidPreparedStatement
    throw new SQLFeatureNotSupportedException("execute not supported - use executeUpdate or executeQuery");
  }

  @Override
  public boolean execute(String sql, int[] columnIndexes) throws SQLException {
    throw new SQLFeatureNotSupportedException("execute not supported - use executeUpdate or executeQuery");
  }

  @Override
  public boolean execute(String sql, String[] columnNames) throws SQLException {
    throw new SQLFeatureNotSupportedException("execute not supported - use executeUpdate or executeQuery");
  }

  @Override
  public int[] executeBatch() throws SQLException {
    updateCount = -1;
    int[] results = new int[1];
    results[0] = EXECUTE_FAILED;
    db.execSQL(sqlBatch.toString());
    results[0] = sqldroidConnection.changedRowsCount();
    updateCount = results[0];
    return results;
  }

  @Override
  public ResultSet executeQuery(String sql) throws SQLException {
    closeResultSet();
    Cursor c = db.rawQuery(sql, null);
    rs = new SQLDroidResultSet(c);
    return rs;
  }

  @Override
  public int executeUpdate(String sql) throws SQLException {
    closeResultSet();
    db.execSQL(sql);
    updateCount = sqldroidConnection.changedRowsCount();
    return updateCount;
  }

  @Override
  public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
    return executeUpdate(sql);
  }

  @Override
  public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
    throw new SQLFeatureNotSupportedException("executeUpdate(String,int[]) not supported - use executeUpdate(String)");
  }

  @Override
  public int executeUpdate(String sql, String[] columnNames) throws SQLException {
    throw new SQLFeatureNotSupportedException("executeUpdate(String,String[]) not supported - use executeUpdate(String)");
  }

  @Override
  public Connection getConnection() throws SQLException {
    return sqldroidConnection;
  }

  @Override
  public int getFetchDirection() throws SQLException {
    // TODO Avoid NPE for rs
    return rs.getFetchDirection();
  }

  @Override
  public int getFetchSize() throws SQLException {
    // TODO Avoid NPE for rs
    return rs.getFetchSize();
  }

  @Override
  public ResultSet getGeneratedKeys() throws SQLException {
    return sqldroidConnection.getGeneratedRowIdResultSet();
  }

  @Override
  public int getMaxFieldSize() throws SQLException {
    return 0;
  }

  @Override
  public int getMaxRows() throws SQLException {
    // TODO: return rs.getMaxRows()
    // TODO: Avoid NPE for rs
    Log.e(" ********************* not implemented @ " + DebugPrinter.getFileName() + " line "
        + DebugPrinter.getLineNumber());
    return 0;
  }

  @Override
  public boolean getMoreResults() throws SQLException {
    return getMoreResults(CLOSE_CURRENT_RESULT);
  }

  @Override
  public boolean getMoreResults(int current) throws SQLException {
    if ( current == CLOSE_CURRENT_RESULT ) {
      closeResultSet();
    }
    return false;
  }

  @Override
  public int getQueryTimeout() throws SQLException {
    throw new UnsupportedOperationException("getQueryTimeout not implemented yet");
  }

  @Override
  public ResultSet getResultSet() throws SQLException {
    return rs;
  }

  @Override
  public int getResultSetConcurrency() throws SQLException {
    return ResultSet.CONCUR_READ_ONLY;
  }

  @Override
  public int getResultSetHoldability() throws SQLException {
    return ResultSet.CLOSE_CURSORS_AT_COMMIT;
  }

  @Override
  public int getResultSetType() throws SQLException {
    return ResultSet.TYPE_FORWARD_ONLY;
  }

  @Override
  public int getUpdateCount() throws SQLException {
    if ( updateCount != -1 ) {  // for any successful update/insert, update count will have been set
      // the documenation states that you're only supposed to call this once per result.
      // on subsequent calls, we'll return -1 (which would appear to be the correct return
      int tmp = updateCount;
      updateCount = -1;
      return tmp;
    }
    return updateCount;  // if the result was a result set, or this is the second call, then this will be -1
  }

  @Override
  public SQLWarning getWarnings() throws SQLException {
    // TODO Evaluate if the implementation is sufficient (if so, delete comment and log)
    Log.e(" ********************* not implemented @ " + DebugPrinter.getFileName() + " line "
        + DebugPrinter.getLineNumber());
    return null;
  }

  @Override
  public void setCursorName(String name) throws SQLException {
  }

  @Override
  public void setEscapeProcessing(boolean enable) throws SQLException {
    if (!enable) {
      throw new UnsupportedOperationException("setEscapeProcessing not implemented yet");
    }
  }

  @Override
  public void setFetchDirection(int direction) throws SQLException {
    // TODO: Avoid NPE
    rs.setFetchDirection(direction);
  }

  @Override
  public void setFetchSize(int rows) throws SQLException {
    // TODO: Avoid NPE
    rs.setFetchSize(rows);
  }

  @Override
  public void setMaxFieldSize(int max) throws SQLException {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void setMaxRows(int max) throws SQLException {
    if (isClosed()) {
      throw new SQLException("Statement is closed.");
    } else if (max < 0) {
      throw new SQLException("Max rows must be zero or positive. Got " + max);
    } else if (max == 0) {
      maxRows = null;
    } else {
      maxRows = max;
    }
  }

  @Override
  public void setQueryTimeout(int seconds) throws SQLException {
    throw new UnsupportedOperationException("setQueryTimeout not implemented yet");
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return iface != null && iface.isAssignableFrom(getClass());
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    if (isWrapperFor(iface)) {
      return (T) this;
    }
    throw new SQLException(getClass() + " does not wrap " + iface);
  }

  @Override
  public boolean isClosed() throws SQLException {
    return sqldroidConnection == null;
  }

  @Override
  public boolean isPoolable() throws SQLException {
    return poolable;
  }

  @Override
  public void setPoolable(boolean poolable) throws SQLException {
    this.poolable = poolable;
  }

  // methods added for JDK7 compilation

  public boolean isCloseOnCompletion() throws SQLException {
    return false;
  }

  public void closeOnCompletion() throws SQLException {
    throw new UnsupportedOperationException("closeOnCompletion not implemented yet");
  }
}
