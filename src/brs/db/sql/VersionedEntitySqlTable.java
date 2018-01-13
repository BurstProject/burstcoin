package brs.db.sql;

import brs.Burst;
import brs.db.BurstKey;
import brs.db.VersionedEntityTable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.jooq.impl.TableImpl;
import org.jooq.SelectQuery;
import org.jooq.UpdateQuery;
import org.jooq.DeleteQuery;
import org.jooq.DSLContext;
import org.jooq.Field;

public abstract class VersionedEntitySqlTable<T> extends EntitySqlTable<T> implements VersionedEntityTable<T> {

  protected VersionedEntitySqlTable(String table, TableImpl<?> tableClass, BurstKey.Factory<T> dbKeyFactory) {
    super(table, tableClass, dbKeyFactory, true);
  }

  @Override
  public void rollback(int height) {
    rollback(table, tableClass, height, dbKeyFactory);
  }

  @Override
  public boolean delete(T t) {
    if (t == null) {
      return false;
    }
    if (!Db.isInTransaction()) {
      throw new IllegalStateException("Not in transaction");
    }
    DbKey dbKey = (DbKey) dbKeyFactory.newKey(t);
    try ( DSLContext ctx = Db.getDSLContext() ) {
      SelectQuery countQuery = ctx.selectQuery();
      countQuery.addFrom(tableClass);
      countQuery.addConditions(dbKey.getPKConditions(tableClass));
      countQuery.addConditions(tableClass.field("height", Integer.class).lt(Burst.getBlockchain().getHeight()));
      if ( countQuery.fetchCount() > 0 ) {
        UpdateQuery updateQuery = ctx.updateQuery(tableClass);
        updateQuery.addValue(
          tableClass.field("latest", Boolean.class),
          false
        );
        updateQuery.addConditions(dbKey.getPKConditions(tableClass));
        updateQuery.addConditions(tableClass.field("latest", Boolean.class).isTrue());

        updateQuery.execute();
        save(ctx, t);
        // delete after the save
        updateQuery.execute();
        
        return true;
      }
      else {
        DeleteQuery deleteQuery = ctx.deleteQuery(tableClass);
        deleteQuery.addConditions(dbKey.getPKConditions(tableClass));
        return deleteQuery.execute() > 0;
      }
    }
    catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
    finally {
      Db.getCache(table).remove(dbKey);
    }
  }

  @Override
  public final void trim(int height) {
    trim(table, tableClass, height, dbKeyFactory);
  }

  static void rollback(final String table, final TableImpl tableClass, final int height, final DbKey.Factory dbKeyFactory) {
    if (!Db.isInTransaction()) {
      throw new IllegalStateException("Not in transaction");
    }

    try ( DSLContext ctx = Db.getDSLContext() ) {
      // get dbKey's for entries whose stuff newer than height would be deleted, to allow fixing
      // their latest flag of the "potential" remaining newest entry
      SelectQuery selectForDeleteQuery = ctx.selectQuery();
      selectForDeleteQuery.addFrom(tableClass);
      selectForDeleteQuery.addConditions(tableClass.field("height", Integer.class).gt(height));
      for ( String column : dbKeyFactory.getPKColumns() ) {
        selectForDeleteQuery.addSelect(tableClass.field(column, Long.class));
      }
      selectForDeleteQuery.setDistinct(true);
      List<DbKey> dbKeys = new ArrayList<>();
      try ( ResultSet toDeleteResultset = selectForDeleteQuery.fetchResultSet() ) {
        while ( toDeleteResultset.next() ) {
          dbKeys.add((DbKey) dbKeyFactory.newKey(toDeleteResultset));
        }
      }

      // delete all entries > height
      DeleteQuery deleteQuery = ctx.deleteQuery(tableClass);
      deleteQuery.addConditions(tableClass.field("height", Integer.class).gt(height));
      deleteQuery.execute();

      // update latest flags for remaining entries, if there any remaining (per deleted dbKey)
      for (DbKey dbKey : dbKeys) {
        SelectQuery selectMaxHeightQuery = ctx.selectQuery();
        selectMaxHeightQuery.addFrom(tableClass);
        selectMaxHeightQuery.addConditions(dbKey.getPKConditions(tableClass));
        selectMaxHeightQuery.addSelect(tableClass.field("height", Integer.class).max());
        Integer maxHeight = (Integer) ctx.fetchValue(selectMaxHeightQuery.fetchResultSet(), tableClass.field("height", Integer.class));

        System.out.println("select max: " + selectMaxHeightQuery.getSQL(true));
        System.out.println("    max height for: " + dbKeyFactory.getSelfJoinClause() + " should be set to " + maxHeight);
        if ( maxHeight != null ) {
          UpdateQuery setLatestQuery = ctx.updateQuery(tableClass);
          setLatestQuery.addConditions(dbKey.getPKConditions(tableClass));
          setLatestQuery.addConditions(tableClass.field("height", int.class).eq(maxHeight));
          setLatestQuery.addValue(
            tableClass.field("latest", Boolean.class),
            true
          );
          System.out.println("        set latest by: " + setLatestQuery.getSQL(true));
          setLatestQuery.execute();
        }
      }
    }
    catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
    Db.getCache(table).clear();
  }

