package nxt.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Iterator;

/**
 * Created by jens on 10.08.2017.
 */
public interface NxtIterator<T> extends Iterator<T>, Iterable<T>, AutoCloseable
{
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
