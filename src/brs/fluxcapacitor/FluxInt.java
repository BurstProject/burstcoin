package brs.fluxcapacitor;

import brs.common.Props;
import brs.fluxcapacitor.FluxHistory.Element;
import java.util.Arrays;

public enum FluxInt {

  BLOCK_SIZE(new FluxValue<>(
      new FluxHistory<>(255, Arrays.asList(new Element<>(500000, 1020))),
      new FluxHistory<>(255, Arrays.asList(new Element<>(88000, 1020))),
      Props.DEV_BLOCK_SIZE_SETTING));

  private FluxValue<Integer> flux;

  FluxInt(FluxValue<Integer> flux) {
    this.flux = flux;
  }

  public FluxValue<Integer> getFlux() {
    return flux;
  }
}
