package nxt.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

public final class DbIterator<T> implements Iterator<T>, AutoCloseable {

    public interface ResultSetReader<T> {
        public T get(Connection con, ResultSet rs) throws Exception;
    }

    private final Connection con;
    private final PreparedStatement pstmt;
    private final ResultSetReader<T> rsReader;
    private final ResultSet rs;

    private boolean hasNext;

    public DbIterator(Connection con, PreparedStatement pstmt, ResultSetReader<T> rsReader) {
        this.con = con;
        this.pstmt = pstmt;
        this.rsReader = rsReader;
        try {
            this.rs = pstmt.executeQuery();
            this.hasNext = rs.next();
        } catch (SQLException e) {
            DbUtils.close(pstmt, con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public boolean hasNext() {
        if (! hasNext) {
            DbUtils.close(rs, pstmt, con);
        }
        return hasNext;
    }

    @Override
    public T next() {
        if (! hasNext) {
            DbUtils.close(rs, pstmt, con);
            return null;
        }
        try {
            T result = rsReader.get(con, rs);
            hasNext = rs.next();
            return result;
        } catch (Exception e) {
            DbUtils.close(rs, pstmt, con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Removal not suported");
    }

    @Override
    public void close() {
        DbUtils.close(rs, pstmt, con);
    }

}
