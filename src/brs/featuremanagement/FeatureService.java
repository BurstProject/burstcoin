package brs.featuremanagement;

public interface FeatureService {

  boolean isActive(FeatureToggle featureToggle);

}
