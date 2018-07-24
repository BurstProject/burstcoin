package brs.services;

import brs.util.Time.FasterTime;

public interface TimeService {

  int getEpochTime();

  long getEpochTimeMillis();

  void setTime(FasterTime fasterTime);
}
