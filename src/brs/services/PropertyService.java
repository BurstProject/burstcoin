package brs.services;

import java.util.List;

public interface PropertyService {

  Boolean getBoolean(String name, boolean assume);

  Boolean getBoolean(String name);

  int getInt(String name, int defaultValue);

  int getInt(String name);

  String getString(String name, String defaultValue);

  String getString(String name);

  List<String> getStringList(String name);

}
