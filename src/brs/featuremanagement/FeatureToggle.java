package brs.featuremanagement;

import static brs.common.AliasNames.DYMAXION_END_BLOCK;
import static brs.common.AliasNames.DYMAXION_START_BLOCK;

public enum FeatureToggle {

  POC2(
      new FeatureDuration(500000, null),
      new FeatureDuration( 88000, null)
  ),
  PRE_DYMAXION(
      new FeatureDuration(500000, null),
      new FeatureDuration( 88000, null)
  ),
  DYMAXION(
      // tba
      new FeatureDuration(Integer.MAX_VALUE, null),
      new FeatureDuration(Integer.MAX_VALUE, null)
  );

  private final FeatureDuration featureDurationProductionNet;
  private final FeatureDuration featureDurationTestNet;

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
