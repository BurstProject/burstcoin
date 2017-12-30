package brs.db.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
  
import java.sql.SQLException;

import org.jooq.impl.DSL;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.TableImpl;
import org.jooq.Field;
import org.jooq.Condition;
import org.jooq.UpdateQuery;
import org.jooq.InsertQuery;
import org.jooq.SelectQuery;

public final class DbUtils {

  private static final Logger logger = LoggerFactory.getLogger(DbUtils.class);
  private DbUtils() {
  } // never

  public static void close(AutoCloseable... closeables) {
    for (AutoCloseable closeable : closeables) {
      if (closeable != null) {
        try {
          closeable.close();
        } catch (Exception ignore) {
        }
      }
    }
  }

  public static String quoteTableName(String table) {
    switch (Db.getDatabaseType()) {
      case FIREBIRD:
        return table.equalsIgnoreCase("at") ? "\"" + table.toUpperCase() + "\"" : table;
      default:
        return table;
    }
  }

  public static void applyLimits(SelectQuery query, int from, int to ) {
    int limit = to >= 0 && to >= from && to < Integer.MAX_VALUE ? to - from + 1 : 0;
    if (limit > 0 && from > 0) {
      query.addLimit(limit, from);
    }
    else if (limit > 0) {
      query.addLimit(limit);
    }
    else if (from > 0) {
      query.addOffset(from);
    }
  }

  public static String limitsClause(int from, int to) {
    int limit = to >= 0 && to >= from && to < Integer.MAX_VALUE ? to - from + 1 : 0;
    switch (Db.getDatabaseType()) {
      case FIREBIRD: {
        if (limit > 0 && from > 0) {
          return " ROWS ? TO ? ";
        } else if (limit > 0) {
          return " ROWS ? ";
        } else if (from > 0) {
          return " ROWS ? TO ? ";
        } else {
          return "";
        }
      }
      default: {
        if (limit > 0 && from > 0) {
          return " LIMIT ? OFFSET ? ";
        } else if (limit > 0) {
          return " LIMIT ? ";
        } else if (from > 0) {
          return " OFFSET ? ";
        } else {
          return "";
        }
      }
    }


  }

  public static void mergeInto(DSLContext ctx, Record record, TableImpl table, Field[] keyFields) throws SQLException {
    ArrayList<Condition> conditions = new ArrayList<Condition>();
    for ( Field field : keyFields ) {
      conditions.add(field.eq(record.getValue(field)));
    }

    UpdateQuery updateQuery = ctx.updateQuery(table);
    updateQuery.setRecord(record);
    updateQuery.addConditions(conditions);
    if ( updateQuery.execute() == 0 ) {
      InsertQuery insertQuery = ctx.insertQuery(table);
      insertQuery.setRecord(record);
      insertQuery.execute();
    }
  }
}
