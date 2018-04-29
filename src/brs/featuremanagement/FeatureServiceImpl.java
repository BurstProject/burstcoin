package brs.featuremanagement;

import brs.Alias;
import brs.Blockchain;
import brs.common.Props;
import brs.services.AliasService;
import brs.services.PropertyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureServiceImpl implements FeatureService {

  private static final Logger logger = LoggerFactory.getLogger(FeatureServiceImpl.class);

  private final Blockchain blockchain;

  private final AliasService aliasService;

  private final PropertyService propertyService;

  public FeatureServiceImpl(Blockchain blockchain, AliasService aliasService, PropertyService propertyService) {
    this.blockchain = blockchain;
    this.aliasService = aliasService;
    this.propertyService = propertyService;
  }

  @Override
  public boolean isActive(FeatureToggle featureToggle) {
    return isActive(featureToggle, propertyService.getBoolean(Props.DEV_TESTNET));
  }

  private boolean isActive(FeatureToggle featureToggle, boolean testNet) {
    FeatureDuration duration = null;

    if(testNet) {
      duration = getPropertiesAlteredDuration(featureToggle);
      if(duration == null) {
        duration = featureToggle.getFeatureDurationTestNet();
      }
    }

    if(duration == null) {
      duration = featureToggle.getFeatureDurationProductionNet();
    }

    return isActive(duration);
  }

  private FeatureDuration getPropertiesAlteredDuration(FeatureToggle featureToggle) {
    final int propertyValueDurationStart = propertyService.getInt(featureToggle.getPropertyNameStartBlock(), -1);
    final int propertyValueDurationEnd = propertyService.getInt(featureToggle.getPropertyNameEndBlock(), -1);

    if(propertyValueDurationStart != -1 || propertyValueDurationEnd != -1) {
      return new FeatureDuration(
          propertyValueDurationStart == -1 ? null: propertyValueDurationStart,
          propertyValueDurationEnd == -1 ? null: propertyValueDurationEnd
      );
    } else {
      return null;
    }
  }

  private boolean isActive(FeatureDuration featureDuration) {
    final int currentBlockHeight = blockchain.getHeight();

    final Integer minimumFeatureHeight = minimumFeatureHeight(featureDuration);
    final Integer maximumFeatureHeight = maximumFeatureHeight(featureDuration);

    return (minimumFeatureHeight == null || minimumFeatureHeight < currentBlockHeight) &&
        (maximumFeatureHeight == null || maximumFeatureHeight > currentBlockHeight);
  }

  private Integer maximumFeatureHeight(FeatureDuration featureDuration) {
    if(featureDuration.getEndHeight() != null) {
      return featureDuration.getEndHeight();
    } else {
      return aliasHeight(featureDuration.getEndHeightAlias());
    }
  }

  private Integer minimumFeatureHeight(FeatureDuration featureDuration) {
    if(featureDuration.getStartHeight() != null) {
      return featureDuration.getStartHeight();
    } else {
      return aliasHeight(featureDuration.getStartHeightAlias());
    }
  }

  private Integer aliasHeight(String aliasHeightName) {
    if(aliasHeightName != null) {
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
