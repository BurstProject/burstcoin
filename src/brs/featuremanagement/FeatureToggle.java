package brs.featuremanagement;

import static brs.common.AliasNames.DYMAXION_END_BLOCK;
import static brs.common.AliasNames.DYMAXION_START_BLOCK;

import brs.common.Props;

public enum FeatureToggle {

  FEATURE_ONE(new FeatureDuration(null, 420000)),
  FEATURE_TWO(new FeatureDuration(420000, 430000), new FeatureDuration(20000, 30000)),
  FEATURE_THREE(new FeatureDuration(DYMAXION_START_BLOCK, DYMAXION_END_BLOCK)),
  FEATURE_FOUR(new FeatureDuration(50000, null), null, null, Props.DEV_FEATURE_POC2_END);

  private final FeatureDuration featureDurationProductionNet;
  private final FeatureDuration featureDurationTestNet;
  private final String propertyNameStartBlock;
  private final String propertyNameEndBlock;

  FeatureToggle(FeatureDuration featureDurationProduction) {
    this(featureDurationProduction, featureDurationProduction);
  }

  FeatureToggle(FeatureDuration featureDurationProductionNet, FeatureDuration featureDurationTestNet) {
    this(featureDurationProductionNet, featureDurationTestNet, null, null);
  }

  FeatureToggle(FeatureDuration featureDurationProductionNet, FeatureDuration featureDurationTestNet, String propertyNameStartBlock, String propertyNameEndBlock) {
    this.featureDurationProductionNet = featureDurationProductionNet;
    this.featureDurationTestNet = featureDurationTestNet;
    this.propertyNameStartBlock = propertyNameStartBlock;
    this.propertyNameEndBlock = propertyNameEndBlock;
  }

  public FeatureDuration getFeatureDurationProductionNet() {
    return featureDurationProductionNet;
  }

  public FeatureDuration getFeatureDurationTestNet() {
    return featureDurationTestNet;
  }

  public String getPropertyNameStartBlock() {
    return propertyNameStartBlock;
  }

  public String getPropertyNameEndBlock() {
    return propertyNameEndBlock;
  }
}
