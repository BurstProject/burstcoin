package brs.services;

import brs.util.Time.FasterTime;

public interface TimeService {

  int getEpochTime();

  void setTime(FasterTime fasterTime);
}
