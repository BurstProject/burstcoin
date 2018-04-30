package brs.fluxcapacitor;

public class FeatureDuration {

  private final Integer startHeight;
  private final Integer endHeight;
  private final String startHeightAlias;
  private final String endHeightAlias;

  FeatureDuration(Integer startHeight, Integer endHeight) {
    this.startHeight = startHeight;
    this.endHeight = endHeight;
    this.startHeightAlias = null;
    this.endHeightAlias = null;
  }

  FeatureDuration(String startHeightAlias, String endHeightAlias) {
    this.startHeightAlias = startHeightAlias;
    this.endHeightAlias = endHeightAlias;
    this.startHeight = null;
    this.endHeight = null;
  }

  public Integer getStartHeight() {
    return startHeight;
  }

  public Integer getEndHeight() {
    return endHeight;
  }

  public String getStartHeightAlias() {
    return startHeightAlias;
  }

  public String getEndHeightAlias() {
    return endHeightAlias;
  }
}
