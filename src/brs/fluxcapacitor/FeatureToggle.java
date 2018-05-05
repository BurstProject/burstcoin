package brs.fluxcapacitor;

import brs.fluxcapacitor.FluxHistory.Element;
import java.util.Arrays;

public enum FeatureToggle {

  POC2(new FluxValue<>(
      new FluxHistory<>(false, Arrays.asList(new Element<>(HistoricalMoments.POC2, true))),
      new FluxHistory<>(false, Arrays.asList(new Element<>(HistoricalMoments.POC2_TN, true))),
      "DEV.Feature.POC2")),
  PRE_DYMAXION(new FluxValue<>(
      new FluxHistory<>(false, Arrays.asList(new Element<>(HistoricalMoments.PRE_DYMAXION, true))),
      new FluxHistory<>(false, Arrays.asList(new Element<>(HistoricalMoments.PRE_DYMAXION_TN, true))),
      "DEV.Feature.PRE_DYMAXION")),
  DYMAXION(new FluxValue<>(
      //TBA
      new FluxHistory<>(false, Arrays.asList(new Element<>(HistoricalMoments.DYMAXION, true))),
      new FluxHistory<>(false, Arrays.asList(new Element<>(HistoricalMoments.DYMAXION_TN, true)))
  ));

  private FluxValue<Boolean> flux;

  FeatureToggle(FluxValue<Boolean> flux) {
    this.flux = flux;
  }

  public FluxValue<Boolean> getFlux() {
    return flux;
  }

}
