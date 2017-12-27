package brs.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface BurstKey {

  interface Factory<T> {
    BurstKey newKey(T t);

    BurstKey newKey(ResultSet rs) throws SQLException;
  }

  int setPK(PreparedStatement pstmt) throws SQLException;

  int setPK(PreparedStatement pstmt, int index) throws SQLException;

  long[] getPKValues();

  interface LongKeyFactory<T> extends Factory<T> {
    @Override
    BurstKey newKey(ResultSet rs);

    BurstKey newKey(long id);

  }

  interface LinkKeyFactory<T> extends Factory<T> {
    BurstKey newKey(long idA, long idB);
  }
}
