package brs.services;

import java.util.List;

public interface PropertyService {

  Boolean getBooleanProperty(String name, boolean assume);

  Boolean getBooleanProperty(String name);

  int getIntProperty(String name, int defaultValue);

  int getIntProperty(String name);

  String getStringProperty(String name, String defaultValue);

  String getStringProperty(String name);

  List<String> getStringListProperty(String name);
}
