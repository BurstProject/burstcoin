package brs.fluxcapacitor;

import java.util.Comparator;
import java.util.List;

class FluxHistory<T> {

  private final T defaultValue;
  private final List<Element<T>> history;

  public FluxHistory(T defaultValue, List<Element<T>> history) {
    this.defaultValue = defaultValue;

    history.sort(Comparator.comparingInt(o -> o.height));

    this.history = history;
  }

  static class Element<T> {

    private final int height;
    private final T value;

    public Element(int height, T value) {
      this.height = height;
      this.value = value;
    }
  }

  T getValueAtHeight(int searchedHeight) {
    T finalValue = null;

    if (history != null && ! history.isEmpty()) {
      for(Element<T> element : history) {
        if(element.height > searchedHeight) {
          break;
        } else {
          finalValue = element.value;
        }
      }
    }

    if(finalValue == null) {
      finalValue = defaultValue;
    }

    return finalValue;
  }
}
