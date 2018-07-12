package brs.feesuggestions;

public class FeeSuggestion {

  private final long cheapFee;
  private final long optimumFee;
  private final long priorityFee;

  public FeeSuggestion(long cheapFee, long optimumFee, long priorityFee) {
    this.cheapFee = cheapFee;
    this.optimumFee = optimumFee;
    this.priorityFee = priorityFee;
  }

  public long getCheapFee() {
    return cheapFee;
  }

  public long getOptimumFee() {
    return optimumFee;
  }

  public long getPriorityFee() {
    return priorityFee;
  }
}
