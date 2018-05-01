package brs.fluxcapacitor;

public enum FeatureToggle {

  POC2(
      new FeatureDuration(HistoricalMoments.POC2, null),
      new FeatureDuration(HistoricalMoments.POC2_TN, null),
      "DEV.Feature.POC2.start",
      "DEV.Feature.POC2.end"
  ),
  PRE_DYMAXION(
      new FeatureDuration(HistoricalMoments.PRE_DYMAXION, null),
      new FeatureDuration(HistoricalMoments.PRE_DYMAXION_TN, null),
      "DEV.Feature.PRE_DYMAXION.start",
      "DEV.Feature.PRE_DYMAXION.end"
  ),
  DYMAXION(
      // tba
      new FeatureDuration(HistoricalMoments.DYMAXION, null),
      new FeatureDuration(HistoricalMoments.DYMAXION_TN, null)
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