  static void trim(final String table, final TableImpl tableClass, final int height, final DbKey.Factory dbKeyFactory) {
    if (!Db.isInTransaction()) {
      throw new IllegalStateException("Not in transaction");
    }
    try ( DSLContext ctx = Db.getDSLContext() ) {
      SelectQuery selectMaxHeightQuery = ctx.selectQuery();
      selectMaxHeightQuery.addFrom(tableClass);
      selectMaxHeightQuery.addSelect(tableClass.field("height", Long.class).max().as("max_height"));
      for ( String column : dbKeyFactory.getPKColumns() ) {
        Field pkField = tableClass.field(column, Long.class);
        selectMaxHeightQuery.addSelect(pkField);
        selectMaxHeightQuery.addGroupBy(pkField);
      }
      selectMaxHeightQuery.addConditions(tableClass.field("height", Long.class).lt(height));
      selectMaxHeightQuery.addHaving(tableClass.field("height", Long.class).countDistinct().gt(1));
      
      try {
        try ( ResultSet rs = selectMaxHeightQuery.fetchResultSet() ) {
          while ( rs.next() ) {
            DbKey dbKey = (DbKey) dbKeyFactory.newKey(rs);
            int maxHeight = rs.getInt("max_height");

            DeleteQuery deleteLowerHeightQuery = ctx.deleteQuery(tableClass);
            deleteLowerHeightQuery.addConditions(tableClass.field("height", Integer.class).lt(maxHeight));
            deleteLowerHeightQuery.addConditions(dbKey.getPKConditions(tableClass));
            deleteLowerHeightQuery.execute();
          }
        }
        catch (Exception e) {
          throw new RuntimeException(e.toString(), e);
        }

        SelectQuery keepQuery = ctx.selectQuery();
        keepQuery.addFrom(tableClass);
        keepQuery.addSelect(tableClass.field("db_id", Long.class));
        keepQuery.addConditions(tableClass.field("height", Integer.class).ge(height));
        keepQuery.asTable("pocc");
        
        //        Table<Record> keepQuery = ctx.select(tableClass.field("db_id", Long.class)).from(tableClass).where(tableClass.field("height", Integer.class).ge(height)).asTable("pocc");
        DeleteQuery deleteQuery = ctx.deleteQuery(tableClass);
        deleteQuery.addConditions(
          tableClass.field("height", Long.class).lt(height),
          tableClass.field("latest", Boolean.class).isFalse(),
          tableClass.field("db_id", Long.class).notIn(ctx.select(keepQuery.fields()).from(keepQuery))
        );

        deleteQuery.execute();
      }
      catch (Exception e) {
        throw new RuntimeException(e.toString(), e);
      }
    }
    catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

}
