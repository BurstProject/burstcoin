package brs.db.sql;

import brs.Burst;
import brs.db.BurstKey;
import brs.db.VersionedEntityTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jooq.impl.TableImpl;
import org.jooq.BatchBindStep;
import org.jooq.SelectQuery;
import org.jooq.UpdateQuery;
import org.jooq.DeleteQuery;
import org.jooq.DSLContext;

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
        countQuery.addConditions(dbKey.getPKConditions(tableClass));
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
    trim(table, height, dbKeyFactory);
  }

  static void rollback(final String table, final TableImpl tableClass, final int height, final DbKey.Factory dbKeyFactory) {
    if (!Db.isInTransaction()) {
      throw new IllegalStateException("Not in transaction");
    }

    try ( DSLContext ctx = Db.getDSLContext() ) {
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
      DeleteQuery deleteQuery = ctx.deleteQuery(tableClass);
      deleteQuery.addConditions(tableClass.field("height", Integer.class).gt(height));

      for (DbKey dbKey : dbKeys) {
        ctx.transaction(configuration -> {
            SelectQuery selectMaxHeightQuery = ctx.selectQuery();
            selectMaxHeightQuery.addFrom(tableClass);
            selectMaxHeightQuery.addConditions(dbKey.getPKConditions(tableClass));
            selectMaxHeightQuery.addSelect(tableClass.field("height", Long.class).max());
            Integer maxHeight = (Integer) ctx.fetchValue(selectMaxHeightQuery);

            UpdateQuery setLatestQuery = ctx.updateQuery(tableClass);
            setLatestQuery.addValue(
              tableClass.field("latest", Boolean.class),
              true
            );
            setLatestQuery.addConditions(dbKey.getPKConditions(tableClass));
            setLatestQuery.addConditions(tableClass.field("height", int.class).eq(height));
            setLatestQuery.execute();
            //Db.getCache(table).remove(dbKey);
        });
      }
    }
    catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
    Db.getCache(table).clear();
  }

  static void trim(final String table, final int height, final DbKey.Factory dbKeyFactory) {
    if (!Db.isInTransaction()) {
      throw new IllegalStateException("Not in transaction");
    }
    try (Connection con = Db.getConnection();
         PreparedStatement pstmtSelect = con.prepareStatement("SELECT " + dbKeyFactory.getPKColumns() + ", MAX(height) AS max_height"
                                                              + " FROM " + DbUtils.quoteTableName(table) + " WHERE height < ? GROUP BY " + dbKeyFactory.getPKColumns() + " HAVING COUNT(DISTINCT height) > 1");
         PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + DbUtils.quoteTableName(table) + dbKeyFactory.getPKClause()
                                                              + " AND height < ?");

         PreparedStatement pstmtDeleteDeleted = con.prepareStatement(
                                                                     Db.getDatabaseType() == Db.TYPE.FIREBIRD
                                                                     ? "DELETE FROM " + DbUtils.quoteTableName(table) + " WHERE height < ? AND latest = FALSE "
                                                                     + " AND (" + String.join(" || '\\0' || ", dbKeyFactory.getPKColumns()) + ") NOT IN ( SELECT * FROM ( SELECT (" + String.join(" || '\\0' || ", dbKeyFactory.getPKColumns()) + ") AS ac1v FROM "
                                                                     + DbUtils.quoteTableName(table) + " WHERE height >= ?) ac0v )"
                                                                     : "DELETE FROM " + DbUtils.quoteTableName(table) + " WHERE height < ? AND latest = FALSE "
                                                                     + " AND CONCAT_WS('\\0', " + dbKeyFactory.getPKColumns() + ") NOT IN ( SELECT * FROM ( SELECT CONCAT_WS('\\0', " + dbKeyFactory.getPKColumns() + ") FROM "
                                                                     + DbUtils.quoteTableName(table) + " WHERE height >= ?) ac0v )"
                                                                     )) {

      // logger.info( "DELETE PK columns: ", dbKeyFactory.getPKColumns() );
      pstmtSelect.setInt(1, height);
      try (ResultSet rs = pstmtSelect.executeQuery()) {
        while (rs.next()) {
          DbKey dbKey = (DbKey) dbKeyFactory.newKey(rs);
          int maxHeight = rs.getInt("max_height");
          int i = 1;
          i = dbKey.setPK(pstmtDelete, i);
          pstmtDelete.setInt(i, maxHeight);
          pstmtDelete.executeUpdate();
        }
        pstmtDeleteDeleted.setInt(1, height);
        pstmtDeleteDeleted.setInt(2, height);
        pstmtDeleteDeleted.executeUpdate();
      }
    } catch (SQLException e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

}
