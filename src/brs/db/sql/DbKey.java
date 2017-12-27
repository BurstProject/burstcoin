package brs.db.sql;

import brs.db.BurstKey;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jooq.Table;
import org.jooq.Condition;
import org.jooq.SelectQuery;
import org.jooq.TableField;

public interface DbKey extends BurstKey {

  public static abstract class Factory<T> implements BurstKey.Factory<T> {

    private final String pkClause;
    private final String pkColumns;
    private final String selfJoinClause;
    private final int pkVariables;

    protected Factory(String pkClause, String pkColumns, String selfJoinClause) {
      this.pkClause = pkClause;
      this.pkColumns = pkColumns;
      this.selfJoinClause = selfJoinClause;
      this.pkVariables = org.apache.commons.lang.StringUtils.countMatches(pkClause, "?");
    }

    public abstract BurstKey newKey(T t);

    public abstract BurstKey newKey(ResultSet rs) throws SQLException;

    public final String getPKClause() {
      return pkClause;
    }

    public final String getPKColumns() {
      return pkColumns;
    }

    // expects tables to be named a and b
    public final String getSelfJoinClause() {
      return selfJoinClause;
    }

    /** @return The number of variables in PKClause */
    public int getPkVariables() {
      return pkVariables;
    }

    public abstract void applySelfJoin(SelectQuery query, Table queryTable, Table otherTable);

  }

  int setPK(PreparedStatement pstmt) throws SQLException;

  int setPK(PreparedStatement pstmt, int index) throws SQLException;

  void applyPKClause(SelectQuery query, Table tableClass);

  long[] getPKValues();

  public static abstract class LongKeyFactory<T> extends Factory<T> implements BurstKey.LongKeyFactory<T> {

    private final String idColumn;

    public LongKeyFactory(String idColumn) {
      super(" WHERE " + idColumn + " = ? ",
            idColumn,
            " a." + idColumn + " = b." + idColumn + " ");
      this.idColumn = idColumn;
    }

    @Override
    public BurstKey newKey(ResultSet rs) {
      try {
        return new LongKey(rs.getLong(idColumn), idColumn);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    public BurstKey newKey(long id) {
      return new LongKey(id, idColumn);
    }

    @Override
    public void applySelfJoin(SelectQuery query, Table queryTable, Table otherTable) {
      query.addConditions(
        queryTable.field(idColumn).eq(
          otherTable.field(idColumn)
        )
      );
    }
  }

  public static abstract class LinkKeyFactory<T> extends Factory<T> implements BurstKey.LinkKeyFactory<T> {

    private final String idColumnA;
    private final String idColumnB;

    public LinkKeyFactory(String idColumnA, String idColumnB) {
      super(" WHERE " + idColumnA + " = ? AND " + idColumnB + " = ? ",
            idColumnA + ", " + idColumnB,
            " a." + idColumnA + " = b." + idColumnA + " AND a." + idColumnB + " = b." + idColumnB + " ");
      this.idColumnA = idColumnA;
      this.idColumnB = idColumnB;
    }

    @Override
    public BurstKey newKey(ResultSet rs) {
      try {
        return new LinkKey(rs.getLong(idColumnA), rs.getLong(idColumnB), idColumnA, idColumnB);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }

    public BurstKey newKey(long idA, long idB) {
      return new LinkKey(idA, idB, idColumnA, idColumnB);
    }

    @Override
    public void applySelfJoin(SelectQuery query, Table queryTable, Table otherTable) {
      query.addConditions(
        queryTable.field(idColumnA).eq(
          otherTable.field(idColumnA)
        )
      );
      query.addConditions(
        queryTable.field(idColumnB).eq(
          otherTable.field(idColumnB)
        )
      );
    }
  }

  static final class LongKey implements DbKey {

    private final long id;
    private final String idColumn;

    private LongKey(long id, String idColumn) {
      this.id       = id;
      this.idColumn = idColumn;
    }

    @Override
    public int setPK(PreparedStatement pstmt) throws SQLException {
      return setPK(pstmt, 1);
    }

    @Override
    public int setPK(PreparedStatement pstmt, int index) throws SQLException {
      pstmt.setLong(index, id);
      return index + 1;
    }

    @Override
    public long[] getPKValues() {
      long[] values = {id};
      return values;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof LongKey && ((LongKey) o).id == id;
    }

    @Override
    public int hashCode() {
      return (int) (id ^ (id >>> 32));
    }

    @Override
    public void applyPKClause(SelectQuery query, Table tableClass) {
      query.addConditions(tableClass.field(idColumn, Long.class).eq(id));
    }
  }

  static final class LinkKey implements DbKey {

    private final long idA;
    private final long idB;
    private final String idColumnA;
    private final String idColumnB;

    private LinkKey(long idA, long idB, String idColumnA, String idColumnB) {
      this.idA       = idA;
      this.idB       = idB;
      this.idColumnA = idColumnA;
      this.idColumnB = idColumnB;
    }

    @Override
    public int setPK(PreparedStatement pstmt) throws SQLException {
      return setPK(pstmt, 1);
    }

    @Override
    public int setPK(PreparedStatement pstmt, int index) throws SQLException {
      pstmt.setLong(index, idA);
      pstmt.setLong(index + 1, idB);
      return index + 2;
    }

    @Override
    public long[] getPKValues() {
      long[] values = {idA, idB};
      return values;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof LinkKey && ((LinkKey) o).idA == idA && ((LinkKey) o).idB == idB;
    }

    @Override
    public int hashCode() {
      return (int) (idA ^ (idA >>> 32)) ^ (int) (idB ^ (idB >>> 32));
    }

    @Override
    public void applyPKClause(SelectQuery query, Table tableClass) {
      query.addConditions(tableClass.field(idColumnA, Long.class).eq(idA));
      query.addConditions(tableClass.field(idColumnB, Long.class).eq(idB));
    }
  }

}
