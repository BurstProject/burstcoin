package brs.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Iterator;

public interface BurstIterator<T> extends Iterator<T>, Iterable<T>, AutoCloseable {
  @Override
  boolean hasNext();

  @Override
  T next();

  @Override
  void remove();

  @Override
  void close();

  @Override
  Iterator<T> iterator();

  public interface ResultSetReader<T> {
    T get(Connection con, ResultSet rs) throws Exception;
  }
}
