package nxt.db.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface NxtKey {

    interface Factory<T> {
        DbKey newKey(T t);

        DbKey newKey(ResultSet rs) throws SQLException;
    }

    int setPK(PreparedStatement pstmt) throws SQLException;

    int setPK(PreparedStatement pstmt, int index) throws SQLException;


    interface LongKeyFactory<T> extends Factory<T> {
        @Override
        DbKey newKey(ResultSet rs);

        DbKey newKey(long id);

    }

    interface LinkKeyFactory<T> extends Factory<T> {
        DbKey newKey(long idA, long idB);
    }
}
