package brs.feesuggestions;

public class FeeSuggestion {

  private final long cheapFee;
  private final long standardFee;
  private final long priorityFee;

  public FeeSuggestion(long cheapFee, long standardFee, long priorityFee) {
    this.cheapFee = cheapFee;
    this.standardFee = standardFee;
    this.priorityFee = priorityFee;
  }

  public long getCheapFee() {
    return cheapFee;
  }

  public long getStandardFee() {
    return standardFee;
  }

  public long getPriorityFee() {
    return priorityFee;
  }
}
