package brs.services.impl;

import brs.services.TimeService;
import brs.util.Time;
import brs.util.Time.FasterTime;

public class TimeServiceImpl implements TimeService {

  private static volatile Time time = new Time.EpochTime();

  @Override
  public int getEpochTime() {
    return time.getTime();
  }

  @Override
  public long getEpochTimeMillis() {
    return time.getTimeInMillis();
  }

  @Override
  public void setTime(FasterTime t) {
    time = t;
  }

}
