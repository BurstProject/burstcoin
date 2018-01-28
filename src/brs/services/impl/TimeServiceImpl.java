package brs.services.impl;

import brs.Burst;
import brs.services.TimeService;

public class TimeServiceImpl implements TimeService {

  @Override
  public int getEpochTime() {
    return Burst.getEpochTime();
  }
}
