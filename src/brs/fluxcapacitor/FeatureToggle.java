package brs.fluxcapacitor;

import brs.Burst;
import brs.common.Props;
import brs.fluxcapacitor.FluxHistory.Element;
import java.util.Arrays;

public enum FeatureToggle {

  DIGITAL_GOODS_STORE(new FluxValue<>(
      new FluxHistory<>(false, Arrays.asList(new Element<>(HistoricalMoments.DIGITAL_GOODS_STORE_BLOCK, true))),
      new FluxHistory<>(false, Arrays.asList(new Element<>(HistoricalMoments.DIGITAL_GOODS_STORE_BLOCK_TN, true)))
      )),
  AUTOMATED_TRANSACTION_BLOCK(new FluxValue<>(
      new FluxHistory<>(false, Arrays.asList(new Element<>(HistoricalMoments.AUTOMATED_TRANSACTION_BLOCK, true))),
      new FluxHistory<>(false, Arrays.asList(new Element<>(HistoricalMoments.AUTOMATED_TRANSACTION_BLOCK_TN, true)))
      )),
  AT_FIX_BLOCK_2(new FluxValue<>(
      new FluxHistory<>(false, Arrays.asList(new Element<>(HistoricalMoments.AT_FIX_BLOCK_2, true))),
      new FluxHistory<>(false, Arrays.asList(new Element<>(HistoricalMoments.AT_FIX_BLOCK_2_TN, true)))
      )),
  AT_FIX_BLOCK_3(new FluxValue<>(
      new FluxHistory<>(false, Arrays.asList(new Element<>(HistoricalMoments.AT_FIX_BLOCK_3, true))),
      new FluxHistory<>(false, Arrays.asList(new Element<>(HistoricalMoments.AT_FIX_BLOCK_3_TN, true)))
      )),
  AT_FIX_BLOCK_4(new FluxValue<>(
      new FluxHistory<>(false, Arrays.asList(new Element<>(HistoricalMoments.AT_FIX_BLOCK_4, true))),
      new FluxHistory<>(false, Arrays.asList(new Element<>(HistoricalMoments.AT_FIX_BLOCK_4_TN, true)))
      )),
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
