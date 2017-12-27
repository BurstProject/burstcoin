package brs.db.sql;

import brs.db.BurstIterator;
import brs.db.BurstKey;
import brs.db.VersionedBatchEntityTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

import org.jooq.impl.TableImpl;
import org.jooq.Condition;
import org.jooq.SelectQuery;
import org.jooq.Query;
import org.jooq.Merge;
import org.jooq.BatchBindStep;
import org.jooq.DSLContext;

public abstract class VersionedBatchEntitySqlTable<T> extends VersionedEntitySqlTable<T> implements VersionedBatchEntityTable<T> {
  protected VersionedBatchEntitySqlTable(String table, TableImpl<?> tableClass, DbKey.Factory<T> dbKeyFactory) {
    super(table, tableClass, dbKeyFactory);
  }

  protected abstract void updateUsing(DSLContext ctx, T t) throws SQLException;

  @Override
  public boolean delete(T t) {
    if(!Db.isInTransaction()) {
      throw new IllegalStateException("Not in transaction");
    }
    DbKey dbKey = (DbKey)dbKeyFactory.newKey(t);
    Db.getBatch(table).put(dbKey, null);
    Db.getCache(table).remove(dbKey);

    return true;
  }

  @Override
  public T get(BurstKey dbKey) {
    if(Db.isInTransaction()) {
      if(Db.getBatch(table).containsKey(dbKey)) {
        return (T)Db.getBatch(table).get(dbKey);
      }
    }
    return super.get(dbKey);
  }

  @Override
  public void insert(T t) {
    if(!Db.isInTransaction()) {
      throw new IllegalStateException("Not in transaction");
    }
    DbKey dbKey = (DbKey)dbKeyFactory.newKey(t);
    Db.getBatch(table).put(dbKey, t);
    Db.getCache(table).put(dbKey, t);
  }

  @Override
  public void finish() {
    if(!Db.isInTransaction()) {
      throw new IllegalStateException("Not in transaction");
    }

    try ( DSLContext ctx = Db.getDSLContext() ) {
      BatchBindStep adjustLatestBatch = ctx.batch(
        ctx.update(tableClass).set(tableClass.field("latest", Boolean.class), false).where(
          tableClass.field("latest", Boolean.class).isTrue().and(
            tableClass.getPrimaryKey().getFields().get(0).eq(null)
          )
        )
      );

      Set keySet = Db.getBatch(table).keySet();
      Iterator<DbKey> it = keySet.iterator();
      while(it.hasNext()) {
        DbKey key = it.next();
        adjustLatestBatch.bind(false, key.getPKValues()[0]);
      }
      adjustLatestBatch.execute();
    }
    catch(SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }

    // ToDo: do a real batch here?
    try ( DSLContext ctx = Db.getDSLContext() ) {
      List<Map.Entry<DbKey,Object>> entries = new ArrayList<>(Db.getBatch(table).entrySet());
      for ( Map.Entry<DbKey,Object> entry: entries) {
        if(entry.getValue() != null) {
          updateUsing(ctx, (T)entry.getValue());
        }
      }
    }
    catch(SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  @Override
  public T get(BurstKey dbKey, int height) {
    if(Db.isInTransaction()) {
      throw new IllegalStateException("Cannot use in batch table transaction");
    }
    return super.get(dbKey, height);
  }

  @Override
  public T getBy(Condition condition) {
    if(Db.isInTransaction()) {
      throw new IllegalStateException("Cannot use in batch table transaction");
    }
    return super.getBy(condition);
  }

  @Override
  public T getBy(Condition condition, int height) {
    if(Db.isInTransaction()) {
      throw new IllegalStateException("Cannot use in batch table transaction");
    }
    return super.getBy(condition, height);
  }

  @Override
  public BurstIterator<T> getManyBy(Condition condition, int from, int to) {
    if(Db.isInTransaction()) {
      throw new IllegalStateException("Cannot use in batch table transaction");
    }
    return super.getManyBy(condition, from, to);
  }

  @Override
  public BurstIterator<T> getManyBy(Condition condition, int from, int to, String sort) {
    if(Db.isInTransaction()) {
      throw new IllegalStateException("Cannot use in batch table transaction");
    }
    return super.getManyBy(condition, from, to, sort);
  }

  @Override
  public BurstIterator<T> getManyBy(Condition condition, int height, int from, int to) {
    if(Db.isInTransaction()) {
      throw new IllegalStateException("Cannot use in batch table transaction");
    }
    return super.getManyBy(condition, height, from, to);
  }

  @Override
  public BurstIterator<T> getManyBy(Condition condition, int height, int from, int to, String sort) {
    if(Db.isInTransaction()) {
      throw new IllegalStateException("Cannot use in batch table transaction");
    }
    return super.getManyBy(condition, height, from, to, sort);
  }

  @Override
  public BurstIterator<T> getManyBy(DSLContext ctx, SelectQuery query, boolean cache) {
    if(Db.isInTransaction()) {
      throw new IllegalStateException("Cannot use in batch table transaction");
    }
    return super.getManyBy(ctx, query, cache);
  }

  @Override
  public BurstIterator<T> getAll(int from, int to) {
    if(Db.isInTransaction()) {
      throw new IllegalStateException("Cannot use in batch table transaction");
    }
    return super.getAll(from, to);
  }

  @Override
  public BurstIterator<T> getAll(int from, int to, String sort) {
    if(Db.isInTransaction()) {
      throw new IllegalStateException("Cannot use in batch table transaction");
    }
    return super.getAll(from, to, sort);
  }

  @Override
  public BurstIterator<T> getAll(int height, int from, int to) {
    if(Db.isInTransaction()) {
      throw new IllegalStateException("Cannot use in batch table transaction");
    }
    return super.getAll(height, from, to);
  }

  @Override
  public BurstIterator<T> getAll(int height, int from, int to, String sort) {
    if(Db.isInTransaction()) {
      throw new IllegalStateException("Cannot use in batch table transaction");
    }
    return super.getAll(height, from, to, sort);
  }

  @Override
  public int getCount() {
    if(Db.isInTransaction()) {
      throw new IllegalStateException("Cannot use in batch table transaction");
    }
    return super.getCount();
  }

  @Override
  public int getRowCount() {
    if(Db.isInTransaction()) {
      throw new IllegalStateException("Cannot use in batch table transaction");
    }
    return super.getRowCount();
  }

  @Override
  public void rollback(int height) {
    super.rollback(height);
    Db.getBatch(table).clear();
  }

  @Override
  public void truncate() {
    super.truncate();
    Db.getBatch(table).clear();
  }
}
