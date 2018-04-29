package brs.featuremanagement;

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
  private final String propertyNameStartBlock;
  private final String propertyNameEndBlock;

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
