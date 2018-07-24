package brs.db;

import java.sql.ResultSet;
import java.util.Iterator;
import org.jooq.DSLContext;

public interface BurstIterator<T> extends Iterator<T>, AutoCloseable {
  @Override
  boolean hasNext();

  @Override
  T next();

  @Override
  void remove();

  @Override
  void close();

  interface ResultSetReader<T> {
    T get(DSLContext ctx, ResultSet rs) throws Exception;
  }
}
