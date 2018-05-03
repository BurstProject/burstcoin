package brs.fluxcapacitor;

import brs.Blockchain;
import brs.services.PropertyService;

public class FluxCapacitorImpl implements FluxCapacitor {

  private Blockchain blockchain;

  private final FeatureCapacitor featureCapacitor;
  private final TypeCapacitor<Integer> intCapacitor;

  public FluxCapacitorImpl(Blockchain blockchain, PropertyService propertyService) {
    this.blockchain = blockchain;

    featureCapacitor = new FeatureCapacitor(propertyService);
    intCapacitor = new IntCapacitor(propertyService);
  }

  @Override
  public boolean isActive(FeatureToggle featureToggle) {
    return isActive(featureToggle, blockchain.getHeight());
  }

  @Override
  public boolean isActive(FeatureToggle featureToggle, int height) {
    return featureCapacitor.getValueAtHeight(featureToggle.getFlux(), height);
  }

  @Override
  public Integer getInt(FluxInt fluxInt) {
    return this.getInt(fluxInt, blockchain.getHeight());
  }

  @Override
  public Integer getInt(FluxInt fluxInt, int height) {
    return intCapacitor.getValueAtHeight(fluxInt.getFlux(), height);
  }

}
