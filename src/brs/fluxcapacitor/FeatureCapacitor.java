package brs.fluxcapacitor;

import brs.services.PropertyService;

public class FeatureCapacitor extends TypeCapacitor<Boolean> {

  public FeatureCapacitor(PropertyService propertyService) {
    super(propertyService);
  }

  @Override
  public Boolean parseFromPropertyLine(String propertyValue) {
    if (propertyValue.matches("(?i)^1|active|true|yes|on$")) {
      return true;
    }

    if (propertyValue.matches("(?i)^0|false|no|off$")) {
      return false;
    }

    return null;
  }

}
