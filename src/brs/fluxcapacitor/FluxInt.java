package brs.fluxcapacitor;

import static brs.Constants.MAX_PAYLOAD_LENGTH_PRE_DYMAXION;

import brs.Constants;
import brs.common.Props;
import brs.fluxcapacitor.FluxHistory.Element;
import java.util.Arrays;

public enum FluxInt {

  MAX_NUMBER_TRANSACTIONS(new FluxValue<>(
      new FluxHistory<>(255, Arrays.asList(new Element<>(HistoricalMoments.PRE_DYMAXION, 1020))),
      new FluxHistory<>(255, Arrays.asList(new Element<>(HistoricalMoments.PRE_DYMAXION_TN, 1020))),
      Props.DEV_BLOCK_SIZE_SETTING))
  , MAX_PAYLOAD_LENGTH(new FluxValue<>(
      new FluxHistory<>(Constants.MAX_PAYLOAD_LENGTH, Arrays.asList(new Element<>(HistoricalMoments.PRE_DYMAXION, MAX_PAYLOAD_LENGTH_PRE_DYMAXION))),
      new FluxHistory<>(Constants.MAX_PAYLOAD_LENGTH, Arrays.asList(new Element<>(HistoricalMoments.PRE_DYMAXION_TN, MAX_PAYLOAD_LENGTH_PRE_DYMAXION)))
  ));

  private FluxValue<Integer> flux;

  FluxInt(FluxValue<Integer> flux) {
    this.flux = flux;
  }

  public FluxValue<Integer> getFlux() {
    return flux;
  }
}
