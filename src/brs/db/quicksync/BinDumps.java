package brs.db.quicksync;


import brs.db.firebird.FirebirdDbs;
import brs.db.sql.Db;
import brs.db.sql.DbUtils;

import java.lang.reflect.Field;

public class BinDumps {
  /** Version of the binary dump files. THIS MUST BE INCREMENTED WHEN CHANGING ANY CLASS IN THE POJO PACKAGE OR
   * ANYTHING RELATED TO CREATING OR LOADING BINARY DUMPS!
   */
  static final int VERSION = 1;
  static final String MAGIC ="BurstBlockChain";

  static String getTableName(Class clazz) {
    return DbUtils.quoteTableName(clazz.getSimpleName().toLowerCase());
  }

  static String getColumnName(Field field) {
    String name = field.getName();
    if (Db.getDatabaseType() == Db.TYPE.FIREBIRD) {
      name = name.toLowerCase();
      switch (name) {
        case "timestamp":
          return "\""+name+"\"";
        case "referenced_transaction_full_hash":
          return FirebirdDbs.maybeToShortIdentifier("referenced_transaction_full_hash");

      }
    }
    return name;
  }

}
