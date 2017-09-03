package nxt.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface NxtKey {

    interface Factory<T> {
        NxtKey newKey(T t);

        NxtKey newKey(ResultSet rs) throws SQLException;
    }

    int setPK(PreparedStatement pstmt) throws SQLException;

    int setPK(PreparedStatement pstmt, int index) throws SQLException;


    interface LongKeyFactory<T> extends Factory<T> {
        @Override
        NxtKey newKey(ResultSet rs);

        NxtKey newKey(long id);

    }

    interface LinkKeyFactory<T> extends Factory<T> {
        NxtKey newKey(long idA, long idB);
    }
}
