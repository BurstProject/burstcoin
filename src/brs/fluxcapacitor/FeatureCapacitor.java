package brs.fluxcapacitor;

import brs.Alias;
import brs.common.Props;
import brs.services.AliasService;
import brs.services.PropertyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureCapacitor {

  private static final Logger logger = LoggerFactory.getLogger(FeatureCapacitor.class);

  private final AliasService aliasService;

  private final PropertyService propertyService;

  public FeatureCapacitor(AliasService aliasService, PropertyService propertyService) {
    this.aliasService = aliasService;
    this.propertyService = propertyService;
  }

  public boolean isActive(FeatureToggle featureToggle, int height) {
    return isActive(featureToggle, propertyService.getBoolean(Props.DEV_TESTNET), height);
  }

  private boolean isActive(FeatureToggle featureToggle, boolean testNet, int height) {
    FeatureDuration duration = null;

    if (testNet) {
      duration = getPropertiesAlteredDuration(featureToggle);
      if (duration == null) {
        duration = featureToggle.getFeatureDurationTestNet();
      }
    }

    if (duration == null) {
      duration = featureToggle.getFeatureDurationProductionNet();
    }

    return isActive(duration, height);
  }

  private FeatureDuration getPropertiesAlteredDuration(FeatureToggle featureToggle) {
    final int propertyValueDurationStart = featureToggle.getPropertyNameStartBlock() == null ? propertyService.getInt(featureToggle.getPropertyNameStartBlock(), -1) : -1;
    final int propertyValueDurationEnd = featureToggle.getPropertyNameStartBlock() == null ? propertyService.getInt(featureToggle.getPropertyNameStartBlock(), -1) : -1;

    if (propertyValueDurationStart != -1 || propertyValueDurationEnd != -1) {
      return new FeatureDuration(
          propertyValueDurationStart == -1 ? null : propertyValueDurationStart,
          propertyValueDurationEnd == -1 ? null : propertyValueDurationEnd
      );
    } else {
      return null;
    }
  }

  private boolean isActive(FeatureDuration featureDuration, int height) {
    final Integer minimumFeatureHeight = minimumFeatureHeight(featureDuration);
    final Integer maximumFeatureHeight = maximumFeatureHeight(featureDuration);

    return (minimumFeatureHeight == null || minimumFeatureHeight <= height) &&
        (maximumFeatureHeight == null || maximumFeatureHeight > height);
  }

  private Integer maximumFeatureHeight(FeatureDuration featureDuration) {
    if (featureDuration.getEndHeight() != null) {
      return featureDuration.getEndHeight();
    } else {
      return aliasHeight(featureDuration.getEndHeightAlias());
    }
  }

  private Integer minimumFeatureHeight(FeatureDuration featureDuration) {
    if (featureDuration.getStartHeight() != null) {
      return featureDuration.getStartHeight();
    } else {
      return aliasHeight(featureDuration.getStartHeightAlias());
    }
  }

  private Integer aliasHeight(String aliasHeightName) {
    if (aliasHeightName != null) {
      try {
        final Alias heightAlias = aliasService.getAlias(aliasHeightName);
        return (heightAlias == null || heightAlias.getAliasURI() == null) ? null : Integer.parseInt(heightAlias.getAliasURI());
      } catch (NumberFormatException ex) {
        logger.debug("Unexpected value for feature height of alias " + aliasHeightName, ex);
      }
    }

    return null;
  }
}
