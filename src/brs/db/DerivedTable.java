package brs.db;

public interface DerivedTable {
  void rollback(int height);

  void truncate();

  void trim(int height);

  void finish();
}
