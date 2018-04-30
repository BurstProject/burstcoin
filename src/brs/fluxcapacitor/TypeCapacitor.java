package brs.fluxcapacitor;

import brs.common.Props;
import brs.fluxcapacitor.FluxHistory.Element;
import brs.services.PropertyService;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TypeCapacitor<T> {

  final Logger logger = LoggerFactory.getLogger(TypeCapacitor.class);

  private final PropertyService propertyService;

  public TypeCapacitor(PropertyService propertyService) {
    this.propertyService = propertyService;
  }

  public T getValueAtHeight(FluxValue<T> flux, int height) {
    return getValueAtHeight(flux, propertyService.getBoolean(Props.DEV_TESTNET), height);
  }

  private T getValueAtHeight(FluxValue<T> flux, Boolean testNet, int height) {
    FluxHistory<T> history = null;

    if (testNet) {
      history = getPropertiesAlteredHistory(flux);
      if (history == null) {
        history = flux.getValueHistoryTestNet();
      }
    }

    if (history == null) {
      history = flux.getValueHistoryProductionNet();
    }

    return history.getValueAtHeight(height);
  }

  /**
   * Other Example required property: defaultValue;height1:value1;height2:value2
   */
  private FluxHistory<T> getPropertiesAlteredHistory(FluxValue<T> flux) {
    try {
      if (flux.getDevPropertyName() != null) {
        final String valueLine = propertyService.getString(flux.getDevPropertyName());

        if (valueLine != null) {
          final String[] splitVersion = valueLine.split(";");

          final T defaultValue = parseFromPropertyLine(splitVersion[0]);

          final List<Element<T>> history = getPropertyHistory(splitVersion);

          return new FluxHistory<>(defaultValue, history);
        }
      }
    } catch (Exception ex) {
      logger.error("Was not able to parse the altered history property " + flux.getDevPropertyName(), ex);
    }

    return null;
  }

  private List<Element<T>> getPropertyHistory(String[] historyElements) {
    final List<Element<T>> result = new ArrayList<>();

    if(historyElements.length > 1) {
      for (int i = 1; i < historyElements.length; i++) {
        final String[] historyElementPair = historyElements[i].split(":");
        result.add(new Element<>(Integer.parseInt(historyElementPair[0]), parseFromPropertyLine(historyElementPair[1])));
      }
    }

    return result;
  }

  public abstract T parseFromPropertyLine(String propertyValue);

}
