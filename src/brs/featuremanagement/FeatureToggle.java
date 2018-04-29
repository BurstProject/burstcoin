package brs.featuremanagement;

import static brs.common.AliasNames.DYMAXION_END_BLOCK;
import static brs.common.AliasNames.DYMAXION_START_BLOCK;

public enum FeatureToggle {

  FEATURE_ONE(new FeatureDuration(null, 420000)), FEATURE_TWO(new FeatureDuration(420000, 430000), new FeatureDuration(20000, 30000)), FEATURE_THREE(new FeatureDuration(DYMAXION_START_BLOCK, DYMAXION_END_BLOCK));

  private final FeatureDuration featureDurationProductionNet;
  private final FeatureDuration featureDurationTestNet;

  FeatureToggle(FeatureDuration featureDurationProduction) {
    this(featureDurationProduction, featureDurationProduction);
  }

  FeatureToggle(FeatureDuration featureDurationProductionNet, FeatureDuration featureDurationTestNet) {
    this.featureDurationProductionNet = featureDurationProductionNet;
    this.featureDurationTestNet = featureDurationTestNet;
  }

  public FeatureDuration getFeatureDurationProductionNet() {
    return featureDurationProductionNet;
  }

  public FeatureDuration getFeatureDurationTestNet() {
    return featureDurationTestNet;
  }


}
