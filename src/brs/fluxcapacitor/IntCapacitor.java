package brs.fluxcapacitor;

import brs.services.PropertyService;

public class IntCapacitor extends TypeCapacitor<Integer> {

  public IntCapacitor(PropertyService propertyService) {
    super(propertyService);
  }

  @Override
  public Integer parseFromPropertyLine(String propertyValue) {
    return Integer.parseInt(propertyValue);
  }
}
