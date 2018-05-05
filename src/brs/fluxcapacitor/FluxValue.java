package brs.fluxcapacitor;

class FluxValue<T> {

  private final FluxHistory<T> valueHistoryProductionNet;
  private final FluxHistory<T> valueHistoryTestNet;
  private final String devPropertyName;

  public FluxValue(FluxHistory<T> valueHistoryProductionNet) {
    this.valueHistoryProductionNet = valueHistoryProductionNet;
    this.valueHistoryTestNet = null;
    this.devPropertyName = null;
  }

  public FluxValue(FluxHistory<T> valueHistoryProductionNet, FluxHistory<T> valueHistoryTestNet) {
    this.valueHistoryProductionNet = valueHistoryProductionNet;
    this.valueHistoryTestNet = valueHistoryTestNet;
    this.devPropertyName = null;
  }

  public FluxValue(FluxHistory<T> valueHistoryProductionNet, FluxHistory<T> valueHistoryTestNet, String devPropertyName) {
    this.valueHistoryProductionNet = valueHistoryProductionNet;
    this.valueHistoryTestNet = valueHistoryTestNet;
    this.devPropertyName = devPropertyName;
  }

  public FluxHistory<T> getValueHistoryProductionNet() {
    return valueHistoryProductionNet;
  }

  public FluxHistory<T> getValueHistoryTestNet() {
    return valueHistoryTestNet;
  }

  public String getDevPropertyName() {
    return devPropertyName;
  }
}
