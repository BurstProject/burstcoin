package brs.db.sql;

import brs.db.BurstKey;
import brs.db.ValuesTable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.jooq.impl.TableImpl;
import org.jooq.DSLContext;
import org.jooq.SelectQuery;
import org.jooq.UpdateQuery;

public abstract class ValuesSqlTable<T,V> extends DerivedSqlTable implements ValuesTable<T, V> {

  private final boolean multiversion;
  protected final DbKey.Factory<T> dbKeyFactory;

  protected ValuesSqlTable(String table, TableImpl<?> tableClass, DbKey.Factory<T> dbKeyFactory) {
    this(table, tableClass, dbKeyFactory, false);
  }

  ValuesSqlTable(String table, TableImpl<?> tableClass, DbKey.Factory<T> dbKeyFactory, boolean multiversion) {
    super(table, tableClass);
    this.dbKeyFactory = dbKeyFactory;
    this.multiversion = multiversion;
  }

  protected abstract V load(DSLContext ctx, ResultSet rs) throws SQLException;

  protected abstract void save(DSLContext ctx, T t, V v) throws SQLException;

  @Override
  public final List<V> get(BurstKey nxtKey) {
    DbKey dbKey = (DbKey) nxtKey;
    List<V> values;
    if (Db.isInTransaction()) {
      values = (List<V>)Db.getCache(table).get(dbKey);
      if (values != null) {
        return values;
      }
    }
    try ( DSLContext ctx = Db.getDSLContext() ) {
      SelectQuery query = ctx.selectQuery();
      query.addFrom(tableClass);
      query.addConditions(dbKey.getPKConditions(tableClass));
      if ( multiversion ) {
        query.addConditions(tableClass.field("latest", int.class).isTrue());
      }
      query.addOrderBy(tableClass.field("db_id").desc());
      values = get(ctx, query.fetchResultSet());
      if (Db.isInTransaction()) {
        Db.getCache(table).put(dbKey, values);
      }
      return values;
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  private List<V> get(DSLContext ctx, ResultSet rs) {
    try {
      List<V> result = new ArrayList<>();
      while (rs.next()) {
        result.add(load(ctx, rs));
      }
      return result;
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  @Override
  public final void insert(T t, List<V> values) {
    if (!Db.isInTransaction()) {
      throw new IllegalStateException("Not in transaction");
    }
    DbKey dbKey = (DbKey)dbKeyFactory.newKey(t);
    Db.getCache(table).put(dbKey, values);
    try ( DSLContext ctx = Db.getDSLContext() ) {
      if (multiversion) {
        UpdateQuery query = ctx.updateQuery(tableClass);
        query.addValue(
          tableClass.field("latest", Boolean.class),
          false
        );
        query.addConditions(dbKey.getPKConditions(tableClass));
        query.addConditions(tableClass.field("latest", Boolean.class).isTrue());
        query.execute();
      }
      for (V v : values) {
        save(ctx, t, v);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  @Override
  public void rollback(int height) {
    super.rollback(height);
    Db.getCache(table).clear();
  }

  @Override
  public final void truncate() {
    super.truncate();
    Db.getCache(table).clear();
  }

}
